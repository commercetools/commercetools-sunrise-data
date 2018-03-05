package com.commercetools.dataimport;

import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = App.class)
@ActiveProfiles({"ci", "integration"})
public class ImportJobIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    @Qualifier("dataImport")
    private Job channelsImport;

    @Autowired
    @Qualifier("ordersImport")
    private Job ordersImport;

    @Test
    public void importsChannels() throws Exception {
        testJob(channelsImport, jobExecution -> {
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
            assertThatChannelsAreImported();
            assertThatOrderTypeIsImported();
            assertThatCustomerTypeIsImported();
        });
    }

    @Ignore("Missing product import")
    @Test
    public void importsOrders() throws Exception {
        testJob(ordersImport, jobExecution -> {
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
            assertThatOrdersAreImported();
        });
    }

    private void assertThatChannelsAreImported() {
        final ChannelQuery query = ChannelQuery.of().withLimit(0);
        final Long totalChannels = sphereClient.executeBlocking(query).getTotal();
        assertThat(totalChannels)
                .as("Channels are correctly imported")
                .isGreaterThan(0);
    }

    private void assertThatOrderTypeIsImported() {
        final Long totalChannels = queryTotalTypes("order");
        assertThat(totalChannels)
                .as("Order type is correctly imported")
                .isGreaterThan(0);
    }

    private void assertThatCustomerTypeIsImported() {
        final Long totalChannels = queryTotalTypes("customer");
        assertThat(totalChannels)
                .as("Customer type is correctly imported")
                .isGreaterThan(0);
    }

    private Long queryTotalTypes(final String resourceTypeId) {
        final TypeQuery query = TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList(resourceTypeId)))
                .withLimit(0);
        return sphereClient.executeBlocking(query).getTotal();
    }

    private void assertThatOrdersAreImported() {
        final Long totalOrders = sphereClient.executeBlocking(OrderQuery.of().withLimit(0)).getTotal();
        assertThat(totalOrders)
                .as("Orders are correctly imported")
                .isGreaterThan(0);
    }

    private void testJob(final Job job, final Consumer<JobExecution> jobExecutionConsumer) throws Exception {
        final JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(job);
        jobExecutionConsumer.accept(jobLauncherTestUtils.launchJob());
    }
}
