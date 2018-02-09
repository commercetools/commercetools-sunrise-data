package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoriesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job categoriesDeleteJob(final Step rootCategoriesDeleteStep, final Step remainingCategoriesDeleteStep) {
        return jobBuilderFactory.get("categoriesDeleteJob")
                .start(rootCategoriesDeleteStep)
                .next(remainingCategoriesDeleteStep)
                .build();
    }

    @Bean
    public Step rootCategoriesDeleteStep(final ItemReader<Category> rootCategoriesDeleteReader,
                                         final ItemWriter<Category> categoriesDeleteWriter) {
        return stepBuilderFactory.get("rootCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(rootCategoriesDeleteReader)
                .writer(categoriesDeleteWriter)
                .build();
    }

    @Bean
    public Step remainingCategoriesDeleteStep(final ItemReader<Category> remainingCategoriesDeleteReader,
                                              final ItemWriter<Category> categoriesDeleteWriter) {
        return stepBuilderFactory.get("remainingCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(remainingCategoriesDeleteReader)
                .writer(categoriesDeleteWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Category> rootCategoriesDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot());
    }

    @Bean
    public ItemStreamReader<Category> remainingCategoriesDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of());
    }

    @Bean
    public ItemWriter<Category> categoriesDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(CategoryDeleteCommand.of(item)));
    }
}
