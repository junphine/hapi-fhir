package ca.uhn.example.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.FullTextQueryIndex;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.springdata22.repository.config.EnableIgniteRepositories;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.example.model.VersionedId;
import ca.uhn.fhir.context.BaseRuntimeChildDatatypeDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildPrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author cjz
 * @date 2019-08-08 17:29
 **/
@Configuration
@EnableIgniteRepositories
public class IgniteAppCfg {

	@Value("${ignite.mode}")
	private String mode = "server";

	@Value("${ignite.connect.address}")
	private String address = "127.0.0.1";
	
	private final static String HISTORY_SUFFIX = "_History";

	@Bean
	public Ignite igniteInstance() {
		Ignite ignite = null;

		if (mode.equalsIgnoreCase("server")) {
			ignite = Ignition.start("config/fhir-ignite.xml");
		} else { // client
					// 客户端方式启动
			Ignition.setClientMode(true);
			ignite = Ignition.start("config/fhir-ignite.xml");
		}

		return ignite;
	}

	@Bean
	public IgniteClient igniteClient() {
		ClientConfiguration cfg = new ClientConfiguration();
		cfg.setAddresses(getAddress());
		cfg.setTimeout(10000);
		return Ignition.startClient(cfg);
	}

	public static <T extends BaseResource> CacheConfiguration<String, T> cacheConfigurationFor(
			FhirContext myFhirContext,
			Class<T> resourceType) {
		String resourceName = resourceType.getSimpleName();
		// Defining and creating a new cache to be used by Ignite Spring Data
		// repository.
		CacheConfiguration<String, T> ccfg2 = new CacheConfiguration<>(resourceName);

		// Setting SQL schema for the cache.
		ccfg2.setSqlSchema(myFhirContext.getVersion().getVersion().name());
		ccfg2.setCacheMode(CacheMode.PARTITIONED);
		
		QueryEntity queryEntity = createResourceQueryEntity(resourceType);
		try {
			RuntimeResourceDefinition def = myFhirContext.getResourceDefinition(resourceName);
			String id = def.getId();
			
			Map<String,String> fieldTypeMap = new HashMap<>();
			for(BaseRuntimeChildDefinition child: def.getChildren()) {
				if(child instanceof BaseRuntimeChildDatatypeDefinition) {
					BaseRuntimeChildDatatypeDefinition rchild = (BaseRuntimeChildDatatypeDefinition) child;
					if(rchild.getMax()<=1) {
						if(IPrimitiveType.class.isAssignableFrom(rchild.getDatatype())) {
							DatatypeDef dataTypeDef = rchild.getDatatype().getAnnotation(DatatypeDef.class);
							if(dataTypeDef!=null && dataTypeDef.name()!=null) {
								fieldTypeMap.put(rchild.getElementName(), String.class.getName());
							}
						}
					}
				}
				else {
					if(child.getMax()<=1 && child instanceof RuntimeChildPrimitiveDatatypeDefinition) {
						fieldTypeMap.put(child.getElementName(), String.class.getName());
					}
					
				}
				
			}
			for(RuntimeSearchParam param: def.getSearchParams()) {
				String field = param.getName();
				if(field!=null) {
					if(fieldTypeMap.containsKey(field)) {
						queryEntity.addQueryField(field, String.class.getName(), null);
						queryEntity.getIndexes().add(new QueryIndex(field,false));
					}
				}
			}
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		ccfg2.setQueryEntity(queryEntity);
		return ccfg2;
	}

	public static <T extends BaseResource> CacheConfiguration<VersionedId, T> historyCacheConfigurationFor(
			FhirContext myFhirContext,
			Class<T> resourceType) {
		String resourceName = resourceType.getSimpleName();
		// Defining and creating a new cache to be used by Ignite Spring Data
		// repository.
		CacheConfiguration<VersionedId, T> ccfg2 = new CacheConfiguration<>(resourceName+HISTORY_SUFFIX);

		// Setting SQL schema for the cache.
		ccfg2.setSqlSchema(myFhirContext.getVersion().getVersion().name()+HISTORY_SUFFIX);
		ccfg2.setCacheMode(CacheMode.PARTITIONED);
		ccfg2.setBackups(0);
		ccfg2.setQueryEntity(createVersionedQueryEntity(resourceType));

		
		return ccfg2;
	}
	
	 /**
     * Create cache type metadata for {@link Organization}.
     *
     * @return Cache type metadata.
     */
    private static QueryEntity createResourceQueryEntity(Class<? extends BaseResource> resourceType) {
    	List<QueryIndex> indexes = new ArrayList<>();
    	QueryEntity entity = new QueryEntity()
            .setValueType(resourceType.getName())
            .setKeyType(String.class.getName())            
            .addQueryField("id", String.class.getName(), null)
            .addQueryField("text", String.class.getName(), null)
            .setKeyFieldName("id")
            .setIndexes(indexes);
    	
    	//indexes.add(new QueryIndex("text"));
    	indexes.add(new FullTextQueryIndex("text"));
    	
    	return entity;
    }

    

    /**
     * Create cache type metadata for {@link Employee}.
     *
     * @return Cache type metadata.
     */
    private static QueryEntity createVersionedQueryEntity(Class<? extends BaseResource> resourceType) {
        return new QueryEntity()
        	.setTableName(resourceType.getSimpleName()+HISTORY_SUFFIX)
            .setValueType(resourceType.getName())
            .setKeyType(VersionedId.class.getName())
            .addQueryField("version", Long.class.getName(), null)
            .addQueryField("idPart", String.class.getName(), "id_part")
            .addQueryField("id", String.class.getName(), null)
            .addQueryField("status", String.class.getName(), null)            
            .addQueryField("meta", Meta.class.getName(), null)            
            .setKeyFields(new HashSet<>(Arrays.asList("version","idPart")))
           ;
    }
    
	public String getAddress() {
		return address;
	}

	public void setAddress(String ip) {
		this.address = ip;
	}
}
