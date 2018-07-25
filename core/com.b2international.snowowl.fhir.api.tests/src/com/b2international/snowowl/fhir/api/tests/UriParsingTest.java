/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.api.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.fhir.SnomedUri;

/**
 * 
 * @since 6.7
 */
public class UriParsingTest extends FhirTest {
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void testInvalidBase() {
		
		String uriString = "invalid uri";
		
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("URI 'invalid uri' is not a valid SNOMED CT URI. It should start as 'http://snomed.info/sct'");

		SnomedUri.fromUriString(uriString);
	}
	
	@Test
	public void testInvalidModule() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING + "/a";
		
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Invalid extension module ID [a] defined.");

		SnomedUri.fromUriString(uriString);
	}
	
	@Test
	public void testInvalidVersionTag() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING + "/900000000000207008/invalidTag";
		
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Invalid path segment [invalidTag], 'version' expected.");

		SnomedUri.fromUriString(uriString);
	}
	
	@Test
	public void testMissingVersionTag() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING + "/900000000000207008/version";
		
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("No version tag is specified after the 'version' parameter.");

		SnomedUri.fromUriString(uriString);
	}

	//correct paths
	@Test
	public void testIntEditionUri() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING;
		
		SnomedUri snomedUri = SnomedUri.fromUriString(uriString);
		
		assertNull(snomedUri.getExtensionModuleId());
		assertNull(snomedUri.getVersionTag());
		
	}
	
	@Test
	public void testIntEditionWithModuleUri() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING + "/" + Concepts.MODULE_SCT_CORE;
		
		SnomedUri snomedUri = SnomedUri.fromUriString(uriString);
		
		assertEquals(Concepts.MODULE_SCT_CORE, snomedUri.getExtensionModuleId());
		assertNull(snomedUri.getVersionTag());
	}
	
	@Test
	public void testIntEditionWithVersionUri() {
		
		String uriString = SnomedUri.SNOMED_BASE_URI_STRING + "/" + Concepts.MODULE_SCT_CORE + "/version/20170131";
		
		SnomedUri snomedUri = SnomedUri.fromUriString(uriString);
		
		assertEquals(Concepts.MODULE_SCT_CORE, snomedUri.getExtensionModuleId());
		assertEquals("20170131", snomedUri.getVersionTag());
	}

}
