package com.commercetools.demo.dataimport.products;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.products.ProductDraft;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.util.List;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductsImportJobConfiguration {
    static final String b2bCustomerGroupStepContextKey = "b2bCustomerGroupId";
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private Resource productsCsvResource;

    @Bean
    public Job importProducts(final Step getOrCreateCustomerGroup, final Step importStep) {
        return jobBuilderFactory.get("productsImportJob")
                .start(getOrCreateCustomerGroup)
                .next(importStep)
                .build();
    }

    @Bean
    public Step getOrCreateCustomerGroup() {
        final ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{b2bCustomerGroupStepContextKey});
        return stepBuilderFactory.get("getOrCreateCustomerGroup")
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
                        final String customerGroupName = "b2b";
                        final CustomerGroup customerGroup = sphereClient.executeBlocking(CustomerGroupQuery.of().byName(customerGroupName)).head()
                                .orElseGet(() -> sphereClient.executeBlocking(CustomerGroupCreateCommand.of(customerGroupName)));
                        chunkContext.getStepContext().getStepExecution().getExecutionContext().put(b2bCustomerGroupStepContextKey, customerGroup.getId());
                        return RepeatStatus.FINISHED;
                    }
                })
                .listener(listener)
                .build();
    }

    @Bean
    public Step importStep() {
        final StepBuilder stepBuilder = stepBuilderFactory.get("productsImportStep");
        final SimpleStepBuilder<ProductDraft, ProductDraft> chunk = stepBuilder
                .chunk(20);
        return chunk
                .reader(productsReader())
                .writer(productsWriter())
                .build();
    }

    @Bean
    protected ItemReader<ProductDraft> productsReader() {
        return new ProductDraftReader(productsCsvResource);
    }

    @Bean
    protected ItemWriter<ProductDraft> productsWriter() {
        return new ItemWriter<ProductDraft>() {
            @Override
            public void write(final List<? extends ProductDraft> items) throws Exception {
                items.forEach(item -> System.err.println("write " + item));
            }
        };
    }


    @Configuration
    public static class MainConfiguration {//TODO improve
        public MainConfiguration() {
        }

        @Bean Resource productsCsvResource() throws MalformedURLException {
            return new UrlResource("file:///Users/mschleichardt/dev/commercetools-sunrise-data/products/products/151123-sunrise-product-data.csv");
        }

    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class, ProductsImportJobConfiguration.class, MainConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
