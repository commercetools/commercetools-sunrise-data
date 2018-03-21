package com.commercetools.dataimport.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.models.*;
import org.springframework.batch.item.ItemProcessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CategoryImportItemProcessor implements ItemProcessor<CategoryCsvEntry, CategoryCreateCommand> {

    @Override
    public CategoryCreateCommand process(final CategoryCsvEntry item) {
        final LocalizedString name = item.getName().toLocalizedString();
        final LocalizedString slug = item.getSlug().toLocalizedString();
        final CategoryDraft draft = CategoryDraftBuilder.of(name, slug)
                .key(item.getKey())
                .externalId(item.getExternalId())
                .parent(resourceIdentifier(item.getParentKey()))
                .assets(extractAssets(item, name))
                .build();
        return CategoryCreateCommand.of(draft);
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

    @Nullable
    private ResourceIdentifier<Category> resourceIdentifier(@Nullable final String key) {
        return key != null && !key.isEmpty() ? ResourceIdentifier.ofKey(key) : null;
    }
}
