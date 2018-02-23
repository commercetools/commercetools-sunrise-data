package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@ContextConfiguration(classes = SphereClientTestConfiguration.class)
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class ImportJobIntegrationTest {

    @Autowired
    @Qualifier("test")
    protected BlockingSphereClient sphereClient;

    @Test
    public void name() throws Exception {
        App.main(new String[]{"payload.json"});
    }
}
