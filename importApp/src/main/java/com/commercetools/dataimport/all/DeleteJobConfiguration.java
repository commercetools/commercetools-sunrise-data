package com.commercetools.dataimport.all;

import com.commercetools.dataimport.categories.CategoriesDeleteJobConfiguration;
import com.commercetools.dataimport.commercetools.CommercetoolsConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.products.ProductDeleteJobConfiguration;
import com.commercetools.dataimport.producttypes.ProductTypeDeleteJobConfiguration;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class DeleteJobConfiguration extends CommercetoolsJobConfiguration {
    @Configuration
    public static class MainConfiguration {//TODO improve
        public MainConfiguration() {
        }
    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class,
                DeleteJobConfiguration.class,
                MainConfiguration.class,
                ProductDeleteJobConfiguration.class,
                CategoriesDeleteJobConfiguration.class,
                ProductTypeDeleteJobConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
