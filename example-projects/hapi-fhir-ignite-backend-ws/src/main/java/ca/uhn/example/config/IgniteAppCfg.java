package ca.uhn.example.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.springdata22.repository.config.EnableIgniteRepositories;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.example.model.VersionedId;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;

import java.util.Arrays;

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

	@Bean
	public Ignite igniteInstance() {
		Ignite ignite = null;

		if (mode.equalsIgnoreCase("server")) {
			ignite = Ignition.start("config/example-ignite.xml");
		} else { // client
					// 客户端方式启动
			Ignition.setClientMode(true);
			ignite = Ignition.start("config/example-ignite.xml");
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

	public static <T> CacheConfiguration<String, T> cacheConfigurationFor(FhirContext myFhirContext,
			String resourceName) {
		resourceName = org.apache.commons.lang3.ClassUtils.getShortClassName(resourceName);
		// Defining and creating a new cache to be used by Ignite Spring Data
		// repository.
		CacheConfiguration<String, T> ccfg2 = new CacheConfiguration<>(resourceName);

		// Setting SQL schema for the cache.
		ccfg2.setSqlSchema(myFhirContext.getVersion().toString());
		ccfg2.setCacheMode(CacheMode.PARTITIONED);

		RuntimeResourceDefinition def = myFhirContext.getResourceDefinition(resourceName);
		def.getId();
		return ccfg2;
	}

	public static <T> CacheConfiguration<VersionedId, T> historyCacheConfigurationFor(FhirContext myFhirContext,
			String resourceName) {
		resourceName = org.apache.commons.lang3.ClassUtils.getShortClassName(resourceName);
		// Defining and creating a new cache to be used by Ignite Spring Data
		// repository.
		CacheConfiguration<VersionedId, T> ccfg2 = new CacheConfiguration<>(resourceName+"_history");

		// Setting SQL schema for the cache.
		ccfg2.setSqlSchema(myFhirContext.getVersion().toString());
		ccfg2.setCacheMode(CacheMode.PARTITIONED);

		RuntimeResourceDefinition def = myFhirContext.getResourceDefinition(resourceName);
		def.getId();
		return ccfg2;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String ip) {
		this.address = ip;
	}
}
