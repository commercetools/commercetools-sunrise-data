package com.commercetools.demo.dataimport.categories;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.projects.Project;
import io.sphere.sdk.projects.queries.ProjectGet;
import io.sphere.sdk.queries.PagedQueryResult;
import org.assertj.core.api.SoftAssertions;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
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
        return new FileSystemResource("../categories/categories.csv");
    }

    @Before
    public void setUp() throws Exception {
        final Project project = sphereClient.executeBlocking(ProjectGet.of());
        assertThat(project.getLanguages())
                .as("languages need to be enabled in the test project https://admin.sphere.io/" + project.getKey() + "/settings")
                .contains("en", "de", "it");
        final CategoryQuery categoryQuery = CategoryQuery.of().byIsRoot().withLimit(500);
        sphereClient.executeBlocking(categoryQuery)
        .getResults()
        .forEach(cat -> sphereClient.executeBlocking(CategoryDeleteCommand.of(cat)));

        assertThat(sphereClient.executeBlocking(CategoryQuery.of().withLimit(0)).getTotal())
                .as("to test the import no categories should exist in the project " + project.getKey())
                .isEqualTo(0);
    }

    @Test
    public void jobCreatesCategories() throws Exception {
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        final CategoryQuery categoryQuery = CategoryQuery.of().withLimit(0);
        final PagedQueryResult<Category> categoryPagedQueryResult = sphereClient.executeBlocking(categoryQuery);
        assertThat(categoryPagedQueryResult.getTotal()).isEqualTo(131);

        final Optional<Category> womenClothingJacketsOptional = sphereClient.executeBlocking(CategoryQuery.of().withPredicates(m -> m.externalId().is("20")).withExpansionPaths(m -> m.ancestors())).head();
        assertThat(womenClothingJacketsOptional).isPresent();
        final Category category = womenClothingJacketsOptional.get();
        final SoftAssertions soft = new SoftAssertions();
        soft.assertThat(category.getName()).isEqualTo(LocalizedString.of(Locale.GERMAN, "Jacken").plus(Locale.ENGLISH, "Jackets").plus(Locale.ITALIAN, "Giacche"));
        soft.assertThat(category.getSlug()).isEqualTo(LocalizedString.of(Locale.GERMAN, "women-clothing-jackets").plus(Locale.ENGLISH, "women-clothing-jackets").plus(Locale.ITALIAN, "women-clothing-jackets"));
        soft.assertThat(category.getExternalId()).isEqualTo("20");
        final List<String> ancestorExternalIds = category.getAncestors().stream()
                .map(a -> a.getObj().getExternalId())
                .collect(toList());
        soft.assertThat(ancestorExternalIds).containsExactly("2", "10");
        soft.assertAll();
    }
}