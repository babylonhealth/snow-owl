/*
 * Copyright 2020 B2i Healthcare Pte Ltd, http://b2i.sg
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
<<<<<<< HEAD:snomed/com.b2international.snowowl.snomed.scg/src/com/b2international/snowowl/snomed/scg/ScgStandaloneSetup.java
package com.b2international.snowowl.snomed.scg;
=======
package com.b2international.snowowl.snomed.etl;
>>>>>>> 7.x:snomed/com.b2international.snowowl.snomed.etl/src/com/b2international/snowowl/snomed/etl/EtlStandaloneSetup.java


/**
 * Initialization support for running Xtext languages without Equinox extension registry.
 */
<<<<<<< HEAD:snomed/com.b2international.snowowl.snomed.scg/src/com/b2international/snowowl/snomed/scg/ScgStandaloneSetup.java
public class ScgStandaloneSetup extends ScgStandaloneSetupGenerated {

	public static void doSetup() {
		new ScgStandaloneSetup().createInjectorAndDoEMFRegistration();
=======
public class EtlStandaloneSetup extends EtlStandaloneSetupGenerated {

	public static void doSetup() {
		new EtlStandaloneSetup().createInjectorAndDoEMFRegistration();
>>>>>>> 7.x:snomed/com.b2international.snowowl.snomed.etl/src/com/b2international/snowowl/snomed/etl/EtlStandaloneSetup.java
	}
}
