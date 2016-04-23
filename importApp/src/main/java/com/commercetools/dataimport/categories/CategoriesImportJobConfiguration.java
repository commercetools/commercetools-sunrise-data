package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Referenceable;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class CategoriesImportJobConfiguration extends DefaultCommercetoolsJobConfiguration {

    public static final String[] CATEGORY_CSV_HEADER_NAMES = new String[]{"externalId", "name.de", "slug.de", "name.en", "slug.en", "name.it", "slug.it", "parentId"};

    @Bean
    public Job categoriesCreateJob(final Step categoriesImportStep) {
        return jobBuilderFactory.get("categoriesImportJob")
                .start(categoriesImportStep)
                .build();
    }

    @Bean
    public Step categoriesImportStep(final ItemReader<CategoryCsvLineValue> categoryReader,
                                     final ItemWriter<CategoryDraft> categoryWriter,
                                     final ItemProcessor<CategoryCsvLineValue, CategoryDraft> categoryProcessor) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("categoriesImportStep");
        return stepBuilder
                .<CategoryCsvLineValue, CategoryDraft>chunk(1)
                .reader(categoryReader)
                .processor(categoryProcessor)
                .writer(categoryWriter)
                .build();
    }

    @Bean
    protected ItemWriter<CategoryDraft> categoryWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(draft -> sphereClient.executeBlocking(CategoryCreateCommand.of(draft)));
    }

    @Bean
    protected ItemProcessor<CategoryCsvLineValue, CategoryDraft> categoryProcessor(final BlockingSphereClient sphereClient) {
        return item -> {
            final LocalizedString name = item.getName().toLocalizedString();
            final LocalizedString slug = item.getSlug().toLocalizedString();
            final String externalId = item.getExternalId();
            final String externalParentId = item.getParentId();
            final Referenceable<Category> parent = externalParentId == null
                    ? null
                    : sphereClient.executeBlocking(CategoryQuery.of().byExternalId(externalParentId)).head().orElse(null);
            return CategoryDraftBuilder.of(name, slug).externalId(externalId).parent(parent).build();
        };
    }

    @Bean
    @StepScope
    protected FlatFileItemReader<CategoryCsvLineValue> categoryReader(@Value("#{jobParameters['resource']}") final Resource categoryCsvResource) {
        FlatFileItemReader<CategoryCsvLineValue> reader = new FlatFileItemReader<>();
        reader.setResource(categoryCsvResource);
        reader.setLineMapper(new DefaultLineMapper<CategoryCsvLineValue>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(CATEGORY_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<CategoryCsvLineValue>() {{
                setTargetType(CategoryCsvLineValue.class);

            }});

        }});
        reader.setLinesToSkip(1);
        return reader;
    }
}
