package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.commercetools.CommercetoolsPayloadFileConfig;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, CommercetoolsPayloadFileConfig.class, CategoriesDeleteJobConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class CategoriesDeleteJobConfigurationIntegrationTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Before
    public void setUp() throws Exception {
        final LocalizedString name = LocalizedString.ofEnglish("foo");
        sphereClient.executeBlocking(CategoryCreateCommand.of(CategoryDraftBuilder.of(name, name.slugifiedUnique()).build()));
    }

    @Test
    public void jobDeletesCategories() throws Exception {
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        final PagedQueryResult<Category> categoryPagedQueryResult = sphereClient.executeBlocking(CategoryQuery.of().withLimit(0));
        assertThat(categoryPagedQueryResult.getTotal()).isZero();
    }
}