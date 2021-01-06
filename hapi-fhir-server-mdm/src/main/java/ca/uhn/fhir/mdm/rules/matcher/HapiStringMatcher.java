package ca.uhn.fhir.mdm.rules.matcher;

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
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * Similarity measure for two IBase fields whose similarity can be measured by their String representations.
 */
public class HapiStringMatcher extends BaseHapiStringMetric implements IMdmFieldMatcher {
	private final IMdmStringMatcher myStringMatcher;

	public HapiStringMatcher(IMdmStringMatcher theStringMatcher) {
		myStringMatcher = theStringMatcher;
	}

	public HapiStringMatcher() {
		myStringMatcher = String::equals;
	}

	@Override
	public boolean matches(FhirContext theFhirContext, IBase theLeftBase, IBase theRightBase, boolean theExact, String theIdentifierSystem) {
		if (theLeftBase instanceof IPrimitiveType && theRightBase instanceof IPrimitiveType) {
			String leftString = extractString((IPrimitiveType<?>) theLeftBase, theExact);
			String rightString = extractString((IPrimitiveType<?>) theRightBase, theExact);

			return myStringMatcher.matches(leftString, rightString);
		}
		return false;
	}
}
