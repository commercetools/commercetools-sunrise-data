package com.commercetools.dataimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

@ComponentScan(basePackages = {
        "com.commercetools.dataimport",
        "com.commercetools.dataimport.categories",
        "com.commercetools.dataimport.channels",
        "com.commercetools.dataimport.customers",
        "com.commercetools.dataimport.inventoryentries",
        "com.commercetools.dataimport.orders",
        "com.commercetools.dataimport.products",
        "com.commercetools.dataimport.producttypes"
})
@EnableBatchProcessing
@EnableAutoConfiguration
public class PayloadJobMain extends CommercetoolsJobConfiguration {

    private static final String PAYLOAD_FILE_ENV_NAME = "PAYLOAD_FILE";

    public static void main(String [] args) throws Exception {
        final String payloadFilePath = System.getenv(PAYLOAD_FILE_ENV_NAME);
        if (StringUtils.isNotEmpty(payloadFilePath)) {
            System.out.println(String.format("The payload file is located at %s.", payloadFilePath));
            run(args, payloadFilePath);
        } else {
            System.err.println("Missing payload file path environment variable " + PAYLOAD_FILE_ENV_NAME);
            System.exit(1);
        }
    }

    private static void run(final String[] args, final String payloadFilePath) throws Exception {
        try(final ConfigurableApplicationContext context = SpringApplication.run(PayloadJobMain.class, args)) {
            final List<JobLaunchingData> jobLaunchingDataList = parseJobParameters(payloadFilePath);
            final JobLauncher jobLauncher = context.getBean(JobLauncher.class);
            for(final JobLaunchingData jobLaunchingData : jobLaunchingDataList) {
                final String jobName = jobLaunchingData.getJobName();
                final JobParameters jobParameters = jobLaunchingData.getJobParameters();
                final Job job = context.getBean(jobName, Job.class);
                final JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                awaitTermination(jobExecution, Duration.ofMinutes(30));
                if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
                    throw new JobExecutionUnsuccessfulException(String.format("Job %s was unsuccessful with status %s.", jobName, jobExecution.getExitStatus()));
                }
            }
        }
    }

    private static void awaitTermination(final JobExecution jobExecution, final Duration duration) throws TimeoutException {
        final ZonedDateTime start = ZonedDateTime.now();
        final ZonedDateTime latestEnd = start.plus(duration);
        while (jobExecution.isRunning()) {
            if (ZonedDateTime.now().isAfter(latestEnd)) {
                throw new TimeoutException("timeout after " + duration);
            }
            try {
                Thread.sleep(1000);//TODO improve
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    private static List<JobLaunchingData> parseJobParameters(final String payloadFilePath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode payload = mapper.readTree(new File(payloadFilePath));
        final ArrayNode jobs = (ArrayNode) payload.get("jobs");
        final List<JobLaunchingData> result = new ArrayList<>(jobs.size());
        for(int i = 0; i < jobs.size(); i++) {
            final JsonNode jobJsonNode = jobs.get(i);
            final JobLaunchingData e = getJobLaunchingData(payload, jobJsonNode);
            result.add(e);
        }
        return result;
    }

    private static JobLaunchingData getJobLaunchingData(final JsonNode payload, final JsonNode jobJsonNode) {
        final String jobName = jobJsonNode.get("name").asText();
        final JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
        final JsonNode commercetools = payload.get("commercetools");
        commercetools.fields().forEachRemaining(stringJsonNodeEntry -> {
            jobParametersBuilder.addString("commercetools." + stringJsonNodeEntry.getKey(), stringJsonNodeEntry.getValue().asText());
        });
        jobJsonNode.fields().forEachRemaining(jobField -> {
            //TODO prepare for other classes
            if (jobField.getValue() instanceof TextNode) {
                jobParametersBuilder.addString(jobField.getKey(), jobField.getValue().asText());
            } else if (jobField.getValue() instanceof IntNode || jobField.getValue() instanceof LongNode) {
                jobParametersBuilder.addLong(jobField.getKey(), jobField.getValue().asLong());
            }
        });
        return new JobLaunchingData(jobName, jobParametersBuilder.toJobParameters());
    }
}
