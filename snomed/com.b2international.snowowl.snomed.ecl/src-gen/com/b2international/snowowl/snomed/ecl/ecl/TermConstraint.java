/**
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.ecl.ecl;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Term Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.b2international.snowowl.snomed.ecl.ecl.TermConstraint#getComparison <em>Comparison</em>}</li>
 * </ul>
 *
 * @see com.b2international.snowowl.snomed.ecl.ecl.EclPackage#getTermConstraint()
 * @model
 * @generated
 */
public interface TermConstraint extends Refinement
{
  /**
   * Returns the value of the '<em><b>Comparison</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Comparison</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Comparison</em>' containment reference.
   * @see #setComparison(TermComparison)
   * @see com.b2international.snowowl.snomed.ecl.ecl.EclPackage#getTermConstraint_Comparison()
   * @model containment="true"
   * @generated
   */
  TermComparison getComparison();

  /**
   * Sets the value of the '{@link com.b2international.snowowl.snomed.ecl.ecl.TermConstraint#getComparison <em>Comparison</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Comparison</em>' containment reference.
   * @see #getComparison()
   * @generated
   */
  void setComparison(TermComparison value);

} // TermConstraint
