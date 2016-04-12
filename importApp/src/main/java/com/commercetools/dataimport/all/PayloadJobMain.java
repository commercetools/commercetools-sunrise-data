package com.commercetools.dataimport.all;

import com.commercetools.dataimport.categories.CategoriesDeleteJobConfiguration;
import com.commercetools.dataimport.categories.CategoriesImportJobConfiguration;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.commercetools.CommercetoolsPayloadFileConfig;
import com.commercetools.dataimport.products.ProductDeleteJobConfiguration;
import com.commercetools.dataimport.products.ProductsImportJobConfiguration;
import com.commercetools.dataimport.producttypes.ProductTypeDeleteJobConfiguration;
import com.commercetools.dataimport.producttypes.ProductTypesImportJobConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class PayloadJobMain extends CommercetoolsJobConfiguration {

    public static void main(String [] args) throws Exception {
        final String parameterStart = "--payloadFile=";
        final Optional<String> payloadFileOptional = Arrays.stream(args)
                .filter(arg -> arg.startsWith(parameterStart))
                .map(s -> s.replace(parameterStart, ""))
                .findFirst();
        if (payloadFileOptional.isPresent()) {
            final String payloadFilePath = payloadFileOptional.get();
            final JsonNode payload = parsePayloadFile(payloadFilePath);
            try(final ConfigurableApplicationContext context = initializeContext(args, payload)) {
                final JobLauncher jobLauncher = context.getBean(JobLauncher.class);
                try {
                    final ArrayNode jobs = (ArrayNode) payload.get("jobs");
                    for(int i = 0; i < jobs.size(); i++) {
                        final JsonNode jobConfig = jobs.get(i);
                        final Job job = context.getBean(jobConfig.get("name").asText(), Job.class);
                        //todo parse to map, to properties, to jobparameter
                        //DefaultJobParametersConverter

                        final JobExecution jobExecution = jobLauncher.run(job, new JobParameters(Collections.singletonMap("payloadFile", new JobParameter(payloadFilePath))));
                        while (jobExecution.isRunning()) {
                            Thread.sleep(1000);//TODO improve
                        }
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                System.exit(SpringApplication.exit(context));
            }
        } else {
            throw new RuntimeException("missing payload file path");
        }
    }

    public static JsonNode parsePayloadFile(final String payloadFilePath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(new File(payloadFilePath));
    }

    @Configuration
    public static class MainConfiguration {
        private String prefix = "https://raw.githubusercontent.com/sphereio/commercetools-sunrise-data/master/";

        public MainConfiguration() {
        }

        @Bean
        Resource categoryCsvResource() throws MalformedURLException {
            return new UrlResource(prefix + "categories/categories.csv");
        }

        @Bean
        Resource productsCsvResource() throws MalformedURLException {
            return new UrlResource(prefix + "products/products.csv");
        }

        @Bean
        Resource productTypesArrayResource() throws MalformedURLException {
            return new UrlResource(prefix + "product-types/product-types.json");
        }

    }

    private static ConfigurableApplicationContext initializeContext(final String[] args, final JsonNode payload) {
        final Object[] sources = {
                CommercetoolsPayloadFileConfig.class,
                ProductTypesImportJobConfiguration.class,
                CategoriesImportJobConfiguration.class,
                ProductsImportJobConfiguration.class,
                ProductDeleteJobConfiguration.class,
                ProductTypeDeleteJobConfiguration.class,
                CategoriesDeleteJobConfiguration.class,
                MainConfiguration.class,
                PayloadJobMain.class
        };
        return SpringApplication.run(sources, args);
    }
}
