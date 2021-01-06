package ca.uhn.example.servlet;

import java.util.*;

import org.apache.ignite.Ignite;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.example.base.FhirBinarySerializer;
import ca.uhn.example.base.IgniteCacheR4ResourceProvider;
import ca.uhn.example.base.MemoryCacheResourceProvider;
import ca.uhn.example.provider.AccountResourceProvider;
import ca.uhn.example.provider.OrganizationResourceProvider;
import ca.uhn.example.provider.PatientResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

/**
 * This servlet is the actual FHIR server r4 itself
 */
public class IgniteRestfulServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;	
	
	 
	@Autowired
	Ignite ignite;
	
	/**
	 * Constructor
	 */
	public IgniteRestfulServlet() {
		super(FhirContext.forR4()); // Support R4
		FhirBinarySerializer.globalFhirContext = this.getFhirContext();
	}
	
	/**
	 * This method is called automatically when the
	 * servlet is initializing.
	 */
	@Override
	public void initialize() {
		/*
		 * Two resource providers are defined. Each one handles a specific
		 * type of resource.
		 */
		List<IResourceProvider> providers = new ArrayList<>();
		providers.add(new PatientResourceProvider());
		//providers.add(new OrganizationResourceProvider());
		providers.add(new AccountResourceProvider(this.getFhirContext()));
		
		for(String resourceName: this.getFhirContext().getResourceTypes()) {
			Resource resouceType = ResourceFactory.createResource(resourceName);
			
			providers.add(new IgniteCacheR4ResourceProvider<>(this.getFhirContext(),resouceType.getClass(),ignite));
		}
		setResourceProviders(providers);
		
		/*
		 * Use a narrative generator. This is a completely optional step, 
		 * but can be useful as it causes HAPI to generate narratives for
		 * resources which don't otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Enable CORS
		 */
		CorsConfiguration config = new CorsConfiguration();
		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("Content-Type");
		config.addAllowedOrigin("*");
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
		registerInterceptor(corsInterceptor);

		/*
		 * This server interceptor causes the server to return nicely
		 * formatter and coloured responses instead of plain JSON/XML if
		 * the request is coming from a browser window. It is optional,
		 * but can be nice for testing.
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());
		
		/*
		 * Tells the server to return pretty-printed responses by default
		 */
		setDefaultPrettyPrint(true);
		
	}

}
