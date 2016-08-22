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

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.FULLY_SPECIFIED_NAME;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.SYNONYM;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

/**
 * A set of assert methods related to manipulation of components through the REST API.
 *
 * @since 2.0
 */
public abstract class SnomedComponentApiAssert {

	public static Map<?, ?> givenConceptRequestBody(final String conceptId, final String parentId, final String moduleId, final Map<?, ?> fsnAcceptabilityMap, final boolean skipComment) {

		final Date creationDate = new Date();
		final Map<?, ?> fsnDescription = givenDescriptionRequestBody(creationDate, "New FSN at ", fsnAcceptabilityMap, FULLY_SPECIFIED_NAME);
		final Map<?, ?> ptDescription = givenDescriptionRequestBody(creationDate, "New PT at ", PREFERRED_ACCEPTABILITY_MAP, SYNONYM);

		final ImmutableMap.Builder<String, Object> conceptBuilder = ImmutableMap.<String, Object>builder()
				.put("moduleId", moduleId)
				.put("descriptions", ImmutableList.of(fsnDescription, ptDescription));

		if (parentId != null) {
			conceptBuilder.put("parentId", parentId);
		}
		
		if (conceptId != null) {
			conceptBuilder.put("id", conceptId);
		}

		if (!skipComment) {
			conceptBuilder.put("commitComment", "New concept");
		}

		return conceptBuilder.build();
	}

	public static ImmutableMap<String, Object> givenDescriptionRequestBody(String termPrefix, Map<?, ?> acceptabilityMap, String typeId) {
		return givenDescriptionRequestBody(new Date(), termPrefix, acceptabilityMap, typeId);
	}
	
	private static ImmutableMap<String, Object> givenDescriptionRequestBody(final Date creationDate, String termPrefix, Map<?, ?> acceptabilityMap, String typeId) {
		return ImmutableMap.<String, Object>builder()
				.put("typeId", typeId)
				.put("term", termPrefix + creationDate)
				.put("languageCode", "en")
				.put("acceptability", acceptabilityMap)
				.build();
	}
	
	private static Builder<String, Object> createRelationshipRequestBuilder(final String sourceId, 
			final String typeId, 
			final String destinationId, 
			final String moduleId, 
			final String comment) {

		return ImmutableMap.<String, Object>builder()
				.put("sourceId", sourceId)
				.put("typeId", typeId)
				.put("destinationId", destinationId)
				.put("moduleId", moduleId)
				.put("commitComment", comment);
	}

	public static Map<String, Object> givenRelationshipRequestBody(final String sourceId, 
			final String typeId, 
			final String destinationId, 
			final String moduleId, 
			final String comment) {

		return createRelationshipRequestBuilder(sourceId, typeId, destinationId, moduleId, comment)
				.build();
	}

	public static Map<String, Object> givenRelationshipRequestBody(final String sourceId, 
			final String typeId, 
			final String destinationId, 
			final String moduleId, 
			final CharacteristicType characteristicType, 
			final String comment) {

		return createRelationshipRequestBuilder(sourceId, typeId, destinationId, moduleId, comment)
				.put("characteristicType", characteristicType.name())
				.build();
	}
	
	public static ValidatableResponse assertComponentReadWithStatus(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId, 
			final int statusCode, final String...expand) {

		return getComponent(branchPath, componentType, componentId, expand)
				.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}

	public static Response getComponent(final IBranchPath branchPath, final SnomedComponentType componentType, final String componentId, final String...expansions) {
		assertNotNull(componentId);
		String url = "/{path}/{componentType}/{id}";
		if (expansions != null && expansions.length > 0) {
			url = url.concat("?expand="+Joiner.on(",").join(expansions));
		}
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API).when().get(url, branchPath.getPath(), componentType.toLowerCasePlural(), componentId);
	}
	
	/**
	 * Asserts that the component with the given type and identifier exists on the given branch.
	 *  
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param componentId the expected component identifier
	 * @param expand expansion parameters
	 */
	public static ValidatableResponse assertComponentExists(final IBranchPath branchPath, final SnomedComponentType componentType, final String componentId, final String...expand) {
		return assertComponentReadWithStatus(branchPath, componentType, componentId, 200, expand);
	}

	/**
	 * Asserts that the component with the given type and identifier does not exist on the given branch.
	 *  
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param componentId the expected component identifier
	 */
	public static void assertComponentNotExists(final IBranchPath branchPath, final SnomedComponentType componentType, final String componentId) {
		assertComponentReadWithStatus(branchPath, componentType, componentId, 404);
	}

	private static Response whenCreatingComponent(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final Map<?, ?> requestBody) {

		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.and().body(requestBody)
				.when().post("/{path}/{componentType}", branchPath.getPath(), componentType.toLowerCasePlural());
	}

	public static ValidatableResponse assertComponentCreatedWithStatus(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final Map<?, ?> requestBody, 
			final int statusCode) {
		return whenCreatingComponent(branchPath, componentType, requestBody)
				.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}

	/**
	 * Asserts that the component with the given type can be created on the specified branch.
	 * 
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param requestBody the request body used for creating the component
	 * 
	 * @return the identifier of the created component
	 */
	public static String assertComponentCreated(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final Map<?, ?> requestBody) {

		final String componentLocation = assertComponentCreatedWithStatus(branchPath, componentType, requestBody, 201)
				.and().header("Location", containsString(String.format("%s/%s", branchPath.getPath(), componentType.toLowerCasePlural())))
				.and().body(equalTo(""))
				.and().extract().response().getHeader("Location");

		return lastPathSegment(componentLocation);
	}

	/**
	 * Asserts that the component creation with the given type will be rejected on the specified branch.
	 * 
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param requestBody the request body used for creating the component
	 * 
	 * @return the validatable response for additional checks
	 */
	public static ValidatableResponse assertComponentNotCreated(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final Map<?, ?> requestBody) {

		return assertComponentCreatedWithStatus(branchPath, componentType, requestBody, 400)
				.and().body("status", equalTo(400));
	}

	/**
	 * Asserts that the given property name/value pair appears on the component when read.
	 * 
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param componentId the component identifier
	 * @param propertyName the property name to test
	 * @param value the expected value 
	 */
	public static void assertComponentHasProperty(final IBranchPath branchPath,
			final SnomedComponentType componentType, 
			final String componentId, 
			final String propertyName, 
			final Object value) {

		assertComponentReadWithStatus(branchPath, componentType, componentId, 200)
		.and().body(propertyName, equalTo(value));
	}

	/**
	 * Assert that the component with the specified type and identifier is active on the given branch.
	 * 
	 * @param branchPath the branch path to test
	 * @param componentType the expected component type
	 * @param componentId the component identifier
	 * @param active the expected status ({@code true} if active, {@code false} if inactive)
	 */
	public static void assertComponentActive(final IBranchPath branchPath,
			final SnomedComponentType componentType, 
			final String componentId, 
			final boolean active) {

		assertComponentHasProperty(branchPath, componentType, componentId, "active", active);
	}

	/**
	 * Asserts that the component with the specified type and identifier can be updated using the given request body.
	 * 
	 * @param branchPath the branch path to use when updating
	 * @param componentType the component type
	 * @param componentId the identifier of the component to update
	 * @param requestBody the request body containing new property/value pairs
	 */
	public static void assertComponentCanBeUpdated(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId, 
			final Map<?, ?> requestBody) {

		assertComponentUpdatedWithStatus(branchPath, componentType, componentId, requestBody, 204);
	}

	public static void assertComponentUpdatedWithStatus(final IBranchPath branchPath, 
			final SnomedComponentType componentType,
			final String componentId, 
			final Map<?, ?> requestBody, 
			final int statusCode) {

		whenUpdatingComponent(branchPath, componentType, componentId, requestBody)
		.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}

	private static Response whenUpdatingComponent(final IBranchPath branchPath, 
			final SnomedComponentType componentType,
			final String componentId, 
			final Map<?, ?> requestBody) {
		
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
		.with().contentType(ContentType.JSON)
		.and().body(requestBody)
		.when().post("/{path}/{componentType}/{id}/updates", branchPath.getPath(), componentType.toLowerCasePlural(), componentId);
	}

	public static void assertComponentCanBeDeleted(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId) {

		assertComponentCanBeDeleted(branchPath, componentType, componentId, false, 204);
	}

	public static void assertComponentCanBeDeleted(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId,
			final boolean force) {

		assertComponentCanBeDeleted(branchPath, componentType, componentId, force, 204);
	}

	public static void assertComponentCanNotBeDeleted(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId,
			final boolean force) {

		assertComponentCanBeDeleted(branchPath, componentType, componentId, force, 409);
	}

	private static void assertComponentCanBeDeleted(final IBranchPath branchPath, 
			final SnomedComponentType componentType, 
			final String componentId,
			final boolean force,
			final int statusCode) {

		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
		.given().queryParam("force", force)
		.when().delete("/{path}/{componentType}/{id}", branchPath.getPath(), componentType.toLowerCasePlural(), componentId)
		.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}

	/**
	 * Asserts that a concept with the given identifier exists on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param conceptId the concept identifier to check
	 */
	public static ValidatableResponse assertConceptExists(final IBranchPath branchPath, final String conceptId) {
		return assertComponentExists(branchPath, SnomedComponentType.CONCEPT, conceptId);
	}

	/**
	 * Asserts that a concept with the given identifier does not exist on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param conceptId the concept identifier to check
	 */
	public static void assertConceptNotExists(final IBranchPath branchPath, final String conceptId) {
		assertComponentNotExists(branchPath, SnomedComponentType.CONCEPT, conceptId);
	}

	/**
	 * Asserts that a description with the given identifier exists on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param descriptionId the description identifier to check
	 */
	public static ValidatableResponse assertDescriptionExists(final IBranchPath branchPath, final String descriptionId, final String... expand) {
		return assertComponentExists(branchPath, SnomedComponentType.DESCRIPTION, descriptionId, expand);
	}

	/**
	 * Asserts that a description with the given identifier does not exist on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param descriptionId the description identifier to check
	 */
	public static void assertDescriptionNotExists(final IBranchPath branchPath, final String descriptionId) {
		assertComponentNotExists(branchPath, SnomedComponentType.DESCRIPTION, descriptionId);
	}

	/**
	 * Asserts that a relationship with the given identifier exists on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param relationshipId the relationship identifier to check
	 * @return 
	 */
	public static ValidatableResponse assertRelationshipExists(final IBranchPath branchPath, final String relationshipId) {
		return assertComponentExists(branchPath, SnomedComponentType.RELATIONSHIP, relationshipId);
	}

	/**
	 * Asserts that a relationship with the given identifier does not exist on the specified branch.
	 * 
	 * @param branchPath the branch path to check
	 * @param relationshipId the relationship identifier to check
	 */
	public static void assertRelationshipNotExists(final IBranchPath branchPath, final String relationshipId) {
		assertComponentNotExists(branchPath, SnomedComponentType.RELATIONSHIP, relationshipId);
	}

	/**
	 * Asserts that the concept's preferred term in the UK language reference set matches the specified description identifier.
	 * 
	 * @param branchPath the branch path to test
	 * @param conceptId the identifier of the concept where the preferred term should be compared
	 * @param descriptionId the expected description identifier
	 */
	public static void assertPreferredTermEquals(final IBranchPath branchPath, final String conceptId, final String descriptionId) {
		assertPreferredTermEquals(branchPath, conceptId, descriptionId, "en-GB");
	}
	
	/**
	 * Asserts that the concept's preferred term in the given language reference set matches the specified description identifier.
	 * 
	 * @param branchPath the branch path to test
	 * @param conceptId the identifier of the concept where the preferred term should be compared
	 * @param descriptionId the expected description identifier
	 * @param language - the language reference set
	 */
	public static void assertPreferredTermEquals(final IBranchPath branchPath, final String conceptId, final String descriptionId, final String language) {
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
		.with().header("Accept-Language", language)
		.when().get("/{path}/concepts/{conceptId}?expand=pt(),descriptions()", branchPath.getPath(), conceptId)
		.then().log().ifValidationFails().assertThat().statusCode(200)
		.and().body("pt.id", equalTo(descriptionId));
	}

	private SnomedComponentApiAssert() {
		throw new UnsupportedOperationException("This class is not supposed to be instantiated.");
	}
	
	public static Map<String, Object> createRefSetRequestBody(SnomedRefSetType type, String referencedComponentType, String parent) {
		final Map<String, Object> conceptBody = (Map<String, Object>) givenConceptRequestBody(null, parent, Concepts.MODULE_SCT_CORE, SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP, true);
		final Builder<String, Object> requestBody = ImmutableMap.builder();
		requestBody.putAll(conceptBody);
		requestBody.put("commitComment", String.format("New %s type reference set with %s members", type, referencedComponentType));
		requestBody.put("type", type);
		requestBody.put("referencedComponentType", referencedComponentType);
		return requestBody.build();
	}
	
	public static Map<String, Object> createRefSetMemberRequestBody(String referencedComponentId, String referenceSetId) {
		return createRefSetMemberRequestBody(Concepts.MODULE_SCT_CORE, referencedComponentId, referenceSetId);
	}
	
	public static Map<String, Object> createRefSetMemberRequestBody(String moduleId, String referencedComponentId, String referenceSetId) {
		return createRefSetMemberRequestBody(moduleId, referencedComponentId, referenceSetId, Collections.<String, Object>emptyMap());
	}
	
	public static Map<String, Object> createRefSetMemberRequestBody(String moduleId, String referencedComponentId, String referenceSetId, Map<String, Object> props) {
		final Builder<String, Object> requestBody = ImmutableMap.builder();
		requestBody.put("moduleId", moduleId);
		requestBody.put("referenceSetId", referenceSetId);
		if (referencedComponentId != null) {
			requestBody.put("referencedComponentId", referencedComponentId);
		}
		requestBody.put("commitComment", String.format("New reference set member '%s' in refset '%s'", referencedComponentId, referenceSetId));
		if (props != null) {
			requestBody.putAll(props);
		}
		return requestBody.build();
	}
	
}
