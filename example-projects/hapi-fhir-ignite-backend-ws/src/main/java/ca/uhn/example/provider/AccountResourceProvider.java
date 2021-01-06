package ca.uhn.example.provider;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Account;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;

import com.google.common.collect.Lists;

import ca.uhn.example.base.MemoryCacheResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class AccountResourceProvider extends MemoryCacheResourceProvider<Account> implements IResourceProvider {

	public AccountResourceProvider(FhirContext theFhirContext) {
		super(theFhirContext);
		// TODO Auto-generated constructor stub
	}

	

	@Search()
	public List<Account> findAccountsByName(
			@RequiredParam(name = Account.SP_NAME) final StringType theName) {
//		throw new UnprocessableEntityException(
//				"Please provide more than 4 characters for the name");
		return Lists.newArrayList();
	}

}
