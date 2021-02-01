package ca.uhn.example.provider;

import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.*;

import ca.uhn.example.model.Dataset;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * This is a simple resource provider which only implements "read/GET" methods, but
 * which uses a custom subclassed resource definition to add statically bound
 * extensions.
 * 
 * See the MyOrganization definition to see how the custom resource 
 * definition works.
 */
public class DatasetResourceProvider implements IResourceProvider {

	/**
	 * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Dataset> getResourceType() {
		return Dataset.class;
	}

	/**
	 * The "@Read" annotation indicates that this method supports the read operation. It takes one argument, the Resource type being returned.
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 */
	@Read()
	public Dataset getResourceById(@IdParam IdType theId) {
		
		/*
		 * We only support one organization, so the follwing
		 * exception causes an HTTP 404 response if the 
		 * ID of "1" isn't used.
		 */
		if (!"1".equals(theId.getIdPart())) {
			throw new ResourceNotFoundException(theId);
		}
		
		Dataset retVal = new Dataset();
		retVal.setId("1");
		retVal.getMeta().setVersionId("2");
		//retVal.addIdentifier().setSystem("urn:example:orgs").setValue("FooOrganization");
		//retVal.addAddress().addLine("123 Fake Street").setCity("Toronto");
		//retVal.addTelecom().setUse(ContactPointUse.WORK).setValue("1-888-123-4567");
		
		// Populate the first, primitive extension
		retVal.setBillingCode(new CodeType("00102-1"));
		
		// The second extension is repeatable and takes a block type
		Dataset.EmergencyContact contact = new Dataset.EmergencyContact();
		contact.setActive(new BooleanType(true));
		contact.setContact(new ContactPoint());
		retVal.getEmergencyContact().add(contact);
		
		return retVal;
	}


	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createResource(@ResourceParam Dataset theDataset) {
		
		String id = theDataset.getId();
		id = "new";

		// Let the caller know the ID of the newly created resource
		return new MethodOutcome(new IdType(id));
	}

}
