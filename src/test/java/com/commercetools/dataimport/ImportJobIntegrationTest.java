package com.commercetools.dataimport;

import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.queries.OrderQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@TestPropertySource(value = "classpath:test.properties")
public class ImportJobIntegrationTest {

//    @Autowired
//    private JobLauncherTestUtils jobLauncherTestUtils;
//
//    @Autowired
////    @Qualifier("test")
//    private BlockingSphereClient sphereClient;
//
//    @Test
//    public void importsPayloads() throws Exception {
//        jobLauncherTestUtils.
//        App.run(, "payload.json");
//        assertThatChannelsAreImported();
//
//        App.main(new String[]{"payload-orders.json"});
//        assertThatOrdersAreImported();
//    }
//
//    private void assertThatChannelsAreImported() {
//        final Long totalChannels = sphereClient.executeBlocking(ChannelQuery.of().withLimit(0)).getTotal();
//        assertThat(totalChannels)
//                .as("Channels are correctly imported")
//                .isGreaterThan(0);
//    }
//
//    private void assertThatOrdersAreImported() {
//        final Long totalOrders = sphereClient.executeBlocking(OrderQuery.of().withLimit(0)).getTotal();
//        assertThat(totalOrders)
//                .as("Orders are correctly imported")
//                .isGreaterThan(0);
//    }
}
