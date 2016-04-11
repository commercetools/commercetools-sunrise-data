package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Referenceable;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class CategoriesImportJobConfiguration extends CommercetoolsJobConfiguration {

    public static final String[] CATEGORY_CSV_HEADER_NAMES = new String[]{"externalId", "name.de", "slug.de", "name.en", "slug.en", "name.it", "slug.it", "parentId"};
    @Autowired
    private Resource categoryCsvResource;

    @Bean
    public Job importCategories(final Step categoriesImportStep) {
        return jobBuilderFactory.get("categoriesImportJob")
                .start(categoriesImportStep)
                .build();
    }

    @Bean
    public Step categoriesImportStep() {
        final StepBuilder stepBuilder = stepBuilderFactory.get("categoriesImportStep");
        return stepBuilder
                .<CategoryCsvLineValue, CategoryDraft>chunk(1)
                .reader(categoryReader())
                .processor(categoryProcessor())
                .writer(categoryWriter())
                .build();
    }

    @Bean
    protected ItemWriter<CategoryDraft> categoryWriter() {
        return items -> items.forEach(draft -> sphereClient.executeBlocking(CategoryCreateCommand.of(draft)));
    }

    @Bean
    protected ItemProcessor<CategoryCsvLineValue, CategoryDraft> categoryProcessor() {
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
    protected ItemReader<CategoryCsvLineValue> categoryReader() {
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
