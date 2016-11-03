package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;

public class CustomItemProcessorListener implements ItemProcessListener<ProductProjection, ProductUpdateCommand> {

    private static final Logger logger = LoggerFactory.getLogger(CustomItemProcessorListener.class);

    @Override
    public void beforeProcess(final ProductProjection productProjection) {
        System.err.println("BEFORE PROCESS");
    }

    @Override
    public void afterProcess(final ProductProjection productProjection, final ProductUpdateCommand productUpdateCommand) {
        System.err.println("AFTER PROCESS");
    }

    @Override
    public void onProcessError(final ProductProjection productProjection, final Exception e) {
        System.err.println("PROCESS ERROR");
    }
}
