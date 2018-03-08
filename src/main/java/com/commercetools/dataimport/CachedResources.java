package com.commercetools.dataimport;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

@Configuration
@Slf4j
public class CachedResources {

    private static final String CTP_CACHE = "ctp";

    @Autowired
    private BlockingSphereClient sphereClient;

    @Cacheable(CTP_CACHE)
    @Nullable
    public Reference<Channel> fetchChannelRef(final String key) {
        final ChannelQuery query = ChannelQuery.of().withPredicates(channel -> channel.key().is(key));
        final Channel channel = fetchResource(query);
        return channel != null ? Channel.referenceOfId(channel.getId()) : null;
    }

    @Cacheable(CTP_CACHE)
    @Nullable
    public String fetchCustomerGroupId(final String name) {
        final CustomerGroup customerGroup = fetchResource(CustomerGroupQuery.of().byName(name));
        return customerGroup != null ? customerGroup.getId() : null;
    }

    @Cacheable(CTP_CACHE)
    @Nullable
    public ProductType fetchProductType(final String name) {
        final ProductTypeQuery query = ProductTypeQuery.of().byName(name);
        return fetchResource(query);
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
