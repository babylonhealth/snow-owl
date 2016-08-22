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
package com.b2international.snowowl.semanticengine.simpleast.normalform.test;

import org.junit.Test;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.dsl.ESCGEcoreRewriter;
import com.b2international.snowowl.dsl.escg.Expression;
import com.b2international.snowowl.dsl.parser.antlr.ESCGParser;
import com.b2international.snowowl.semanticengine.simpleast.normalform.SimpleAstExpressionNormalFormGenerator;
import com.b2international.snowowl.semanticengine.simpleast.test.utils.TestUtils;
import com.b2international.snowowl.snomed.dsl.query.queryast.RValue;

/**
 *
 */
public class SimpleAstNormalFormGeneratorTest {
	
	private static final ESCGParser escgParser = TestUtils.createESCGParser();
	
	@Test
	public void testAuscultation() {
		Expression originalExpression = TestUtils.parseExpression(escgParser, "37931006 | auscultation | :" +
				"260686004 | method | = 129436005 | auscultation - action |");
		Expression expectedLongNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"315306007 | examination by method | : 260686004 | method | = 129436005 | auscultation - action |");
		Expression expectedShortNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"315306007 | examination by method | : 260686004 | method | = 129436005 | auscultation - action |");	// TODO: check short form
		testNormalFormGenerator(originalExpression, expectedLongNormalFormExpression, expectedShortNormalFormExpression);
	}
	
	@Test
	public void testExpiratoryCrackles() {
		Expression originalExpression = TestUtils.parseExpression(escgParser, "12529006 | expiratory crackles | ");
		Expression expectedLongNormalFormExpression = TestUtils.parseExpression(escgParser,
				"12529006 | expiratory crackles | :" +
				"363698007 | finding site | = 82094008 | lower respiratory tract structure |," +
				"418775008 | finding method | =" +
				"(37931006 | auscultation | : " +
				"260686004 | method | = 129436005 | auscultation - action |)");
		Expression expectedShortNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"12529006 | expiratory crackles | ");	// TODO: verify short normal form
		testNormalFormGenerator(originalExpression, expectedLongNormalFormExpression, expectedShortNormalFormExpression);
	}

	//////////////////////////////////////////////// tests from Orsi ///////////////////////////////////////////
	
	@Test
	public void testTetralogyOfFallot() {
		Expression originalExpression = TestUtils.parseExpression(escgParser, "86299006 | tetralogy of fallot |");
		Expression expectedLongNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"86299006 | tetralogy of Fallot | :"+
				"246454002 | occurrence | = 255399007 | congenital |"+ 
				"{ 116676008 | associated morphology | = 107656002 | congenital anomaly |"+ 
				", 363698007 | finding site | = 21814001 | cardiac ventricular structure |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 30812002 | overriding structures |"+ 
				", 363698007 | finding site | = 113262008 | thoracic aorta structure |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 415582006 | stenosis |"+ 
				", 363698007 | finding site | = 39057004 | pulmonary valve structure |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 56246009 | hypertrophy |"+ 
				", 363698007 | finding site | = 53085002 | right ventricular structure |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 6920004 | defect |"+ 
				", 363698007 | finding site | = 589001 | interventricular septum structure |"+ 
				"}");
		Expression expectedShortNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"86299006 | tetralogy of Fallot |");
		testNormalFormGenerator(originalExpression, expectedLongNormalFormExpression, expectedShortNormalFormExpression);
	}
	
	@Test
	public void testExcisionOfSubmandibularGland() {
		Expression originalExpression = TestUtils.parseExpression(escgParser, "47227006 | excision of submandibular gland | ");
		Expression expectedLongNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"71388002 | procedure | :"+
				"{ 260686004 | method | = 129304002 | excision - action |"+ 
				", 405813007 | procedure site - Direct | = 385296007 | submandibular salivary gland structure |"+ 
				"}");
		Expression expectedShortNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"71388002 | procedure | :"+
				"{ 260686004 | method | = 129304002 | excision - action |"+ 
				", 405813007 | procedure site - Direct | = 385296007 | submandibular salivary gland structure |}");
		testNormalFormGenerator(originalExpression, expectedLongNormalFormExpression, expectedShortNormalFormExpression);
	}
	
	@Test
	public void testGaleazziFractureDislocation() {
		Expression originalExpression = TestUtils.parseExpression(escgParser, "271576001 | Galeazzi fracture dislocation |");
		Expression expectedLongNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"263079005 | fracture dislocation of wrist joint | :"+
				"{ 116676008 | associated morphology | = 72704001 | fracture |"+ 
				", 363698007 | finding site | = 299706009 | bone structure of wrist and/or hand |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 72704001 | fracture |"+ 
				", 363698007 | finding site | = 47728000 | bone structure of shaft of radius |"+ 
				"}"+
				"{ 116676008 | associated morphology | = 87642003 | dislocation |"+ 
				", 363698007 | finding site | = 74670003 | wrist joint structure |"+ 
				"}");
		Expression expectedShortNormalFormExpression = TestUtils.parseExpression(escgParser, 
				"263079005 | fracture dislocation of wrist joint | :"+
				"{ 116676008 | associated morphology | = 72704001 | fracture |"+ 
				", 363698007 | finding site | = 47728000 | bone structure of shaft of radius |}");
		testNormalFormGenerator(originalExpression, expectedLongNormalFormExpression, expectedShortNormalFormExpression);
	}

	private void testNormalFormGenerator(Expression originalExpression, Expression expectedLongNormalFormExpression, 
			Expression expectedShortNormalFormExpression) {
		SimpleAstExpressionNormalFormGenerator normalFormGenerator = new SimpleAstExpressionNormalFormGenerator(Branch.MAIN_PATH);
		ESCGEcoreRewriter rewriter = new ESCGEcoreRewriter(escgParser);
		RValue rewrittenOriginalExpression = rewriter.rewrite(originalExpression);
		System.out.println("Original: " + rewrittenOriginalExpression);
		RValue longNormalFormExpression = normalFormGenerator.getLongNormalForm(rewrittenOriginalExpression);
		System.out.println("Normal form: " + longNormalFormExpression);
//		TestUtils.assertExpressionsEqual("Long normal form expression does not match,", expectedLongNormalFormExpression, longNormalFormExpression);
		
//		normalFormGenerator = new BasicExpressionNormalFormGenerator(terminologyBrowser, statementBrowser);
//		Expression shortNormalFormExpression = normalFormGenerator.getShortNormalForm(originalExpression);
//		TestUtils.assertExpressionsEqual("Short normal form expression does not match,", expectedShortNormalFormExpression, shortNormalFormExpression);
	}

}
