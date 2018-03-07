package com.commercetools.dataimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.projects.Project;
import io.sphere.sdk.projects.commands.ProjectUpdateCommand;
import io.sphere.sdk.projects.commands.updateactions.ChangeCountries;
import io.sphere.sdk.projects.commands.updateactions.ChangeCurrencies;
import io.sphere.sdk.projects.commands.updateactions.ChangeLanguages;
import io.sphere.sdk.projects.queries.ProjectGet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
@Slf4j
public class ProjectSettingsStepConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.project}")
    private Resource projectResource;

    @Bean
    @JobScope
    public Step projectSettingsStep() {
        return stepBuilderFactory.get("projectSettingsStep")
                .tasklet(projectSettingsStepTasklet())
                .build();
    }

    private Tasklet projectSettingsStepTasklet() {
        return (contribution, chunkContext) -> {
            final List<UpdateAction<Project>> updateActions = new ArrayList<>();
            final JsonNode jsonNode = MAPPER.readTree(projectResource.getInputStream());
            final List<String> currencies = extractValues(jsonNode, "currencies");
            final List<CountryCode> countries = extractValues(jsonNode, "countries").stream()
                    .map(CountryCode::getByCode)
                    .collect(toList());
            final List<String> languages = extractValues(jsonNode, "languages");
            if (!currencies.isEmpty()) {
                updateActions.add(ChangeCurrencies.of(currencies));
            }
            if (!countries.isEmpty()) {
                updateActions.add(ChangeCountries.of(countries));
            }
            if (!languages.isEmpty()) {
                updateActions.add(ChangeLanguages.of(languages));
            }
            if (!updateActions.isEmpty()) {
                final Project project = sphereClient.executeBlocking(ProjectGet.of());
                sphereClient.executeBlocking(ProjectUpdateCommand.of(project, updateActions));
                log.debug("Updated project settings");
            }
            return RepeatStatus.FINISHED;
        };
    }

    private List<String> extractValues(final JsonNode jsonNode, final String fieldName) {
        final JsonNode arrNode = jsonNode.get(fieldName);
        final List<String> values = new ArrayList<>();
        if (arrNode.isArray()) {
            arrNode.forEach(value -> values.add(value.asText()));
        }
        return values;
    }
}
