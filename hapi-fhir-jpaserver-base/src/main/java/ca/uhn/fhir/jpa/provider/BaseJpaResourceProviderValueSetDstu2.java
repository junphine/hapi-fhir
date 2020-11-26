package ca.uhn.fhir.jpa.provider;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.hl7.fhir.instance.model.api.IBaseParameters;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
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
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.util.ParametersUtil;

public class BaseJpaResourceProviderValueSetDstu2 {



	private String toFilterString(StringDt theFilter) {
		return theFilter != null ? theFilter.getValue() : null;
	}

	public static IBaseParameters toValidateCodeResult(FhirContext theContext, IValidationSupport.CodeValidationResult theResult) {
		IBaseParameters retVal = ParametersUtil.newInstance(theContext);

		ParametersUtil.addParameterToParametersBoolean(theContext, retVal, "result", theResult.isOk());
		if (isNotBlank(theResult.getMessage())) {
			ParametersUtil.addParameterToParametersString(theContext, retVal, "message", theResult.getMessage());
		}
		if (isNotBlank(theResult.getDisplay())) {
			ParametersUtil.addParameterToParametersString(theContext, retVal, "display", theResult.getDisplay());
		}

		return retVal;
	}

	private static boolean moreThanOneTrue(boolean... theBooleans) {
		boolean haveOne = false;
		for (boolean next : theBooleans) {
			if (next) {
				if (haveOne) {
					return true;
				} else {
					haveOne = true;
				}
			}
		}
		return false;
	}


}