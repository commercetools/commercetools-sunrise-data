package com.commercetools.dataimport.inventory;

import com.commercetools.dataimport.CtpResourceRepository;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.models.Reference;
import org.springframework.batch.item.ItemProcessor;

public class InventoryItemProcessor implements ItemProcessor<InventoryCsvEntry, InventoryEntryCreateCommand> {

    private CtpResourceRepository ctpResourceRepository;

    public InventoryItemProcessor(final CtpResourceRepository ctpResourceRepository) {
        this.ctpResourceRepository = ctpResourceRepository;
    }

    @Override
    public InventoryEntryCreateCommand process(final InventoryCsvEntry item) throws Exception {
        final String channelKey = item.getSupplyChannel();
        final Reference<Channel> channelRef = channelKey != null ? ctpResourceRepository.fetchChannelRef(channelKey) : null;
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(item.getSku(), item.getQuantityOnStock())
                .supplyChannel(channelRef)
                .build();
        return InventoryEntryCreateCommand.of(draft);
    }
}
