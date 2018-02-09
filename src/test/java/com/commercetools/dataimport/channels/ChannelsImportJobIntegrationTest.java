package com.commercetools.dataimport.channels;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.IntegrationTest;
import com.commercetools.dataimport.TestConfiguration;
import com.commercetools.dataimport.channels.ChannelsImportJobConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.dataimport.JsonUtils.createJsonList;
import static com.commercetools.dataimport.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, ChannelsImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
@NotThreadSafe
public class ChannelsImportJobIntegrationTest extends IntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext appContext;

    @Test
    public void channelImportStepTest() throws Exception {
        final Map<String, JobParameter> jobParametersMap = new HashMap<>();
        final String channelsResourcePath = "file://" + new File(".", "data/channels/channels.json").getAbsolutePath();
        final String typesResourcePath = "file://" + new File(".", "data/channels/channel-types.json").getAbsolutePath();
        jobParametersMap.put("typesResource", new JobParameter(typesResourcePath));
        jobParametersMap.put("channelsResource", new JobParameter(channelsResourcePath));
        addCommercetoolsCredentialValues(env, jobParametersMap);
        final JobParameters jobParameters = new JobParameters(jobParametersMap);
        final ExitStatus status = jobLauncherTestUtils.launchJob(jobParameters).getExitStatus();
        assertThat(status).isEqualTo(ExitStatus.COMPLETED);
        checkChannels(channelsResourcePath);
        checkTypes(typesResourcePath);
    }

    private void checkChannels(final String channelsResourcePath) throws IOException {
        Resource resource = appContext.getResource(channelsResourcePath);
        final List<Channel> channelsFromJsonFile = createJsonList(resource, Channel.class);
        final ChannelQuery channelQuery = ChannelQuery.of();
        final List<Channel> channelList = sphereClient.executeBlocking(channelQuery).getResults();
        assertThat(channelList).hasSize(channelsFromJsonFile.size());
        deleteChannels(sphereClient);
    }

    private void checkTypes(final String typesResourcePath) throws IOException {
        Resource resource = appContext.getResource(typesResourcePath);
        final List<Type> typesFromJsonFile = createJsonList(resource, Type.class);
        final TypeQuery typeQuery = TypeQuery.of();
        final List<Type> typeList = sphereClient.executeBlocking(typeQuery).getResults();
        assertThat(typeList).hasSize(typesFromJsonFile.size());
        deleteTypes(sphereClient);
    }
}
