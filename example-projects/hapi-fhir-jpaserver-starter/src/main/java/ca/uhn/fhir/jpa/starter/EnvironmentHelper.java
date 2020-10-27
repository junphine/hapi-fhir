package ca.uhn.fhir.jpa.starter;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class EnvironmentHelper {

  public static Properties getHibernateProperties(ConfigurableEnvironment environment) {
    Properties properties = new Properties();

    if (environment.getProperty("spring.jpa.properties.hibernate.dialect", String.class) == null) {
      properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL10Dialect");
      properties.put("hibernate.search.model_mapping", "ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory");
      properties.put("hibernate.format_sql", "false");
      properties.put("hibernate.show_sql", "true");
      properties.put("hibernate.hbm2ddl.auto", "update");
      properties.put("hibernate.jdbc.batch_size", "20");
      properties.put("hibernate.cache.use_query_cache", "false");
      properties.put("hibernate.cache.use_second_level_cache", "false");
      properties.put("hibernate.cache.use_structured_entries", "false");
      properties.put("hibernate.cache.use_minimal_puts", "false");
      properties.put("hibernate.search.default.directory_provider", "filesystem");
      properties.put("hibernate.search.default.indexBase", "target/lucenefiles");
      properties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
    } else {
      String[] names = {"hibernate.dialect","hibernate.search.model_mapping","hibernate.format_sql","hibernate.show_sql"
    		  ,"hibernate.hbm2ddl.auto","hibernate.jdbc.batch_size"
    		  ,"hibernate.cache.use_query_cache","hibernate.cache.use_second_level_cache","hibernate.cache.use_structured_entries", "hibernate.cache.use_minimal_puts"
    		  ,"hibernate.search.default.directory_provider","hibernate.search.default.indexBase","hibernate.search.lucene_version"
      };
      for(String name: names) {
        properties.put(name, environment.getProperty("spring.jpa.properties."+name, String.class));       
      }
    }
    return properties;
  }

  public static Map<String, Object> getPropertiesStartingWith(ConfigurableEnvironment aEnv,
                                                              String aKeyPrefix) {
    Map<String, Object> result = new HashMap<>();

    Map<String, Object> map = getAllProperties(aEnv);

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();

      if (key.startsWith(aKeyPrefix)) {
        result.put(key, entry.getValue());
      }
    }

    return result;
  }

  public static Map<String, Object> getAllProperties(ConfigurableEnvironment aEnv) {
    Map<String, Object> result = new HashMap<>();
    aEnv.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
    return result;
  }

  public static Map<String, Object> getAllProperties(PropertySource<?> aPropSource) {
    Map<String, Object> result = new HashMap<>();

    if (aPropSource instanceof CompositePropertySource) {
      CompositePropertySource cps = (CompositePropertySource) aPropSource;
      cps.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
      return result;
    }

    if (aPropSource instanceof EnumerablePropertySource<?>) {
      EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) aPropSource;
      Arrays.asList(ps.getPropertyNames()).forEach(key -> result.put(key, ps.getProperty(key)));
      return result;
    }

    return result;

  }

  private static void addAll(Map<String, Object> aBase, Map<String, Object> aToBeAdded) {
    for (Map.Entry<String, Object> entry : aToBeAdded.entrySet()) {
      if (aBase.containsKey(entry.getKey())) {
        continue;
      }

      aBase.put(entry.getKey(), entry.getValue());
    }
  }
}
