package com.commercetools.dataimport.products;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.search.SearchKeywords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Component
@Lazy
public class SuggestKeywordsFromNameJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SuggestKeywordsFromNameJobConfiguration.class);

    @Bean
    public Job productsSuggestionsCopyJob(final Step productsSuggestionsCopyStep) {
        return jobBuilderFactory.get("productsSuggestionsCopyJob")
                .start(productsSuggestionsCopyStep)
                .build();
    }

    @Bean
    public Step productsSuggestionsCopyStep(final BlockingSphereClient sphereClient) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("productsSuggestionsCopyStep");
        return stepBuilder
                .<Product, ProductUpdateCommand>chunk(1)
                .reader(productsReader(sphereClient))
                .processor(productsProcessor())
                .writer(productsWriter(sphereClient))
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        //https://jira.spring.io/browse/BATCH-2269
        final SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor() {
            @Override
            protected void doExecute(Runnable task) {
                //gets the jobExecution of the configuration thread
                final JobExecution jobExecution = JobSynchronizationManager.getContext().getJobExecution();
                super.doExecute(() -> {
                    JobSynchronizationManager.register(jobExecution);
                    try {
                        task.run();
                    } finally {
//                        JobSynchronizationManager.release();
                        JobSynchronizationManager.close();
                    }
                });
            }
        };
        simpleAsyncTaskExecutor.setConcurrencyLimit(20);
        return simpleAsyncTaskExecutor;
    }

    private ItemProcessor<Product, ProductUpdateCommand> productsProcessor() {
        return new ItemProcessor<Product, ProductUpdateCommand>() {
            @Override
            public ProductUpdateCommand process(final Product item) throws Exception {
                return isUseful(item) ? processInternal(item) : null;
            }

            private ProductUpdateCommand processInternal(final Product item) {

                System.err.println("processing " + item.getMasterData().getStaged().getName());

                final SearchKeywords searchKeywords = ProductDraftReader.searchKeywordsFromName(item.getMasterData().getStaged().getName());
                return ProductUpdateCommand.of(item, asList(SetSearchKeywords.of(searchKeywords), Publish.of()));
            }

            private boolean isUseful(final Product item) {
                return item.getMasterData().getStaged().getSearchKeywords().getContent().isEmpty();
            }
        };
    }

    private SynchronizedItemStreamReader<Product> productsReader(final BlockingSphereClient sphereClient) {
        final ItemStreamReader<Product> productItemStreamReader = ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
        final SynchronizedItemStreamReader<Product> objectSynchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        objectSynchronizedItemStreamReader.setDelegate(productItemStreamReader);
        return objectSynchronizedItemStreamReader;
    }

    private ItemWriter<ProductUpdateCommand> productsWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .peek(draft -> logger.info("attempting to update product {}", draft.httpRequestIntent().getPath()))
                .peek(x -> {
                    final Runtime runtime = Runtime.getRuntime();
                    final long memory = runtime.totalMemory() - runtime.freeMemory();
                    System.out.println("Used memory is megabytes: " + (memory / (1024L * 1024L)));
                })
                .map(item -> sphereClient.execute(item))
                .collect(toList())
                .forEach(stage -> blockingWait(stage, 30, TimeUnit.SECONDS));
    }
}
