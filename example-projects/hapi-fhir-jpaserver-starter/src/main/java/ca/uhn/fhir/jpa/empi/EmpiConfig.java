package ca.uhn.fhir.jpa.empi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.rules.config.MdmRuleValidator;
import ca.uhn.fhir.mdm.rules.config.MdmSettings;
import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.jpa.mdm.config.MdmSubmitterConfig;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.util.ISearchParamRetriever;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * TODO: Move this to package "ca.uhn.fhir.jpa.starter" in HAPI FHIR 5.2.0+. The lousy component scan
 * in 5.1.0 picks this up even if EMPI is disabled currently.
 */
@Configuration
@Conditional(EmpiConfigCondition.class)

@Import({SubscriptionSubmitterConfig.class, SubscriptionChannelConfig.class,MdmConsumerConfig.class, MdmSubmitterConfig.class})
public class EmpiConfig {

  @Bean
  MdmRuleValidator empiRuleValidator(FhirContext theFhirContext, ISearchParamRetriever theSearchParamRetriever) {
    return new MdmRuleValidator(theFhirContext, theSearchParamRetriever);
  }

  @Bean
  IMdmSettings empiSettings(@Autowired MdmRuleValidator theEmpiRuleValidator, AppProperties appProperties) throws IOException {
    DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
    Resource resource = resourceLoader.getResource("empi-rules.json");
    String json = IOUtils.toString(resource.getInputStream(), Charsets.UTF_8);
    return new MdmSettings(theEmpiRuleValidator).setEnabled(appProperties.getEmpi_enabled()).setScriptText(json);
  }

}
