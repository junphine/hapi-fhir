package ca.uhn.fhir.mdm.util;

/*-
 * #%L
 * HAPI FHIR - Master Data Management
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
 * %%
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
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.PrimitiveType;

import java.util.List;
import java.util.stream.Collectors;

public final class NameUtil {

	private NameUtil() {
	}

	public static List<String> extractGivenNames(FhirContext theFhirContext, IBase theBase) {
		switch(theFhirContext.getVersion().getVersion()) {
			case R4:
				HumanName humanNameR4 = (HumanName)theBase;
				return humanNameR4.getGiven().stream().map(PrimitiveType::getValueAsString).filter(s -> !StringUtils.isEmpty(s)).collect(Collectors.toList());
			
			default:
				throw new UnsupportedOperationException("Version not supported: " + theFhirContext.getVersion().getVersion());

		}
	}

	public static String extractFamilyName(FhirContext theFhirContext, IBase theBase) {
		switch(theFhirContext.getVersion().getVersion()) {
			case R4:
				HumanName humanNameR4 = (HumanName)theBase;
				return humanNameR4.getFamily();
			
			default:
				throw new UnsupportedOperationException("Version not supported: " + theFhirContext.getVersion().getVersion());

		}
	}
}
