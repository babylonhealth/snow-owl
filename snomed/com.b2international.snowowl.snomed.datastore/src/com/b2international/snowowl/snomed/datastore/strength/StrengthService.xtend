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
package com.b2international.snowowl.snomed.datastore.strength

import com.b2international.snowowl.snomed.SnomedConstants.Concepts
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry
import com.google.common.collect.Multimaps
import com.google.common.collect.Range
import java.util.Collection

/**
 * Service implementation for extracting strength information from {@link SnomedRefSetMemberIndexEntry}s.
 */
class StrengthService implements IStrengthService {
	
	val CD_NUMERATOR_VALUE = "NumeratorValue";
	val CD_NUMERATOR_MIN_VALUE = "NumeratorMinValue";
	val CD_NUMERATOR_MAX_VALUE = "NumeratorMaxValue";
	val CD_DENOMINATOR_VALUE = "DenominatorValue";
	val CD_DELIMITER = "Delimiter";
	val CD_RANGE_NUMERATOR_MIN_VALUE = "RangeNumeratorMinValue";
	val CD_RANGE_NUMERATOR_MAX_VALUE = "RangeNumeratorMaxValue";
	
	val ALLOWED_STRENGTH_SUFFIXES = #[
		CD_RANGE_NUMERATOR_MIN_VALUE, CD_RANGE_NUMERATOR_MAX_VALUE, CD_NUMERATOR_VALUE, 
		CD_NUMERATOR_MIN_VALUE, CD_NUMERATOR_MAX_VALUE, CD_DENOMINATOR_VALUE, CD_DELIMITER
	]
	
	/**
	 * Returns a collection of {@link StrengthEntry} for the specified collection of {@link SnomedRefSetMemberIndexEntry}s
	 * 
	 * @param entries
	 * @return
	 */
	override getStrengths(Collection<SnomedRefSetMemberIndexEntry> entries) {
		if (!entries.empty) {
			val result = <StrengthEntry>newArrayList()
			val map = Multimaps.index(entries)[removeStrengthSuffixes(it.attributeName)]
			map.asMap.forEach[ name, cds |
				if (cds.simpleType) {
					result += name.createSimpleStrength(cds)
				} else if (cds.simpleRangeType) {
					result += name.createSimpleRangeStrength(cds)
				} else if (cds.ratioType) {
					result += name.createRatioStrength(cds)
				} else if (cds.ratioRangeType) {
					result += name.createRatioRangeStrength(cds)
				} else {
					throw new IllegalArgumentException("Unable to convert concrete domain to strength.")
				}
			]
			return result
		}
		return emptyList
	}
	
	def private removeStrengthSuffixes(String label) {
		ALLOWED_STRENGTH_SUFFIXES.fold(label, [c,n | c.replace(n, '')])
	}
	
	def private isSimpleType(Collection<SnomedRefSetMemberIndexEntry> entries) {
		entries.size == 1 && 
		entries.head.attributeName.endsWith(CD_NUMERATOR_VALUE)
	}
	
	def private isSimpleRangeType(Collection<SnomedRefSetMemberIndexEntry> entries) {
		Range.closed(1,2).contains(entries.size) && 
		entries.forall[attributeName.endsWithMinLabel || attributeName.endsWithMaxLabel]
	}
	
	def private isRatioType(Collection<SnomedRefSetMemberIndexEntry> entries) {
		Range.closed(2,3).contains(entries.size) && 
		entries.exists[attributeName.endsWith(CD_NUMERATOR_VALUE)] && 
		entries.exists[attributeName.endsWith(CD_DENOMINATOR_VALUE)]
	}
	
	def private isRatioRangeType(Collection<SnomedRefSetMemberIndexEntry> entries) {
		Range.closed(2,4).contains(entries.size) && 
		entries.exists[attributeName.endsWithMinLabel || attributeName.endsWithMaxLabel] && 
		entries.exists[attributeName.endsWith(CD_DENOMINATOR_VALUE)]
	}
	
	def private endsWithMinLabel(String it) {
		endsWith(CD_NUMERATOR_MIN_VALUE) || endsWith(CD_RANGE_NUMERATOR_MIN_VALUE)
	}
	
	def private endsWithMaxLabel(String it) {
		endsWith(CD_NUMERATOR_MAX_VALUE) || endsWith(CD_RANGE_NUMERATOR_MAX_VALUE)
	}
	
	def private StrengthEntry createSimpleStrength(String name, Collection<SnomedRefSetMemberIndexEntry> entries) {
		val numeratorEntry = entries.head
		return new StrengthEntry(name, numeratorEntry.valueAs, numeratorEntry.unitId.toLong)
	}
	
	def private StrengthEntry createSimpleRangeStrength(String name, Collection<SnomedRefSetMemberIndexEntry> entries) {
		val numeratorMinEntry = entries.findFirst[attributeName.endsWithMinLabel]
		val numeratorMaxEntry = entries.findFirst[attributeName.endsWithMaxLabel]
		val numeratorUnit = if (numeratorMinEntry != null) numeratorMinEntry.unitId.toLong else numeratorMaxEntry.unitId.toLong 
		return new StrengthEntry(name, numeratorMinEntry?.valueAs, numeratorMaxEntry?.valueAs, numeratorUnit) 
	}
	
	def private StrengthEntry createRatioStrength(String name, Collection<SnomedRefSetMemberIndexEntry> entries) {
		val numeratorEntry = entries.findFirst[attributeName.endsWith(CD_NUMERATOR_VALUE)]
		val denominatorEntry = entries.findFirst[attributeName.endsWith(CD_DENOMINATOR_VALUE)]
		val delimiterEntry = entries.findFirst[attributeName.endsWith(CD_DELIMITER)]
		
		val isDefaultUnit = isDefaultUnit(numeratorEntry.unitId, denominatorEntry.unitId)
		
		val numeratorUnit = if (isDefaultUnit) -1L else numeratorEntry.unitId.toLong
		val denominatorUnit = if (isDefaultUnit) -1L else denominatorEntry.unitId.toLong
		
		val delimiter = if (delimiterEntry != null) getDelimiterFor(delimiterEntry.value) ?: StrengthEntryDelimiter.SLASH else StrengthEntryDelimiter.SLASH
		
		return new StrengthEntry(name, numeratorEntry.valueAs, numeratorUnit, delimiter, denominatorEntry.valueAs, denominatorUnit)
	}
	
	def private StrengthEntry createRatioRangeStrength(String name, Collection<SnomedRefSetMemberIndexEntry> entries) {
		val numeratorMinEntry = entries.findFirst[attributeName.endsWithMinLabel]
		val numeratorMaxEntry = entries.findFirst[attributeName.endsWithMaxLabel]
		val denominatorEntry = entries.findFirst[attributeName.endsWith(CD_DENOMINATOR_VALUE)]
		val delimiterEntry = entries.findFirst[attributeName.endsWith(CD_DELIMITER)]
		
		val numeratorUnit = if (numeratorMinEntry != null) numeratorMinEntry.unitId.toLong else numeratorMaxEntry.unitId.toLong
		val denominatorUnit = denominatorEntry.unitId.toLong
		
		val delimiter = if (delimiterEntry != null)  getDelimiterFor(delimiterEntry.value) ?: StrengthEntryDelimiter.SLASH else StrengthEntryDelimiter.SLASH
		
		return new StrengthEntry(name, numeratorMinEntry?.valueAs, numeratorMaxEntry?.valueAs, numeratorUnit, delimiter, denominatorEntry.valueAs, denominatorUnit)
	}
	
	def private isDefaultUnit(String numeratorUnit, String denominatorUnit) {
		numeratorUnit == Concepts.DEFAULT_UNIT && denominatorUnit == Concepts.DEFAULT_UNIT
	}
	
	def private toLong(String element) {
		Long.valueOf(element)
	}
	
	def private StrengthEntryDelimiter getDelimiterFor(String value) {
		return StrengthEntryDelimiter.values.findFirst[value == it.literal]
	}
	
}