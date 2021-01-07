package ca.uhn.example;

import ca.uhn.example.config.FhirTesterConfig;
import ca.uhn.example.servlet.IgniteRestfulServlet;
import ca.uhn.fhir.rest.server.RestfulServer;


import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@ServletComponentScan
@SpringBootApplication(exclude = {
		//ElasticsearchAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
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
