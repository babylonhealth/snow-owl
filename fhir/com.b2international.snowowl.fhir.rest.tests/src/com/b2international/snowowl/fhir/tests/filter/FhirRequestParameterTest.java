/*
 * Copyright 2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.tests.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.b2international.commons.Pair;
import com.b2international.snowowl.fhir.core.exceptions.FhirException;
import com.b2international.snowowl.fhir.core.model.codesystem.CodeSystem;
import com.b2international.snowowl.fhir.core.search.*;
import com.b2international.snowowl.fhir.core.search.FhirUriParameterDefinition.FhirRequestParameterType;
import com.b2international.snowowl.fhir.tests.FhirTest;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;


/**
 * @since 6.4
 */
public class FhirRequestParameterTest extends FhirTest {
	
	private static FhirUriParameterManager parameterManager;
	
	private static Logger LOGGER = LoggerFactory.getLogger(FhirRequestParameterTest.class);

	@BeforeClass
	public static void loadParameterDefinitions() {
		parameterManager = FhirUriParameterManager.createFor(CodeSystem.class);
		LOGGER.info(parameterManager.toString());
	}
	
	//Raw unprocessed parameter
	@Test
	public void rawRequestParameterTest() {
		
		RawRequestParameter fhirParameter = new RawRequestParameter("_summary:data", ImmutableSet.of("1 ,2", "3"));
		assertThat(fhirParameter.getName(), equalTo("_summary"));
		assertThat(fhirParameter.getValues(), contains("1", "2", "3"));
		assertThat(fhirParameter.getModifier(), equalTo("data"));
	}
	
	//URI -> Raw unprocessed parameter
	@Test
	public void parameterParseTest() {
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_summary=1, 2");
		String key = paramMap.keySet().iterator().next();
		Collection<String> values = paramMap.get(key);
		RawRequestParameter fhirParameter = new RawRequestParameter(key, values);
		assertThat(fhirParameter.getName(), equalTo("_summary"));
		assertThat(fhirParameter.getValues(), contains("1", "2"));
		assertThat(fhirParameter.getModifier(), equalTo(null));
		
		paramMap = convertToMultimap("http://localhost?_text:exact=test");
		key = paramMap.keySet().iterator().next();
		values = paramMap.get(key);
		fhirParameter = new RawRequestParameter(key, values);
		assertThat(fhirParameter.getName(), equalTo("_text"));
		assertThat(fhirParameter.getValues(), contains("test"));
		assertThat(fhirParameter.getModifier(), equalTo("exact"));
	}
	
	@Test
	public void supportedParameterDefinitionsTest() {
		
		FhirUriParameterManager supportedDefinitions = FhirUriParameterManager.createFor(CodeSystem.class);
		
		Map<String, FhirUriFilterParameterDefinition> supportedFilterParameters = supportedDefinitions.getSupportedFilterParameters();
		
		Set<String> supportedFilterKeys = supportedFilterParameters.keySet();
		
		assertFalse(supportedFilterKeys.isEmpty());
		
		Optional<String> summaryFilterOptional = supportedFilterKeys.stream().filter(f -> f.equals(FhirUriFilterParameterDefinition.FhirFilterParameterKey._summary.name())).findFirst();
		assertTrue(summaryFilterOptional.isPresent());
		
		FhirUriFilterParameterDefinition summaryFilterParameter = supportedFilterParameters.get(summaryFilterOptional.get());
		
		assertThat(summaryFilterParameter.getType(), equalTo(FhirRequestParameterType.STRING));
	}
	
	//URI->Raw -> unknown param
	@Test
	public void unknownParameterTest() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("URI parameter unknownparameter is unknown.");
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?unknownparameter=true");
		parameterManager.processParameters(paramMap);
	}
	
	@Test
	public void unsupportedFilterParameterTest() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("Filter parameter _contained is not supported.");
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_contained=true");
		parameterManager.processParameters(paramMap);
		
	}
	
	@Test
	public void unsupportedFilterParameterValueTest() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("Filter parameter value [uknownvalue] is not supported.");
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_summary=uknownvalue");
		parameterManager.processParameters(paramMap);
	}
	
	@Test
	public void tooManyFilterParameterValuesTest() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("Too many filter parameter values [true, false] are submitted for parameter _summary.");
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_summary=true, false");
		parameterManager.processParameters(paramMap);
	}
	
	@Test
	public void testInvalidCrossFilterParameters() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("Both '_summary' and '_elements' search parameters cannot be specified at the same time.");
		parameterManager.processParameters(convertToMultimap("http://localhost?_summary=true&_elements=1"));
	}
	
	@Test
	public void invalidParameterModifierTest() {
		
		exception.expect(FhirException.class);
		exception.expectMessage("Invalid modifier ");
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_lastUpdated:type");
		parameterManager.processParameters(paramMap);
	}
	
	//URI->Raw -> filter
	@Test
	public void filterParameterTest() {
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_summary=true");
		Pair<Set<FhirFilterParameter>,Set<FhirSearchParameter>> parameters = parameterManager.processParameters(paramMap);
		assertFalse(parameters.getA().isEmpty());
		assertTrue(parameters.getB().isEmpty());
		
		FhirFilterParameter fhirParameter = parameters.getA().iterator().next();
		assertThat(fhirParameter.getName(), equalTo("_summary"));
		assertThat(fhirParameter.getType(), equalTo(FhirRequestParameterType.STRING));
		assertThat(fhirParameter.getValues(), contains("true"));
	}
	
	//URI->Raw -> filter
	@Test
	public void searchParameterTest() {
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost?_id=1");
		Pair<Set<FhirFilterParameter>,Set<FhirSearchParameter>> parameters = parameterManager.processParameters(paramMap);
	
		assertTrue(parameters.getA().isEmpty());
		assertFalse(parameters.getB().isEmpty());
		
		FhirSearchParameter fhirParameter = parameters.getB().iterator().next();
		
		assertThat(fhirParameter.getClass(), equalTo(FhirSearchParameter.class));
		assertThat(fhirParameter.getName(), equalTo("_id"));
		assertThat(fhirParameter.getType(), equalTo(FhirRequestParameterType.STRING));
		assertThat(fhirParameter.getValues(), contains("1"));
	}
	
	//Not really a test case - to check Spring/Guava parameter processing
	@Test
	public void uriParsingTest() {
		
		Multimap<String, String> paramMap = convertToMultimap("http://localhost");
		assertTrue(paramMap.isEmpty());
		
		paramMap = convertToMultimap("http://localhost?_summary=1, 2");
		Collection<String> paramValues = paramMap.get("_summary");
		assertThat(paramValues, contains("1, 2")); //Not split yet!
		
		paramMap = convertToMultimap("http://localhost?_summary=1&_summary=2");
		paramValues = paramMap.get("_summary");
		assertThat(paramValues, contains("1", "2"));

		paramMap = convertToMultimap("http://localhost?_summary");
		paramValues = paramMap.get("_summary");
		assertThat(paramValues.iterator().next(), equalTo(null)); //Bizarre Spring parameter handling
		
		paramMap = convertToMultimap("http://localhost?_summary=1, 2&_elements=id");
		paramValues = paramMap.get("_summary");
		assertThat(paramValues, contains("1, 2")); //not split!
		paramValues = paramMap.get("_elements");
		assertThat(paramValues, contains("id"));
		
		paramMap = convertToMultimap("http://localhost?_summary=1, 2&_summary=3");
		paramValues = paramMap.get("_summary");
		assertThat(paramValues, contains("1, 2", "3")); //not split!

		paramMap = convertToMultimap("http://localhost?property=isActive&property=effectiveTime");
		paramValues = paramMap.get("property");
		assertThat(paramValues, contains("isActive", "effectiveTime"));
	}
	
	/*
	 * Similar code as in BaseFhirResourceRestService
	 */
	private Multimap<String, String> convertToMultimap(final String urlString) {
		
		MultiValueMap<String, String> multiValueMap = UriComponentsBuilder
				.fromHttpUrl(urlString)
				.build()
				.getQueryParams();
		
		Multimap<String, String> multiMap = HashMultimap.create();
		multiValueMap.keySet().forEach(k -> multiMap.putAll(k, multiValueMap.get(k)));
		return multiMap;
	}
	
	private void printQueryParams(MultiValueMap<String, String> queryParams) {
		Set<String> keySet = queryParams.keySet();
		for (String key : keySet) {
			System.out.println("Key: " + key + ": " + queryParams.get(key));
		}
	}
	
}
