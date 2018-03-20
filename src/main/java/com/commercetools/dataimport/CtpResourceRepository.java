package com.commercetools.dataimport;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.models.Identifiable;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.Query;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;

@Component
@Slf4j
public class CtpResourceRepository {

    @Autowired
    private BlockingSphereClient sphereClient;

    @Cacheable("channels")
    @Nullable
    public Reference<Channel> fetchChannelRef(final String key) {
        final ChannelQuery query = ChannelQuery.of().withPredicates(channel -> channel.key().is(key));
        final Channel channel = fetchResource(query);
        return channel != null ? Channel.referenceOfId(channel.getId()) : null;
    }

    @Cacheable("customerGroups")
    @Nullable
    public String fetchCustomerGroupId(final String name) {
        final CustomerGroup customerGroup = fetchResource(CustomerGroupQuery.of().byName(name));
        return customerGroup != null ? customerGroup.getId() : null;
    }

    @Cacheable("taxCategories")
    @Nullable
    public String fetchTaxCategoryId(final String name) {
        final TaxCategory taxCategory = fetchResource(TaxCategoryQuery.of().byName(name));
        return taxCategory != null ? taxCategory.getId() : null;
    }

    @Cacheable("productTypes")
    @Nullable
    public ProductType fetchProductType(final String name) {
        final ProductTypeQuery query = ProductTypeQuery.of().byName(name);
        return fetchResource(query);
    }

    public CategoryTree fetchCategoryTree() {
        final List<Category> categories = blockingWait(QueryExecutionUtils.queryAll(sphereClient, CategoryQuery.of()), Duration.ofSeconds(30));
        log.debug("Fetched category tree with {} categories", categories.size());
        return CategoryTree.of(categories);
    }

    @Nullable
    private <T extends Identifiable<T>> T fetchResource(final Query<T> request) {
        return sphereClient.executeBlocking(request)
                .head()
                .map(resource -> {
                    log.debug("Fetched \"{}\" \"{}\"", resource.getClass().getSimpleName(), resource.getId());
                    return resource;
                })
                .orElse(null);
    }
}
