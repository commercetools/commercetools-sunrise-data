package com.commercetools.demo.dataimport.categories;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.projects.Project;
import io.sphere.sdk.projects.queries.ProjectGet;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, CommercetoolsConfig.class, CategoriesImportJobConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class CategoriesImportJobConfigurationTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    Resource categoryCsvResource() {
        return new FileSystemResource("../categories/sunrise-categories.csv");
    }

    @Before
    public void setUp() throws Exception {
        final Project project = sphereClient.executeBlocking(ProjectGet.of());
        assertThat(project.getLanguages())
                .as("languages need to be enabled in the test project https://admin.sphere.io/" + project.getKey() + "/settings")
                .contains("en", "de", "it");
        sphereClient.executeBlocking(CategoryQuery.of().byIsRoot())
        .getResults()
        .forEach(cat -> sphereClient.executeBlocking(CategoryDeleteCommand.of(cat)));
        assertThat(sphereClient.executeBlocking(CategoryQuery.of().withLimit(0)).getTotal())
                .as("to test the import no categories should exist in the project " + project.getKey())
                .isEqualTo(0);
    }

    @Test
    public void jobCreatesCategories() throws Exception {
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        final PagedQueryResult<Category> categoryPagedQueryResult = sphereClient.executeBlocking(CategoryQuery.of().withLimit(0));
        assertThat(categoryPagedQueryResult.getTotal()).isEqualTo(131);
    }
}