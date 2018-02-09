package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class CategoriesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job categoriesDeleteJob(Step deleteRootCategories, Step deleteRemainingCategories) {
        return jobBuilderFactory.get("categoriesDeleteJob")
                .start(deleteRootCategories)
                .next(deleteRemainingCategories)
                .build();
    }

    @Bean
    public Step deleteRootCategories(final ItemReader<Category> rootCategoryReader, final ItemWriter<Category> categoryWriter) {
        return stepBuilderFactory.get("deleteRootCategoriesStep")
                .<Category, Category>chunk(1)
                .reader(rootCategoryReader)
                .writer(categoryWriter)
                .build();
    }

    @Bean
    public Step deleteRemainingCategories(final ItemReader<Category> remainingCategoryReader, final ItemWriter<Category> categoryWriter) {
        return stepBuilderFactory.get("deleteRemainingCategoriesStep")
                .<Category, Category>chunk(1)
                .reader(remainingCategoryReader)
                .writer(categoryWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Category> rootCategoryReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot());
    }

    @Bean
    public ItemStreamReader<Category> remainingCategoryReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of());
    }

    @Bean
    public ItemWriter<Category> categoryWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(CategoryDeleteCommand.of(item)));
    }
}
