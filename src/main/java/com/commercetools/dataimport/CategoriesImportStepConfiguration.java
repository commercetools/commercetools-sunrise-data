package com.commercetools.dataimport;

import com.commercetools.dataimport.categories.CategoryCsvEntry;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Configuration
@Slf4j
public class CategoriesImportStepConfiguration {

    private static final String[] CATEGORY_CSV_HEADER_NAMES = new String[]{"externalId", "name.de", "slug.de", "name.en", "slug.en", "name.it", "slug.it", "parentId", "webImageUrl", "iosImageUrl"};

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.categories}")
    private Resource categoriesResource;

    @Bean
    @JobScope
    public Step rootCategoriesDeleteStep() {
        return stepBuilderFactory.get("rootCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(rootCategoriesDeleteStepReader())
                .writer(categoriesDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step remainingCategoriesDeleteStep() {
        return stepBuilderFactory.get("remainingCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(remainingCategoriesDeleteStepReader())
                .writer(categoriesDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step categoriesImportStep() {
        return stepBuilderFactory.get("categoriesImportStep")
                .<CategoryCsvEntry, CategoryDraft>chunk(1)
                .reader(categoriesImportStepReader())
                .processor(categoriesImportStepProcessor())
                .writer(categoriesImportStepWriter())
                .build();
    }

    private ItemReader<Category> rootCategoriesDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot());
    }

    private ItemReader<Category> remainingCategoriesDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of());
    }

    private ItemWriter<Category> categoriesDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final Category category = sphereClient.executeBlocking(CategoryDeleteCommand.of(item));
            log.debug("Removed category \"{}\"", category.getExternalId());
        });
    }

    private ItemReader<CategoryCsvEntry> categoriesImportStepReader() {
        final FlatFileItemReader<CategoryCsvEntry> reader = new FlatFileItemReader<>();
        reader.setResource(categoriesResource);
        reader.setLineMapper(new DefaultLineMapper<CategoryCsvEntry>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(CATEGORY_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<CategoryCsvEntry>() {{
                setTargetType(CategoryCsvEntry.class);
            }});
        }});
        reader.setLinesToSkip(1);
        return reader;
    }

    private ItemWriter<CategoryDraft> categoriesImportStepWriter() {
        return items -> items.forEach(draft -> {
            final Category category = sphereClient.executeBlocking(CategoryCreateCommand.of(draft));
            log.debug("Created category \"{}\"", category.getExternalId());
        });
    }

    private ItemProcessor<CategoryCsvEntry, CategoryDraft> categoriesImportStepProcessor() {
        return item -> {
            final LocalizedString name = item.getName().toLocalizedString();
            final LocalizedString slug = item.getSlug().toLocalizedString();
            final String externalId = item.getExternalId();
            final ResourceIdentifier<Category> parent = fetchParent(item);
            final List<AssetDraft> assets = extractAssets(item, name);
            return CategoryDraftBuilder.of(name, slug)
                    .externalId(externalId)
                    .parent(parent)
                    .assets(assets)
                    .build();
        };
    }

    private List<AssetDraft> extractAssets(final CategoryCsvEntry item, final LocalizedString name) {
        final List<AssetDraft> assets = new ArrayList<>();
        if (!isEmpty(item.getWebImageUrl())) {
            final AssetSource assetSource = AssetSourceBuilder.ofUri(item.getWebImageUrl())
                    .key("web")
                    .build();
            final AssetDraft assetDraft = AssetDraftBuilder.of(singletonList(assetSource), name)
                    .tags("web")
                    .build();
            assets.add(assetDraft);
        }
        if (!isEmpty(item.getIosImageUrl())) {
            final AssetSource assetSource = AssetSourceBuilder.ofUri(item.getIosImageUrl())
                    .key("ios")
                    .build();
            final AssetDraft assetDraft = AssetDraftBuilder.of(singletonList(assetSource), name)
                    .tags("ios")
                    .build();
            assets.add(assetDraft);
        }
        return assets;
    }

    private ResourceIdentifier<Category> fetchParent(final CategoryCsvEntry categoryCsvEntry) {
        final String parentId = categoryCsvEntry.getParentId();
        return parentId == null ? null : sphereClient.executeBlocking(CategoryQuery.of().byExternalId(parentId)).head()
                .map(Referenceable::toResourceIdentifier)
                .orElse(null);
    }
}
