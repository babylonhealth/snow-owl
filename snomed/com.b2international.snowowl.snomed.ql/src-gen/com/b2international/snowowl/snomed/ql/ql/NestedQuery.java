/**
 * Copyright 2020-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.ql.ql;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Nested Query</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.b2international.snowowl.snomed.ql.ql.NestedQuery#getNested <em>Nested</em>}</li>
 * </ul>
 *
 * @see com.b2international.snowowl.snomed.ql.ql.QlPackage#getNestedQuery()
 * @model
 * @generated
 */
public interface NestedQuery extends SubQuery
{
  /**
   * Returns the value of the '<em><b>Nested</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the value of the '<em>Nested</em>' containment reference.
   * @see #setNested(QueryConstraint)
   * @see com.b2international.snowowl.snomed.ql.ql.QlPackage#getNestedQuery_Nested()
   * @model containment="true"
   * @generated
   */
  QueryConstraint getNested();

  /**
   * Sets the value of the '{@link com.b2international.snowowl.snomed.ql.ql.NestedQuery#getNested <em>Nested</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Nested</em>' containment reference.
   * @see #getNested()
   * @generated
   */
  void setNested(QueryConstraint value);

} // NestedQuery
