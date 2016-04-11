package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.commercetools.CommercetoolsPayloadFileConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypeDeleteJobConfiguration extends CommercetoolsJobConfiguration {
    @Bean
    public Job productTypesDeleteJob(final Step deleteProductTypes) {
        return jobBuilderFactory.get("productTypeDeleteJob")
                .start(deleteProductTypes)
                .build();
    }

    @Bean
    public Step deleteProductTypes(final ItemWriter<ProductType> productTypeDeleteItemWriter) {
        return stepBuilderFactory.get("deleteProductTypesInCommercetoolsPlatform")
                .<ProductType, ProductType>chunk(1)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of()))
                .writer(productTypeDeleteItemWriter)
                .build();
    }

    @Bean
    public ItemWriter<ProductType> productTypeDeleteItemWriter() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item)));
    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsPayloadFileConfig.class, ProductTypeDeleteJobConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
