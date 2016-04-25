package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.Versioned;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.client.SphereClientUtils.blockingWaitForEachCollector;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class CategoriesDeleteJobConfiguration extends DefaultCommercetoolsJobConfiguration {

    @Bean
    public Job categoriesDeleteJob(Step deleteRootCategories, Step deleteRemainingCategories) {
        return jobBuilderFactory.get("categoriesDeleteJob")
                .start(deleteRootCategories)
                .next(deleteRemainingCategories)
                .build();
    }

    @Bean
    public Step deleteRootCategories(final BlockingSphereClient sphereClient,
                                        final ItemWriter<Versioned<Category>> categoryDeleteWriter) {
        return stepBuilderFactory.get("deleteRootCategoriesStep")
                .<Category, Category>chunk(1)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot()))
                .writer(categoryDeleteWriter)
                .build();
    }

    @Bean
    public Step deleteRemainingCategories(final BlockingSphereClient sphereClient, final ItemWriter<Versioned<Category>> categoryDeleteWriter) {
        return stepBuilderFactory.get("deleteRemainingCategoriesStep")
                .<Category, Category>chunk(1)//should always be 1 for commerctools platform
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of()))
                .writer(categoryDeleteWriter)
                .build();
    }

    @Bean
    public ItemWriter<Versioned<Category>> categoryDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .map(item -> sphereClient.execute(CategoryDeleteCommand.of(item)))
                .collect(blockingWaitForEachCollector(30, TimeUnit.SECONDS));
    }
}
