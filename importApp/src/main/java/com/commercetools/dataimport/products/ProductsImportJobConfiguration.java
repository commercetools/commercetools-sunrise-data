package com.commercetools.dataimport.products;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClientUtils;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
@Lazy
public class ProductsImportJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ProductsImportJobConfiguration.class);
    private int productsImportStepChunkSize = 1;

    @Bean
    public Job productsCreateJob(final Step getOrCreateCustomerGroup,
                              final Step getOrCreateTaxCategoryStep,
                              final Step productsImportStep,
                              final Step publishProductsStep) {
        return jobBuilderFactory.get("productsImportJob")
                .start(getOrCreateCustomerGroup)
                .next(getOrCreateTaxCategoryStep)
                .next(productsImportStep)
                .next(publishProductsStep)
                .build();
    }

    @Bean
    public Step publishProductsStep() {
        return stepBuilderFactory.get("publishProductsStep")
                .<Product, Product>chunk(20)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of()))
                .writer(productPublishWriter())
                .build();
    }

    private ItemWriter<Versioned<Product>> productPublishWriter() {
        return items -> {
            final List<CompletionStage<Product>> completionStages = items.stream()
                    .map(item -> sphereClient.execute(ProductUpdateCommand.of(item, Publish.of())))
                    .collect(toList());
            completionStages.forEach(stage -> SphereClientUtils.blockingWait(stage, 60, TimeUnit.SECONDS));
        };
    }

    @Bean
    public Step getOrCreateTaxCategoryStep() {
        return stepBuilderFactory.get("getOrCreateTaxCategoryStep")
                .tasklet(saveTaxCategoryTasklet())
                .build();
    }

    @Bean
    public Step getOrCreateCustomerGroup() {
        return stepBuilderFactory.get("getOrCreateCustomerGroupStep")
                .tasklet(saveCustomerGroupTasklet())
                .build();
    }

    private Tasklet saveTaxCategoryTasklet() {
        return (contribution, chunkContext) -> {
            final String name = "standard";
            final TaxCategory taxCategory =
                    sphereClient.executeBlocking(TaxCategoryQuery.of().byName(name)).head()
                    .orElseGet(() -> {
                        final TaxCategoryDraft body = TaxCategoryDraft.of(name, asList(
                                TaxRate.of(name, 0.19, true, CountryCode.DE),
                                TaxRate.of(name, 0.08, true, CountryCode.CH),
                                TaxRate.of(name, 0.21, true, CountryCode.CZ),
                                TaxRate.of(name, 0.22, true, CountryCode.IT),
                                TaxRate.of(name, 0.20, true, CountryCode.AU)
                        ));
                        return sphereClient.executeBlocking(TaxCategoryCreateCommand.of(body));
                    });
            return RepeatStatus.FINISHED;
        };
    }

    private Tasklet saveCustomerGroupTasklet() {
        return (contribution, chunkContext) -> {
            final String customerGroupName = "b2b";
            final CustomerGroup customerGroup =
                    sphereClient.executeBlocking(CustomerGroupQuery.of().byName(customerGroupName)).head()
                    .orElseGet(() -> sphereClient.executeBlocking(CustomerGroupCreateCommand.of(customerGroupName)));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }

    @Bean
    public Step productsImportStep(final ItemReader<ProductDraft> productsReader, final TaskExecutor taskExecutor) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("productsImportStep");
        return stepBuilder
                .<ProductDraft, ProductDraft>chunk(productsImportStepChunkSize)
                .reader(productsReader)
                .processor(productsProcessor())
                .writer(productsWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    protected ItemProcessor<ProductDraft, ProductDraft> productsProcessor() {
        return new ItemProcessor<ProductDraft, ProductDraft>() {
            @Override
            public ProductDraft process(final ProductDraft item) throws Exception {
                return isUseful(item) ? item : null;//filter out products without useful name
            }

            private boolean isUseful(final ProductDraft item) {
                return !isEmpty(item.getName().get(Locale.ENGLISH)) && !item.getName().find(Locale.GERMAN).orElse("").startsWith("#max");
            }
        };
    }

    @Bean
    @StepScope
    protected ProductDraftReader productsReader(@Value("#{jobParameters['resource']}") final Resource productsCsvResource,
                                                            @Value("#{jobParameters['maxProducts']}") final Integer maxProducts) {
        return new ProductDraftReader(productsCsvResource, maxProducts != null ? maxProducts : 2, sphereClient);
    }

    @Bean
    protected ItemWriter<ProductDraft> productsWriter() {
        return items -> items.stream()
                //!!!TODO some products are filtered out since the product type is incomplete
                .filter(item -> {
                    final List<AttributeDraft> attributes = item.getMasterVariant().getAttributes();
                    return !attributes.stream().anyMatch(a -> a.getName().equals("designer") && a.getValue().equals(new TextNode("juliat")));
                })
                .peek(draft -> logger.info("attempting to create product {}", draft.getSlug()))
                .peek(x -> {
                    final Runtime runtime = Runtime.getRuntime();
                    final long memory = runtime.totalMemory() - runtime.freeMemory();
                    System.out.println("Used memory is megabytes: " + (memory / (1024L * 1024L)));
                })
                .map(item -> sphereClient.execute(ProductCreateCommand.of(item)))
                .collect(toList())
                .forEach(stage -> blockingWait(stage, 30, TimeUnit.SECONDS));
    }
}
