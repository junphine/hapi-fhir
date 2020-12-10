package ca.uhn.example;

import ca.uhn.example.config.FhirTesterConfig;
import ca.uhn.example.servlet.IgniteRestfulServlet;
import ca.uhn.fhir.rest.server.RestfulServer;

/*-
 * #%L
 * hapi-fhir-spring-boot-sample-server-jersey
 * %%
 * Copyright (C) 2014 - 2017 University Health Network
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

import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@ServletComponentScan
@SpringBootApplication(exclude = {ElasticsearchAutoConfiguration.class,SecurityAutoConfiguration.class})
public class IgniteRestfulServerApplication extends SpringBootServletInitializer{

    public static void main(String[] args) {
        SpringApplication.run(IgniteRestfulServerApplication.class, args);
    }

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }
    

    @Override
    protected SpringApplicationBuilder configure(
      SpringApplicationBuilder builder) {
      return builder.sources(IgniteRestfulServerApplication.class);
    }

    @Autowired
    AutowireCapableBeanFactory beanFactory;

    @Bean
    public ServletRegistrationBean<RestfulServer> hapiServletRegistration() {
      ServletRegistrationBean<RestfulServer> servletRegistrationBean = new ServletRegistrationBean<>();
      IgniteRestfulServlet restfulServer = new IgniteRestfulServlet();
      beanFactory.autowireBean(restfulServer);
      servletRegistrationBean.setServlet(restfulServer);
      servletRegistrationBean.addUrlMappings("/fhir/*");
      servletRegistrationBean.setLoadOnStartup(1);

      return servletRegistrationBean;
    }

    @Bean
    public ServletRegistrationBean<DispatcherServlet> overlayRegistrationBean() {

      AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
      annotationConfigWebApplicationContext.register(FhirTesterConfig.class);

      DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
      dispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
      dispatcherServlet.setContextConfigLocation(FhirTesterConfig.class.getName());

      ServletRegistrationBean<DispatcherServlet> registrationBean = new ServletRegistrationBean<>();
      registrationBean.setServlet(dispatcherServlet);
      registrationBean.addUrlMappings("/*");
      registrationBean.setLoadOnStartup(1);
      return registrationBean;

    }
}
