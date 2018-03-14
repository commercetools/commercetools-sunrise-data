package com.commercetools.dataimport.orders;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.*;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.utils.MoneyImpl;
import org.springframework.batch.item.ItemProcessor;

import javax.money.MonetaryAmount;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class OrderImportItemProcessor implements ItemProcessor<List<OrderCsvEntry>, OrderImportCommand> {

    @Override
    public OrderImportCommand process(final List<OrderCsvEntry> items) {
        if (!items.isEmpty()) {
            final OrderCsvEntry firstCsvLine = items.get(0);
            final MonetaryAmount totalPrice = centAmountToMoney(firstCsvLine.getTotalPrice());
            final OrderState state = OrderState.COMPLETE;
            final List<LineItemImportDraft> lineItemImportDrafts = items.stream()
                    .map(this::lineItemToDraft)
                    .collect(toList());
            final OrderImportDraft draft = OrderImportDraftBuilder.ofLineItems(totalPrice, state, lineItemImportDrafts)
                    .customerEmail(firstCsvLine.getCustomerEmail())
                    .orderNumber(firstCsvLine.getOrderNumber())
                    .build();
            return OrderImportCommand.of(draft);
        }
        return null;
    }

    private LineItemImportDraft lineItemToDraft(final OrderCsvEntry item) {
        final ProductVariantImportDraft draft = ProductVariantImportDraftBuilder.ofSku(item.getLineItems().getVariant().getSku()).build();
        final long quantity = item.getLineItems().getQuantity();
        final Price price = Price.of(centAmountToMoney(item.getLineItems().getPrice()));
        final LocalizedString name = LocalizedString.ofEnglish("Product Name");
        return LineItemImportDraftBuilder.of(draft, quantity, price, name).build();
    }

    private static MonetaryAmount centAmountToMoney(final double centAmount) {
        return MoneyImpl.ofCents((long) (centAmount * 100), "EUR");
    }
}
