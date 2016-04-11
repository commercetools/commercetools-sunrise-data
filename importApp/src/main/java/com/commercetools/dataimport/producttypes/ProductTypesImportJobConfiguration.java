package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypesImportJobConfiguration extends CommercetoolsJobConfiguration {

    @Autowired
    private Resource productTypesArrayResource;

    @Bean
    public Job productTypeCreateJob(final Step createProductTypes) {
        return jobBuilderFactory.get("productTypeCreateJob")
                .start(createProductTypes)
                .build();
    }

    @Bean
    public Step createProductTypes(final ItemReader<ProductTypeDraft> productTypeReader,
                                   final ItemWriter<ProductTypeDraft> productTypeDraftItemWriter) {
        return stepBuilderFactory.get("createProductTypesInCommercetoolsPlatform")
                .<ProductTypeDraft, ProductTypeDraft>chunk(1)
                .reader(productTypeReader)
                .writer(productTypeDraftItemWriter)
                .build();
    }

    @Bean
    public ItemWriter<ProductTypeDraft> productTypeDraftItemWriter() {
        return items -> items.forEach(draft -> sphereClient.executeBlocking(ProductTypeCreateCommand.of(draft)));
    }

    @Bean
    public ItemReader<ProductTypeDraft> productTypeReader() throws IOException {
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<ProductTypeDraft>>() {
        });
        final InputStream inputStream = productTypesArrayResource.getInputStream();
        final List<ProductTypeDraft> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }
}
