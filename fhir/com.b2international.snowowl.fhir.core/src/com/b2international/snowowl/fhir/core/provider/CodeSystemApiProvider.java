/*
 * Copyright 2018-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.provider;

import static com.google.common.collect.Sets.newHashSet;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.codesystem.*;
import com.b2international.snowowl.core.codesystem.version.CodeSystemVersionSearchRequestBuilder;
import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.core.request.SearchResourceRequest;
import com.b2international.snowowl.core.uri.CodeSystemURI;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.fhir.core.codesystems.*;
import com.b2international.snowowl.fhir.core.exceptions.BadRequestException;
import com.b2international.snowowl.fhir.core.exceptions.FhirException;
import com.b2international.snowowl.fhir.core.model.Meta;
import com.b2international.snowowl.fhir.core.model.codesystem.*;
import com.b2international.snowowl.fhir.core.model.codesystem.CodeSystem;
import com.b2international.snowowl.fhir.core.model.codesystem.CodeSystem.Builder;
import com.b2international.snowowl.fhir.core.model.dt.Identifier;
import com.b2international.snowowl.fhir.core.model.dt.Instant;
import com.b2international.snowowl.fhir.core.model.dt.Uri;
import com.b2international.snowowl.fhir.core.search.FhirSearchParameter;
import com.b2international.snowowl.fhir.core.search.FhirUriSearchParameterDefinition;
import com.b2international.snowowl.fhir.core.search.FhirParameter.PrefixedValue;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * FHIR provider base class.
 * 
 * @since 7.0
 */
public abstract class CodeSystemApiProvider extends FhirApiProvider implements ICodeSystemApiProvider {
	
	private static final String CODE_SYSTEM_LOCATION_MARKER = "CodeSystem";

	private final String repositoryId;
	
	private Collection<IConceptProperty> supportedProperties;

	public CodeSystemApiProvider(IEventBus bus, List<ExtendedLocale> locales, String repositoryId) {
		super(bus, locales);
		this.repositoryId = repositoryId;
	}
	
	@Override
	protected String getRepositoryId() {
		return repositoryId;
	}
	
	/**
	 * Subclasses may override this method to provide additional properties supported by this FHIR provider.
	 * @return
	 */
	protected Collection<IConceptProperty> getSupportedConceptProperties() {
		return Collections.emptySet();
	}

	@Override
	public boolean isSupported(String uri) {
		if (Strings.isNullOrEmpty(uri)) return false;
		return getSupportedURIs().stream()
			.filter(uri::equalsIgnoreCase)
			.findAny()
			.isPresent();
	}
	
	@Override
	public CodeSystem getCodeSystem(CodeSystemURI codeSystemURI) {
		
		CodeSystems codeSystems = CodeSystemRequests.prepareSearchCodeSystem()
			.one()
			.filterById(codeSystemURI.getCodeSystem())
			.build(repositoryId)
			.execute(getBus())
			.getSync(1000, TimeUnit.MILLISECONDS);
		
		Optional<com.b2international.snowowl.core.codesystem.CodeSystem> optionalCodeSystem = codeSystems.first();
		if (optionalCodeSystem.isEmpty()) {
			throw FhirException.createFhirError(String.format("No code system version found for code system %s", codeSystemURI.getUri()), OperationOutcomeCode.MSG_PARAM_INVALID, CODE_SYSTEM_LOCATION_MARKER);
		}
		
		com.b2international.snowowl.core.codesystem.CodeSystem codeSystem = optionalCodeSystem.get();
		
		CodeSystemVersions codeSystemVersions = CodeSystemRequests.prepareSearchCodeSystemVersion()
			.one()
			.filterByCodeSystemShortName(codeSystem.getShortName())
			.filterByVersionId(codeSystemURI.getPath())
			.build(repositoryId)
			.execute(getBus())
			.getSync(1000, TimeUnit.MILLISECONDS);
		
		Optional<CodeSystemVersion> codeSystemVersionsOptional = codeSystemVersions.first();
		if (codeSystemVersionsOptional.isEmpty()) {
			throw FhirException.createFhirError(String.format("No code system version found for code system %s", codeSystemURI.getUri()), OperationOutcomeCode.MSG_PARAM_INVALID, CODE_SYSTEM_LOCATION_MARKER);
		}
		
		CodeSystemVersion codeSystemVersionEntry = codeSystemVersionsOptional.get();
		
		return createCodeSystemBuilder(codeSystem, codeSystemVersionEntry).build();
	}
	
	@Override
	public Collection<CodeSystem> getCodeSystems(final Set<FhirSearchParameter> searchParameters) {
		
		CodeSystemSearchRequestBuilder requestBuilder = CodeSystemRequests.prepareSearchCodeSystem().all();
		
		//TODO: what should we do with the hardcoded names of these dynamic properties?
		Optional<FhirSearchParameter> idParamOptional = getSearchParam(searchParameters, "_id");
		if (idParamOptional.isPresent()) {
			Collection<String> ids = idParamOptional.get().getValues().stream()
					.map(PrefixedValue::getValue)
					.collect(Collectors.toSet());
			
			requestBuilder.filterByIds(ids);
		}

		Optional<FhirSearchParameter> nameOptional = getSearchParam(searchParameters, "_name"); 
		if (nameOptional.isPresent()) {
			Collection<String> names = nameOptional.get().getValues().stream()
					.map(PrefixedValue::getValue)
					.collect(Collectors.toSet());
			
			requestBuilder.filterByNameExact(names);
		}
		
		CodeSystems codeSystems = requestBuilder.build(repositoryId)
			.execute(getBus())
			.getSync();
		
		//fetch all the versions
		CodeSystemVersionSearchRequestBuilder versionSearchRequestBuilder = CodeSystemRequests.prepareSearchCodeSystemVersion().all();
		
		Optional<FhirSearchParameter> lastUpdatedOptional = getSearchParam(searchParameters, "_lastUpdated"); //date type
		if (lastUpdatedOptional.isPresent()) {

			PrefixedValue lastUpdatedPrefixedValue = lastUpdatedOptional.get().getValues().iterator().next();
			String lastUpdatedDateString = lastUpdatedPrefixedValue.getValue();
			
			//validate the date value
			long localLong = Long.MAX_VALUE;
			try {
				LocalDate localDate = LocalDate.parse(lastUpdatedDateString);
				localLong = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(); //?
			} catch (DateTimeParseException dtpe) {
				throw FhirException.createFhirError(String.format("Invalid _lastUpdate search parameter value '%s'.", lastUpdatedDateString), OperationOutcomeCode.MSG_PARAM_INVALID, CODE_SYSTEM_LOCATION_MARKER);
			}

			//TODO: filterByCreated at searches for importDates as opposed to lastModificationDates
			if (lastUpdatedPrefixedValue.getPrefix() == null || 
					FhirUriSearchParameterDefinition.SearchRequestParameterValuePrefix.eq == lastUpdatedPrefixedValue.getPrefix()) {
					
				versionSearchRequestBuilder.filterByCreatedAt(localLong);
			} else {
				switch (lastUpdatedPrefixedValue.getPrefix()) {
				case eb:
					versionSearchRequestBuilder.filterByCreatedAt(0, localLong);
					break;
				case sa:
					versionSearchRequestBuilder.filterByCreatedAt(localLong, Long.MAX_VALUE);
					break;
				default:
					throw FhirException.createFhirError(String.format("Unsupported _lastUpdate search parameter modifier '%s' for value '%s'.", lastUpdatedPrefixedValue.getPrefix().name(), lastUpdatedDateString), OperationOutcomeCode.MSG_PARAM_INVALID, CODE_SYSTEM_LOCATION_MARKER);
				}
			}
		}
		
		CodeSystemVersions codeSystemVersions = versionSearchRequestBuilder
			.sortBy(SearchResourceRequest.SortField.descending(CodeSystemVersionEntry.Fields.EFFECTIVE_DATE))
			.build(repositoryId)
			.execute(getBus())
			.getSync();
		
		List<CodeSystem> fhirCodeSystemList = Lists.newArrayList();
		
		codeSystems.forEach(cse -> { 
			List<CodeSystem> fhirCodeSystems = codeSystemVersions.stream()
				.filter(csv -> csv.getUri().getCodeSystem().equals(cse.getShortName()))
				.map(csve -> createCodeSystemBuilder(cse, csve))
				.map(Builder::build)
				.collect(Collectors.toList());
			
			fhirCodeSystemList.addAll(fhirCodeSystems);
			
		});
		return fhirCodeSystemList;
	}
	
	/**
	 * Returns the designated FHIR Uri for the given code system
	 * @param codeSystem
	 * @param codeSystemVersion 
	 * @return
	 */
	protected abstract Uri getFhirUri(com.b2international.snowowl.core.codesystem.CodeSystem codeSystem, CodeSystemVersion codeSystemVersion);
	
	/**
	 * Creates a FHIR {@link CodeSystem} from a {@link com.b2international.snowowl.core.codesystem.CodeSystem}
	 * @param codeSystem
	 * @param codeSystemVersion
	 * @return FHIR Code system
	 */
	protected final Builder createCodeSystemBuilder(final com.b2international.snowowl.core.codesystem.CodeSystem codeSystem, 
			final CodeSystemVersion codeSystemVersion) {
		
		Identifier identifier = Identifier.builder()
			.use(IdentifierUse.OFFICIAL)
			.system(codeSystem.getOrganizationLink())
			.value(codeSystem.getOid())
			.build();
		
		String id = getId(codeSystem, codeSystemVersion);
		
		final Builder builder = CodeSystem.builder(id)
			.identifier(identifier)
			.language(getLanguage(codeSystem))
			.name(codeSystem.getShortName())
			.narrative(NarrativeStatus.ADDITIONAL, "<div>"+ codeSystem.getCitation() + "</div>")
			.publisher(codeSystem.getOrganizationLink())
			.status(PublicationStatus.ACTIVE)
			.hierarchyMeaning(CodeSystemHierarchyMeaning.IS_A)
			.title(codeSystem.getName())
			.description(codeSystem.getCitation())
			.url(getFhirUri(codeSystem, codeSystemVersion))
			.content(getCodeSystemContentMode())
			.count(getCount(codeSystemVersion));
		
		if (codeSystemVersion !=null) {
			builder.version(codeSystemVersion.getVersion());
			
			Meta meta = Meta.builder()
				.lastUpdated(Instant.builder().instant(codeSystemVersion.getLastModificationDate()).build())
				.build();
			
			builder.meta(meta);
		}

		//add filters here
		Collection<Filter> supportedFilters = getSupportedFilters();
		for (Filter filter : supportedFilters) {
			builder.addFilter(filter);
		}
		
		Collection<Concept> concepts = getConcepts(codeSystem);
		for (Concept concept: concepts) {
			builder.addConcept(concept);
		}
		
		// include supported concept properties
		getSupportedProperties().stream()
			.filter(p -> !(SupportedCodeSystemRequestProperties.class.isInstance(p)))
			.map(SupportedConceptProperty::builder)
			.map(SupportedConceptProperty.Builder::build)
			.forEach(builder::addProperty);
		
		return builder;
	}
	
	/*
	 * Returns the logical ID of the code system resource
	 */
	private String getId(com.b2international.snowowl.core.codesystem.CodeSystem codeSystem, CodeSystemVersion codeSystemVersion) {
		//in theory there should always be at least one version present
		if (codeSystemVersion != null) {
			return codeSystem.getRepositoryId() + ":" + codeSystemVersion.getPath();
		} else {
			return codeSystem.getRepositoryId() + ":" + codeSystem.getBranchPath();
		}
	}

	protected Collection<Concept> getConcepts(com.b2international.snowowl.core.codesystem.CodeSystem codeSystem) {
		return Collections.emptySet();
	}

	protected abstract int getCount(CodeSystemVersion codeSystemVersion);

	@Override
	public SubsumptionResult subsumes(SubsumptionRequest subsumptionRequest) {
		
		final String version = getVersion(subsumptionRequest);
		final String branchPath = getBranchPath(version);
		
		String codeA = null;
		String codeB = null;
		if (subsumptionRequest.getCodeA() != null && subsumptionRequest.getCodeB() != null) {
			codeA = subsumptionRequest.getCodeA();
			codeB = subsumptionRequest.getCodeB();
		} else {
			codeA = subsumptionRequest.getCodingA().getCodeValue();
			codeB = subsumptionRequest.getCodingB().getCodeValue();
		}
		
		final Set<String> ancestorsA = fetchAncestors(branchPath, codeA);
		final Set<String> ancestorsB = fetchAncestors(branchPath, codeB);
		
		if (codeA.equals(codeB)) {
			return SubsumptionResult.equivalent();
		} else if (ancestorsA.contains(codeB)) {
			return SubsumptionResult.subsumedBy();
		} else if (ancestorsB.contains(codeA)) {
			return SubsumptionResult.subsumes();
		} else {
			return SubsumptionResult.notSubsumed();
		}
	}
	
	/**
	 * Returns the version information from the request
	 * @param subsumptionRequest 
	 * @return version string
	 */
	protected String getVersion(SubsumptionRequest subsumptionRequest) {
		String version = subsumptionRequest.getVersion();

		//get the latest version
		if (version == null) {
			return CodeSystemRequests.prepareSearchCodeSystemVersion()
				.one()
				.filterByCodeSystemShortName(getCodeSystemShortName())
				.sortBy(SearchResourceRequest.SortField.descending(CodeSystemVersionEntry.Fields.EFFECTIVE_DATE))
				.build(getRepositoryId())
				.execute(getBus())
				.getSync()
				.first()
				.map(CodeSystemVersion::getVersion)
				//never been versioned
				.orElse(null);
		}
		
		return version;
	}
	
	/**
	 * Builds a lookup result property for the given @see {@link IConceptProperty} based on the supplier's value
	 * @param supplier
	 * @param lookupRequest
	 * @param resultBuilder
	 * @param conceptProperty
	 */
	protected void addProperty(Supplier<?> supplier, LookupRequest lookupRequest, LookupResult.Builder resultBuilder, IConceptProperty conceptProperty) {
		
		if (lookupRequest.containsProperty(conceptProperty.getCode())) {
			if (supplier.get() != null) {
				resultBuilder.addProperty(conceptProperty.propertyOf(supplier));
			}
		}
	}

	/**
	 * Returns all ancestors up to the terminology's root component (in terms of Snow Owl, this means {@link IComponent#ROOT_ID}).
	 * 
	 * @param branchPath
	 * @param componentId
	 * @return
	 */
	protected abstract Set<String> fetchAncestors(String branchPath, String componentId);

	/**
	 * Returns the supported properties
	 * @return the supported properties
	 */
	protected Collection<IConceptProperty> getSupportedProperties() {
		if (supportedProperties == null) {
			supportedProperties = newHashSet();
			Stream.of(SupportedCodeSystemRequestProperties.values()).forEach(supportedProperties::add);
			supportedProperties.addAll(getSupportedConceptProperties());
		}
		return supportedProperties;
	}
	
	/**
	 * Subclasses may override this method to provide filters supported by this FHIR provider/code system.
	 * @return the supported filters
	 */
	protected Collection<Filter> getSupportedFilters() {
		return Collections.emptySet();
	}
	
	/**
	 * @param request - the lookup request
	 */
	protected void validateRequestedProperties(LookupRequest request) {
		final Collection<String> properties = request.getPropertyCodes();
		
		final Set<String> supportedCodes = getSupportedProperties().stream().map(p -> {
			if (p instanceof IConceptProperty.Dynamic) {
				return p.getUri().getUriValue();
			} else {
				return p.getCodeValue();
			}
		})
		.collect(Collectors.toSet());
		
		if (!supportedCodes.containsAll(properties)) {
			if (properties.size() == 1) {
				throw new BadRequestException(String.format("Unrecognized property %s. Supported properties are: %s.", Arrays.toString(properties.toArray()), Arrays.toString(supportedCodes.toArray())), "LookupRequest.property");
			} else {
				throw new BadRequestException(String.format("Unrecognized properties %s. Supported properties are: %s.", Arrays.toString(properties.toArray()), Arrays.toString(supportedCodes.toArray())), "LookupRequest.property");
			}
		}
	}
	
	/**
	 * Set the base properties if requested
	 * @param lookupRequest
	 * @param resultBuilder 
	 * @param name
	 * @param version
	 * @param displayString
	 */
	protected void setBaseProperties(LookupRequest lookupRequest, LookupResult.Builder resultBuilder, String name, String version, String displayString) {
		
		/*
		 * Name is mandatory, why is it allowed to be listed as a requested property in the spec?? - bbanfai
		 */
		resultBuilder.name(name);
				
		if (lookupRequest.isVersionPropertyRequested()) {
			resultBuilder.version(version);
		}
			
		/*
		 * Display is mandatory, why is it allowed to be listed as a requested property in the spec?? - bbanfai
		 */
		resultBuilder.display(displayString);
	}
	
	protected CodeSystemContentMode getCodeSystemContentMode() {
		return CodeSystemContentMode.NOT_PRESENT;
	}

	protected String getLanguage(com.b2international.snowowl.core.codesystem.CodeSystem codeSystem) {
		return getLanguageCode(codeSystem.getPrimaryLanguage());
	}
}
