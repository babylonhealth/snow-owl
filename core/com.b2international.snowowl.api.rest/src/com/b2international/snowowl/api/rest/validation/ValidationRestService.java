/*
 * Copyright 2011-2019 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.api.rest.validation;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.snowowl.api.rest.AbstractRestService;
import com.b2international.snowowl.api.rest.admin.AbstractAdminRestService;
import com.b2international.snowowl.api.rest.domain.RestApiError;
import com.b2international.snowowl.api.rest.util.DeferredResults;
import com.b2international.snowowl.api.rest.util.Responses;
import com.b2international.snowowl.core.RepositoryInfo;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.exceptions.ApiValidation;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.core.internal.validation.ValidationConfiguration;
import com.b2international.snowowl.core.validation.ValidateRequestBuilder;
import com.b2international.snowowl.core.validation.ValidationRequests;
import com.b2international.snowowl.core.validation.ValidationResult;
import com.b2international.snowowl.core.validation.issue.ValidationIssue;
import com.b2international.snowowl.core.validation.issue.ValidationIssues;
import com.b2international.snowowl.core.validation.rule.ValidationRule;
import com.b2international.snowowl.datastore.CodeSystemEntry;
import com.b2international.snowowl.datastore.remotejobs.RemoteJobEntry;
import com.b2international.snowowl.datastore.remotejobs.RemoteJobs;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.datastore.request.job.JobRequests;
import com.b2international.snowowl.terminologyregistry.core.request.CodeSystemRequests;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Spring controller for exposing validation functionality.
 * 
 * @since 6.13
 */
@Api("Validations")
@Controller
@RequestMapping(produces={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
public class ValidationRestService extends AbstractAdminRestService {
	
	private Map<String, String> uniqueIdByUuid = Maps.newHashMap();
	
	@ApiOperation(
			value="Retrieve all validation runs from the termserver", 
			notes="Returns a list of validations runs")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK")
	})
	@RequestMapping(value="/validations", method=RequestMethod.GET)
	public @ResponseBody DeferredResult<RemoteJobs> getAllValidationRuns() {
		
		return DeferredResults.wrap(JobRequests.prepareSearch()
			.all()
			.buildAsync()
			.execute(bus)
			.then(jobs -> {
				final List<RemoteJobEntry> validationJobs = jobs.stream().filter(ValidationRequests::isValidationJob).collect(Collectors.toList());
				return new RemoteJobs(validationJobs, null, null, jobs.getLimit(), validationJobs.size());
			}));
	}
	
	
	@ApiOperation(
			value="Start a validation on a branch",
			notes = "Validation runs are async jobs. The call to this method immediately returns with a unique URL "
					+ "pointing to the validation run.<p>The URL can be used to fetch the state of the validation "
					+ "to determine whether it's completed or not.")
	@ApiResponses({
		@ApiResponse(code = 201, message = "Created"),
		@ApiResponse(code = 404, message = "Branch or CodeSystem not found", response=RestApiError.class),
		@ApiResponse(code = 409, message = "Validation job with the same id is already running", response=RestApiError.class)
	})
	@RequestMapping(
			value="/validations", 
			method=RequestMethod.POST,
			consumes={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(value=HttpStatus.CREATED)
	public DeferredResult<ResponseEntity<?>> beginValidation(
			@ApiParam(value="Validation parameters")
			@RequestBody 
			final ValidationRestInput validationInput,

			final Principal principal) {
		ApiValidation.checkInput(validationInput);

		final String codeSystemShortName = validationInput.codeSystemShortName();
		final CodeSystemEntry codeSystem = getCodeSystem(codeSystemShortName);
		
		Preconditions.checkArgument(codeSystem.getBranchPath().equals(validationInput.branchPath()));
		
		final String uniqueJobId = ValidationRequests.createUniqueValidationId(codeSystemShortName, codeSystem.getBranchPath());
		
		final Map<String, Object> ruleParams = ImmutableMap.<String, Object>builder()
				.put(ValidationConfiguration.IS_UNPUBLISHED_ONLY, validationInput.isUnpublishedValidation())
				.build();
		
		final Promise<Boolean> deleteValidationJobPromise;
		final RemoteJobEntry remoteJobEntry = JobRequests.prepareSearch()
				.filterById(uniqueJobId)
				.buildAsync()
				.execute(bus)
				.getSync().stream()
				.sorted(Comparator.comparing(RemoteJobEntry::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
				.findFirst()
				.orElse(null);
		
		if (remoteJobEntry != null) {
			if (remoteJobEntry.isDone() || remoteJobEntry.isCancelled() || remoteJobEntry.isDeleted()) {
				deleteValidationJobPromise = JobRequests.prepareDelete(uniqueJobId)
					.buildAsync()
					.execute(bus);
			} else {
				return DeferredResults.wrap(Promise.immediate(Responses.status(HttpStatus.CONFLICT).build()));
			}
		} else {
			deleteValidationJobPromise = Promise.immediate(Boolean.TRUE);
		}
		
		
		final ControllerLinkBuilder linkBuilder = linkTo(ValidationRestService.class)
				.slash("validations");
		return DeferredResults.wrap(deleteValidationJobPromise
			.<String>thenWith(success -> {
				final ValidateRequestBuilder validateRequestBuilder = ValidationRequests
						.prepareValidate()
						.setRuleParameters(ruleParams)
						.setRuleIds(validationInput.ruleIds());
				
				final Request<ServiceProvider, ValidationResult> request = validateRequestBuilder
						.build(codeSystem.getRepositoryUuid(), validationInput.branchPath())
						.getRequest();
					
				return JobRequests.prepareSchedule()
					.setRequest(request)
					.setDescription(String.format("Validating SNOMED CT on branch '%s'", validationInput.branchPath()))
					.setUser(principal.getName())
					.setId(uniqueJobId)
					.buildAsync()
					.execute(bus);
			})
			.then(success -> {
				final String validationUuid = UUID.randomUUID().toString();
				uniqueIdByUuid.put(validationUuid, uniqueJobId);
				
				final URI responseURI = linkBuilder.slash(validationUuid).toUri();
				return Responses.created(responseURI).build();
				
			})
			.fail(e -> {
				return Responses.status(HttpStatus.BAD_REQUEST).build();
			}));
	}
	
	@ApiOperation(
			value="Retrieve the state of a validation run from branch")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Validation not found", response=RestApiError.class)
	})
	@RequestMapping(value="/validations/{validationId}", method=RequestMethod.GET)
	public @ResponseBody DeferredResult<RemoteJobEntry> getValidationRun(
			@ApiParam(value="The validation identifier")
			@PathVariable(value="validationId") 
			final String validationId) {
		
		final String uniqueJobId = uniqueIdByUuid.get(validationId);
		if (uniqueJobId != null) {
			return DeferredResults.wrap(JobRequests.prepareGet(uniqueJobId)
					.buildAsync()
					.execute(bus));
			
		} else {
			throw new NotFoundException("Validation job", validationId);
		}
				
	}
	
	@ApiOperation(
			value="Retrieve the validation issues from a completed validation on a branch.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Branch not found", response=RestApiError.class)
	})
	@RequestMapping(value="/validations/{branchPath}/issues", method=RequestMethod.GET)
	public @ResponseBody DeferredResult<ValidationIssues> getValidationResults(
			@ApiParam(value="Branch path where the validation was run on.")
			@PathVariable(value="branchPath")
			final String branchPath,
			
			@ApiParam(value="The maximum number of items to return")
			@RequestParam(value="limit", defaultValue="50", required=false)   
			final int limit) {
		
		return DeferredResults.wrap(ValidationRequests.issues().prepareSearch()
			.all()
			.filterByBranchPath(branchPath)
			.setLimit(limit)
			.buildAsync()
			.execute(bus));
	}
	
	@ApiOperation(
			value="Retrieve the validation issues from a completed validation on a branch.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Branch or validation not found", response=RestApiError.class)
	})
	@RequestMapping(value="/validations/{branchPath}/issues", method=RequestMethod.GET, produces={AbstractRestService.CSV_MEDIA_TYPE})
	public @ResponseBody DeferredResult<Collection<Object>> getValidationResultsAsCsv(
			@ApiParam(value="Branch path where the validation was run on.")
			@PathVariable(value="branchPath")
			final String branchPath,
			
			@ApiParam(value="The maximum number of items to return")
			@RequestParam(value="limit", defaultValue="50", required=false)   
			final int limit) {
		
		return DeferredResults.wrap(ValidationRequests.issues().prepareSearch()
			.all()
			.filterByBranchPath(branchPath)
			.setLimit(limit)
			.buildAsync()
			.execute(bus)
			.then(issues -> {
				final Set<String> rulesToFetch = issues.stream().map(ValidationIssue::getRuleId).collect(Collectors.toSet());
				final Map<String, String> ruleDescriptionById = ValidationRequests.rules().prepareSearch()
					.all()
					.filterByIds(rulesToFetch)
					.buildAsync()
					.execute(bus)
					.getSync()
					.stream().collect(Collectors.toMap(ValidationRule::getId, ValidationRule::getMessageTemplate));
				final Collection<Object> reports = issues.stream().map(issue -> {
					final String ruleId = issue.getRuleId();
					final String ruleDescription = ruleDescriptionById.get(ruleId);
					final String affectedComponentLabel = Iterables.getFirst(issue.getAffectedComponentLabels(), "No label found");
					final String affectedComponentId = issue.getAffectedComponent().getComponentId();
					return new ValidationIssueReport(ruleId, ruleDescription, affectedComponentId, affectedComponentLabel);
				}).collect(Collectors.toList());
				
				return reports;
			}));
		
	}
	
	private CodeSystemEntry getCodeSystem(final String codeSystemShortName) {
		final Set<String> repositoryIds = getAllRepoIds();
		
		for (String repoId : repositoryIds) {
			CodeSystemEntry codeSystemEntry = CodeSystemRequests.prepareGetCodeSystem(codeSystemShortName)
				.build(repoId)
				.execute(bus)
				.getSync();
			
			if (codeSystemEntry != null) {
				return codeSystemEntry;
			}
		}
		
		throw new NotFoundException("Codesystem", codeSystemShortName);
		
	}
	
	private Set<String> getAllRepoIds() {
		return RepositoryRequests.prepareSearch()
					.all()
					.buildAsync()
					.execute(bus)
					.getSync().stream()
					.map(RepositoryInfo::id)
					.collect(Collectors.toSet());
	}
	
}
