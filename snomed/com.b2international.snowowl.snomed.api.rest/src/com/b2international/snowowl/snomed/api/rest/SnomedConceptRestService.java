/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.commons.http.AcceptHeader;
import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.domain.PageableCollectionResource;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.snomed.api.rest.domain.ChangeRequest;
import com.b2international.snowowl.snomed.api.rest.domain.RestApiError;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedConceptRestInput;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedConceptRestUpdate;
import com.b2international.snowowl.snomed.api.rest.util.DeferredResults;
import com.b2international.snowowl.snomed.api.rest.util.Responses;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @since 1.0
 */
@Api("Concepts")
@Controller
@RequestMapping(produces={ AbstractRestService.SO_MEDIA_TYPE })
public class SnomedConceptRestService extends AbstractSnomedRestService {

	@ApiOperation(
			value="Retrieve Concepts from a branch", 
			notes="Returns a list with all/filtered Concepts from a branch."
					+ "<p>The following properties can be expanded:"
					+ "<p>"
					+ "&bull; pt() &ndash; the description representing the concept's preferred term in the given locale<br>"
					+ "&bull; fsn() &ndash; the description representing the concept's fully specified name in the given locale<br>"
					+ "&bull; descriptions() &ndash; the list of descriptions for the concept<br>")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK", response = PageableCollectionResource.class),
		@ApiResponse(code = 400, message = "Invalid filter config", response = RestApiError.class),
		@ApiResponse(code = 404, message = "Branch not found", response = RestApiError.class)
	})
	@RequestMapping(value="/{path:**}/concepts", method=RequestMethod.GET)
	public @ResponseBody DeferredResult<SnomedConcepts> search(
			@ApiParam(value="The branch path")
			@PathVariable(value="path")
			final String branch,

			@ApiParam(value="The description term to match")
			@RequestParam(value="term", required=false) 
			final String termFilter,

			@ApiParam(value="The ESCG expression to match")
			@RequestParam(value="escg", required=false) 
			final String escgFilter,
			
			@ApiParam(value="The concept module identifier to match")
			@RequestParam(value="module", required=false) 
			final String moduleFilter,
			
			@ApiParam(value="The concept status to match")
			@RequestParam(value="active", required=false) 
			final Boolean activeFilter,

			@ApiParam(value="The starting offset in the list")
			@RequestParam(value="offset", defaultValue="0", required=false) 
			final int offset,

			@ApiParam(value="The maximum number of items to return")
			@RequestParam(value="limit", defaultValue="50", required=false) 
			final int limit,
			
			@ApiParam(value="Expansion parameters")
			@RequestParam(value="expand", required=false)
			final String expand,

			@ApiParam(value="Accepted language tags, in order of preference")
			@RequestHeader(value="Accept-Language", defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
			final String acceptLanguage) {

		final List<ExtendedLocale> extendedLocales;
		
		try {
			extendedLocales = AcceptHeader.parseExtendedLocales(new StringReader(acceptLanguage));
		} catch (IOException e) {
			throw new BadRequestException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
		
		return DeferredResults.wrap(
				SnomedRequests
					.prepareSearchConcept()
					.setLimit(limit)
					.setOffset(offset)
					.filterByTerm(termFilter)
					.filterByEscg(escgFilter)
					.filterByModule(moduleFilter)
					.filterByActive(activeFilter)
					.setExpand(expand)
					.filterByExtendedLocales(extendedLocales)
					.build(branch)
					.execute(bus));
	}

	@ApiOperation(
			value="Retrieve Concept properties",
			notes="Returns all properties of the specified Concept, including a summary of inactivation indicator and association members."
					+ "<p>The following properties can be expanded:"
					+ "<p>"
					+ "&bull; pt() &ndash; the description representing the concept's preferred term in the given locale<br>"
					+ "&bull; fsn() &ndash; the description representing the concept's fully specified name in the given locale<br>"
					+ "&bull; descriptions() &ndash; the list of descriptions for the concept<br>"
					+ "&bull; ancestors(offset:0,limit:50,direct:true,expand(pt(),...)) &ndash; the list of concept ancestors (parameter 'direct' is required)<br>"
					+ "&bull; descendants(offset:0,limit:50,direct:true,expand(pt(),...)) &ndash; the list of concept descendants (parameter 'direct' is required)<br>")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK", response = Void.class),
		@ApiResponse(code = 404, message = "Branch or Concept not found", response = RestApiError.class)
	})
	@RequestMapping(value="/{path:**}/concepts/{conceptId}", method=RequestMethod.GET)
	public @ResponseBody DeferredResult<ISnomedConcept> read(
			@ApiParam(value="The branch path")
			@PathVariable(value="path")
			final String branchPath,

			@ApiParam(value="The Concept identifier")
			@PathVariable(value="conceptId")
			final String conceptId,
			
			@ApiParam(value="Expansion parameters")
			@RequestParam(value="expand", required=false)
			final String expand,
			
			@ApiParam(value="Accepted language tags, in order of preference")
			@RequestHeader(value="Accept-Language", defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
			final String acceptLanguage) {

		final List<ExtendedLocale> extendedLocales;
		
		try {
			extendedLocales = AcceptHeader.parseExtendedLocales(new StringReader(acceptLanguage));
		} catch (IOException e) {
			throw new BadRequestException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}

		return DeferredResults.wrap(
				SnomedRequests
					.prepareGetConcept()
					.setComponentId(conceptId)
					.setExpand(expand)
					.setLocales(extendedLocales)
					.build(branchPath)
					.execute(bus));
	}

	@ApiOperation(
			value="Create Concept", 
			notes="Creates a new Concept directly on a branch.")
	@ApiResponses({
		@ApiResponse(code = 201, message = "Concept created on task"),
		@ApiResponse(code = 404, message = "Branch not found", response = RestApiError.class)
	})
	@RequestMapping(
			value="/{path:**}/concepts", 
			method=RequestMethod.POST, 
			consumes={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Void> create(
			@ApiParam(value="The branch path")
			@PathVariable(value="path")
			final String branchPath,

			@ApiParam(value="Concept parameters")
			@RequestBody 
			final ChangeRequest<SnomedConceptRestInput> body,

			final Principal principal) {
		
		final String userId = principal.getName();
		
		final SnomedConceptRestInput change = body.getChange();
		final String commitComment = body.getCommitComment();
		
		final String createdConceptId = change
			.toRequestBuilder()
			.build(userId, branchPath, commitComment)
			.executeSync(bus, 120L * 1000L)
			.getResultAs(String.class);
		
		
		return Responses.created(getConceptLocationURI(branchPath, createdConceptId)).build();
	}

	@ApiOperation(
			value="Update Concept",
			notes="Updates properties of the specified Concept, also managing inactivation indicator and association reference set "
					+ "membership in case of inactivation."
					+ "<p>The following properties are allowed to change:"
					+ "<p>"
					+ "&bull; module identifier<br>"
					+ "&bull; subclass definition status<br>"
					+ "&bull; definition status<br>"
					+ "&bull; associated Concepts<br>"
					+ ""
					+ "<p>The following properties, when changed, will trigger inactivation:"
					+ "<p>"
					+ "&bull; inactivation indicator<br>")
	@ApiResponses({
		@ApiResponse(code = 204, message = "Update successful"),
		@ApiResponse(code = 404, message = "Branch or Concept not found", response = RestApiError.class)
	})
	@RequestMapping(
			value="/{path:**}/concepts/{conceptId}/updates", 
			method=RequestMethod.POST,
			consumes={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void update(			
			@ApiParam(value="The branch path")
			@PathVariable(value="path")
			final String branchPath,

			@ApiParam(value="The Concept identifier")
			@PathVariable(value="conceptId")
			final String conceptId,
			
			@ApiParam(value="Updated Concept parameters")
			@RequestBody 
			final ChangeRequest<SnomedConceptRestUpdate> body,

			final Principal principal) {

		final String userId = principal.getName();
		final String commitComment = body.getCommitComment();
		final SnomedConceptRestUpdate update = body.getChange();

		SnomedRequests
			.prepareUpdateConcept(conceptId)
			.setActive(update.isActive())
			.setModuleId(update.getModuleId())
			.setAssociationTargets(update.getAssociationTargets())
			.setDefinitionStatus(update.getDefinitionStatus())
			.setInactivationIndicator(update.getInactivationIndicator())
			.setSubclassDefinitionStatus(update.getSubclassDefinitionStatus())
			.build(userId, branchPath, commitComment)
			.executeSync(bus, 120L * 1000L);
	}

	@ApiOperation(
			value="Delete Concept",
			notes="Permanently removes the specified unreleased Concept and related components.<p>If any participating "
					+ "component has already been released the Concept can not be removed and a <code>409</code> "
					+ "status will be returned."
					+ "<p>The force flag enables the deletion of a released Concept. "
					+ "Deleting published components is against the RF2 history policy so"
					+ " this should only be used to remove a new component from a release before the release is published.</p>")
	@ApiResponses({
		@ApiResponse(code = 204, message = "Deletion successful"),
		@ApiResponse(code = 404, message = "Branch or Concept not found", response = RestApiError.class),
		@ApiResponse(code = 409, message = "Cannot be deleted if released", response = RestApiError.class)
	})
	@RequestMapping(value="/{path:**}/concepts/{conceptId}", method=RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(			
			@ApiParam(value="The branch path")
			@PathVariable(value="path")
			final String branchPath,

			@ApiParam(value="The Concept identifier")
			@PathVariable(value="conceptId")
			final String conceptId,

			@ApiParam(value="Force deletion flag")
			@RequestParam(defaultValue="false", required=false)
			final Boolean force,

			final Principal principal) {
		SnomedRequests
			.prepareDeleteConcept()
			.setComponentId(conceptId)
			.force(force)
			.build(principal.getName(), branchPath, String.format("Deleted Concept '%s' from store.", conceptId))
			.executeSync(bus, 120L * 1000L);
	}

	private URI getConceptLocationURI(String branchPath, String conceptId) {
		return linkTo(SnomedConceptRestService.class).slash(branchPath).slash("concepts").slash(conceptId).toUri();
	}
}
