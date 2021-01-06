package ca.uhn.hapi.fhir.docs;

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;

/*-
 * #%L
 * HAPI FHIR - Docs
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

public class ResourceRefs {

   private static FhirContext ourCtx = FhirContext.forDstu2();

   public static void main(String[] args) {
      manualContained();
   }

   public static void manualContained() {
      // START SNIPPET: manualContained
      // Create an organization, and give it a local ID
      Organization org = new Organization();
      org.setId("#localOrganization");
      org.getNameElement().setValue("Contained Test Organization");

      // Create a patient
      Patient patient = new Patient();
      patient.setId("Patient/1333");
      patient.addIdentifier().setSystem("urn:mrns").setValue("253345");

      // Set the reference, and manually add the contained resource
      patient.getManagingOrganization().setReference("#localOrganization");
      patient.getContained().add(org);

      String encoded = ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
      System.out.println(encoded);
      // END SNIPPET: manualContained
   }

}
