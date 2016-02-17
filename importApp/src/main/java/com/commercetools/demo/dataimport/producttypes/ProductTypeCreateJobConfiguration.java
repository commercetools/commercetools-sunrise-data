package com.commercetools.demo.dataimport.producttypes;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.*;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypeCreateJobConfiguration {
    public static final String ATTRIBUTE_DEFINITIONS_LIST_KEY = "attributeDefinitionsList";
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private Resource attributeDefinitionsCsvResource;

    @Autowired
    private Resource productTypesCsvResource;

    @Bean
    public Job productTypeCreateJob(final Step readAttributeDefinitions,
                                    final Step createProductTypes) {
        return jobBuilderFactory.get("productTypeCreateJob")
                .start(readAttributeDefinitions)
                .next(createProductTypes)
                .build();
    }

    @Bean
    public Step createProductTypes(final ItemReader<ProductTypeCsvEntry> productTypeCsvEntryReader,
                                   final ItemProcessor<ProductTypeCsvEntry, ProductTypeDraft> productTypeProcessor,
                                   final ItemWriter<ProductTypeDraft> productTypeDraftItemWriter) {
        return stepBuilderFactory.get("createProductTypesInCommercetoolsPlatform")
                .<ProductTypeCsvEntry, ProductTypeDraft>chunk(1)
                .reader(productTypeCsvEntryReader)
                .processor(productTypeProcessor)
                .writer(productTypeDraftItemWriter)
                .build();
    }

    @Bean
    public ItemProcessor<ProductTypeCsvEntry, ProductTypeDraft> productTypeProcessor() {
        return new ItemProcessor<ProductTypeCsvEntry, ProductTypeDraft>() {
            List<AttributeDefinition> attributeDefinitions;

            @Override
            public ProductTypeDraft process(final ProductTypeCsvEntry item) throws Exception {
                return item.getName() == null ? null : processItem(item);
            }

            private ProductTypeDraft processItem(final ProductTypeCsvEntry item) {
                final String name = item.getName();
                final String description = item.getDescription();
                final List<AttributeDefinition> attributeDefinitions = this.attributeDefinitions.stream()
                        .filter(definition -> item.getAttributeNames().contains(definition.getName()))
                        .collect(toList());
                return ProductTypeDraft.of(name, name, description, attributeDefinitions);
            }

            @BeforeStep
            public void retrieveInterstepData(final StepExecution stepExecution) {
                JobExecution jobExecution = stepExecution.getJobExecution();
                ExecutionContext jobContext = jobExecution.getExecutionContext();
                this.attributeDefinitions = (List<AttributeDefinition>) jobContext.get(ATTRIBUTE_DEFINITIONS_LIST_KEY);
            }
        };
    }

    @Bean
    public ItemWriter<ProductTypeDraft> productTypeDraftItemWriter() {
        return items -> items.forEach(draft -> sphereClient.executeBlocking(ProductTypeCreateCommand.of(draft)));
    }

    @Bean
    public ItemReader<ProductTypeCsvEntry> productTypeCsvEntryReader() {
        final DefaultLineMapper<ProductTypeCsvEntry> fullLineMapper = new DefaultLineMapper<ProductTypeCsvEntry>() {{
            final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
            setLineTokenizer(tokenizer);
            setFieldSetMapper(new FieldSetMapper<ProductTypeCsvEntry>() {
                FieldSet headers = null;
                @Override
                public ProductTypeCsvEntry mapFieldSet(final FieldSet fieldSet) throws BindException {
                    final boolean first = headers == null;
                    if (first) {
                        headers = fieldSet;
//                        tokenizer.setNames(fieldSet.getValues());
                        return new ProductTypeCsvEntry();//dummy to filter out
                    } else {
                        final ProductTypeCsvEntry entry = new ProductTypeCsvEntry();
                        for (int column = 0; column < headers.getValues().length; column++) {
                            final String columnName = headers.getValues()[column];
                            final String value = fieldSet.getValues()[column];
                            if ("name".equals(columnName)) {
                                entry.setName(value);
                            } else if ("description".equals(columnName)) {
                                entry.setDescription(value);
                            } else {
                                entry.addAttribute(columnName);//the value would be an 'x'
                            }
                        }
                        return entry;
                    }
                }
            });
        }};
        FlatFileItemReader<ProductTypeCsvEntry> reader = new FlatFileItemReader<>();
        reader.setResource(this.productTypesCsvResource);
        reader.setLineMapper(fullLineMapper);
        return reader;
    }

    @Bean
    public Step readAttributeDefinitions(final ItemReader<AttributeDefinitionCsvEntry> attributeDefinitionCsvEntryReader,
                                         final ItemWriter<AttributeDefinition> attributeDefinitionWriter,
                                         ItemProcessor<AttributeDefinitionCsvEntry, AttributeDefinition> attributeDefinitionProcessor) {
        final ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{ATTRIBUTE_DEFINITIONS_LIST_KEY});
        return stepBuilderFactory.get("readProductTypeAttributesFromCsv")
                .<AttributeDefinitionCsvEntry, AttributeDefinition>chunk(1)
                .reader(attributeDefinitionCsvEntryReader)
                .processor(attributeDefinitionProcessor)
                .writer(attributeDefinitionWriter)
                .listener(listener)
                .build();
    }

    @Bean
    public ItemReader<AttributeDefinitionCsvEntry> attributeDefinitionCsvEntryReader() {
        return new AttributeDefinitionReader(attributeDefinitionsCsvResource);
    }

    @Bean
    public ItemWriter<AttributeDefinition> attributeDefinitionWriter() {
        return new ItemWriter<AttributeDefinition>() {
            public StepExecution stepExecution;

            @Override
            public void write(final List<? extends AttributeDefinition> items) throws Exception {
                final ExecutionContext stepContext = this.stepExecution.getExecutionContext();
                if (!stepContext.containsKey(ATTRIBUTE_DEFINITIONS_LIST_KEY)) {
                    stepContext.put(ATTRIBUTE_DEFINITIONS_LIST_KEY, new LinkedList<>());
                }
                final List<AttributeDefinition> memory = (List<AttributeDefinition>) stepContext.get(ATTRIBUTE_DEFINITIONS_LIST_KEY);
                for (final AttributeDefinition item : items) {
                    memory.add(item);
                }
            }

            @BeforeStep
            public void saveStepExecution(final StepExecution stepExecution) {
                this.stepExecution = stepExecution;
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

        @Bean Resource productTypesCsvResource() throws MalformedURLException {
            return new UrlResource("file:///Users/mschleichardt/dev/commercetools-sunrise-data/products/product%20type/sunrise-producttypes.csv");
        }

    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class, ProductTypeCreateJobConfiguration.class, MainConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
