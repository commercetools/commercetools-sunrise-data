package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.Command;
import io.sphere.sdk.models.Versioned;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
public class CtpResourceItemWriter<C extends Command<T>, T extends Versioned<T>> implements ItemWriter<C> {

    private final BlockingSphereClient sphereClient;

    public CtpResourceItemWriter(final BlockingSphereClient sphereClient) {
        this.sphereClient = sphereClient;
    }

    @Override
    public void write(final List<? extends C> items) {
        items.forEach(item -> {
            if (item != null) {
                final T resource = sphereClient.executeBlocking(item);
                if (log.isDebugEnabled()) {
                    log.debug("Executed {} on \"{}\"", item.getClass().getSuperclass().getSimpleName(), resource.getId());
                }
            }
        });
    }
}
