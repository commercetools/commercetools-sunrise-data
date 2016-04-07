package com.commercetools.dataimport.commercetools;

import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class CommercetoolsJobConfiguration {

    @Autowired
    protected JobBuilderFactory jobBuilderFactory;

    @Autowired
    protected StepBuilderFactory stepBuilderFactory;

    @Autowired
    protected BlockingSphereClient sphereClient;
}
