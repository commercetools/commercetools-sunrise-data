package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, ChannelsImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
@NotThreadSafe
public class ChannelsImportJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Environment env;

    @Test
    public void channelImportStepTest() throws Exception {
        final Map<String, JobParameter> jobParametersMap = new HashMap<>();
        jobParametersMap.put("typesResource", new JobParameter("file://" + new File(".", "../joyride/types.json").getAbsolutePath()));
        jobParametersMap.put("channelsResource", new JobParameter("file://" + new File(".", "../joyride/channels.json").getAbsolutePath()));
        addCommercetoolsCredentialValues(env, jobParametersMap);
        final JobParameters jobParameters = new JobParameters(jobParametersMap);
        final ExitStatus status = jobLauncherTestUtils.launchJob(jobParameters).getExitStatus();
        assertThat(status).isEqualTo(ExitStatus.COMPLETED);
        checkChannels();
        checkTypes();
    }

    private void checkChannels() {
        final ChannelQuery channelQuery = ChannelQuery.of();
        final PagedQueryResult<Channel> channelPagedQueryResult = sphereClient.executeBlocking(channelQuery);
        assertThat(channelPagedQueryResult.getTotal()).isEqualTo(16);
        deleteChannels(sphereClient);
    }

    private void checkTypes() {
        final TypeQuery typeQuery = TypeQuery.of().withSort(m -> m.key().sort().asc());
        final PagedQueryResult<Type> typePagedQueryResult = sphereClient.executeBlocking(typeQuery);
        assertThat(typePagedQueryResult.getTotal()).isEqualTo(2);
        final Type type1 = typePagedQueryResult.getResults().get(0);
        assertThat(type1.getKey()).isEqualTo("physicalStore");
        final Type type2 = typePagedQueryResult.getResults().get(1);
        assertThat(type2.getKey()).isEqualTo("reservationOrder");
        deleteTypes(sphereClient);
    }
}
