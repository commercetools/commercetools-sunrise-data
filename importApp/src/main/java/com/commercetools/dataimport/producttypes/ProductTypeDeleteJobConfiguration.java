package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypeDeleteJobConfiguration extends CommercetoolsJobConfiguration {

}
