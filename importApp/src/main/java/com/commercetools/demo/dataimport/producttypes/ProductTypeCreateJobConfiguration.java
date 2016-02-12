package com.commercetools.demo.dataimport.producttypes;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypeCreateJobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private Resource attributeDefinitionsCsvResource;

    @Bean
    public Job productTypeCreateJob(final Step readAttributeDefinitions) {
        return jobBuilderFactory.get("productTypeCreateJob")
                .start(readAttributeDefinitions)
                .build();
    }

    @Bean
    public Step readAttributeDefinitions(final ItemReader<AttributeDefinitionCsvEntry> attributeDefinitionCsvEntryReader,
                                         final ItemWriter<AttributeDefinition> attributeDefinitionWriter,
                                         ItemProcessor<AttributeDefinitionCsvEntry, AttributeDefinition> attributeDefinitionProcessor) {
        return stepBuilderFactory.get("readProductTypeAttributesFromCsv")
                .<AttributeDefinitionCsvEntry, AttributeDefinition>chunk(1)
                .reader(attributeDefinitionCsvEntryReader)
                .processor(attributeDefinitionProcessor)
                .writer(attributeDefinitionWriter)
                .build();
    }

    @Bean
    public ItemReader<AttributeDefinitionCsvEntry> attributeDefinitionCsvEntryReader() {
        return new AttributeDefinitionReader(attributeDefinitionsCsvResource);
    }

    @Bean
    public ItemWriter<AttributeDefinition> attributeDefinitionWriter() {
        return new ItemWriter<AttributeDefinition>() {
            @Override
            public void write(final List<? extends AttributeDefinition> items) throws Exception {
                for (final AttributeDefinition item : items) {
                    System.err.println(item);
                }
            }
        };
    }

    @Bean
    public ItemProcessor<AttributeDefinitionCsvEntry, AttributeDefinition> attributeDefinitionProcessor() {
        return new ItemProcessor<AttributeDefinitionCsvEntry, AttributeDefinition>() {
            private final Map<String, Function<AttributeDefinitionCsvEntry, AttributeType>> typNameToCreatorMap = new HashMap<>();
            {
                typNameToCreatorMap.put("datetime", e -> DateTimeAttributeType.of());
                typNameToCreatorMap.put("text", e -> StringAttributeType.of());
                typNameToCreatorMap.put("ltext", e -> LocalizedStringAttributeType.of());
                typNameToCreatorMap.put("enum", e -> {

                    //TODO multi line processing?
                    final List<EnumValue> values = Collections.emptyList();
                    return EnumAttributeType.of(values);
                });
                typNameToCreatorMap.put("lenum", e -> {

                    //TODO multi line processing?
                    final List<LocalizedEnumValue> values = Collections.emptyList();
                    return LocalizedEnumAttributeType.of(values);
                });
                typNameToCreatorMap.put("set:ltext", e -> {

                    //TODO multi line processing?
                    final List<LocalizedEnumValue> values = Collections.emptyList();
                    return SetAttributeType.of(LocalizedEnumAttributeType.of(values));
                });
                typNameToCreatorMap.put("set:text", e -> SetAttributeType.of(StringAttributeType.of()));
                typNameToCreatorMap.put("boolean", e -> BooleanAttributeType.of());
            }

            @Override
            public AttributeDefinition process(final AttributeDefinitionCsvEntry item) throws Exception {
                final String name = item.getName();
                final LocalizedString label = item.getLabel().toLocalizedString();
                final String attributeTypeName = item.getType();
                final Function<? super String, ? extends Function<AttributeDefinitionCsvEntry, AttributeType>> mappingFunction = key -> {throw new RuntimeException("unknown type '" + attributeTypeName + "'");};
                final AttributeType attributeType = typNameToCreatorMap.computeIfAbsent(attributeTypeName, mappingFunction).apply(item);
                final AttributeDefinitionBuilder attributeDefinitionBuilder = AttributeDefinitionBuilder.of(name, label, attributeType)
                        .attributeConstraint(AttributeConstraint.ofSphereValue(item.getAttributeConstraint()))
                        .isRequired("true".equals(item.getIsRequired()))
                        .isSearchable("true".equals(item.getIsSearchable()));
                final String textInputHint = item.getTextInputHint();
                if (!StringUtils.isEmpty(textInputHint)) {
                    attributeDefinitionBuilder.inputHint(TextInputHint.ofSphereValue(textInputHint));
                }
                return attributeDefinitionBuilder.build();
            }
        };
    }

    @Configuration
    public static class MainConfiguration {//TODO improve
        public MainConfiguration() {
        }

        @Bean Resource attributeDefinitionsCsvResource() throws MalformedURLException {
            return new UrlResource("file:///Users/mschleichardt/dev/commercetools-sunrise-data/products/product%20type/sunrise-producttype-attributes.csv");
        }

    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class, ProductTypeCreateJobConfiguration.class, MainConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
