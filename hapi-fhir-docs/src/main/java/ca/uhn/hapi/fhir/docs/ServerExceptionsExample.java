package ca.uhn.hapi.fhir.docs;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

public abstract class ServerExceptionsExample implements IResourceProvider {

private boolean databaseIsDown;

//START SNIPPET: returnOO
@Read
public Patient read(@IdParam IdType theId) {
   if (databaseIsDown) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(new CodeableConcept().setText("Database is down"));
      throw new InternalErrorException("Database is down", oo);
   }
   
   Patient patient = new Patient(); // populate this
   return patient;
}
//END SNIPPET: returnOO


}


