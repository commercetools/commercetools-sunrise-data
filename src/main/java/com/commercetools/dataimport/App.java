package com.commercetools.dataimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
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

@ComponentScan("com.commercetools.dataimport")
@EnableBatchProcessing
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class App {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String [] args) throws Exception {
        if (args.length > 0) {
            final String payloadFilePath = args[0];
            try {
                final JsonNode payloadJson = MAPPER.readTree(new File(payloadFilePath));
                System.out.println("The payload file is located at " + payloadFilePath);
                run(args, payloadJson);
            } catch (IOException e) {
                System.err.println("Could not load payload file in path " + payloadFilePath);
                System.exit(1);
            }
        } else {
            System.err.println("Missing argument with payload file path");
            System.exit(1);
        }
    }

    private static void run(final String[] args, final JsonNode payloadJson) throws Exception {
        try (final ConfigurableApplicationContext context = SpringApplication.run(App.class, args)) {
            final List<JobLaunchingData> jobLaunchingDataList = buildJobLaunchingDataList(payloadJson);
            final JobLauncher jobLauncher = context.getBean(JobLauncher.class);
            for (final JobLaunchingData jobLaunchingData : jobLaunchingDataList) {
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

    private static List<JobLaunchingData> buildJobLaunchingDataList(final JsonNode payloadJson) {
        final ArrayNode jobsJson = (ArrayNode) payloadJson.get("jobs");
        final List<JobLaunchingData> jobLaunchingData = new ArrayList<>(jobsJson.size());
        for (int i = 0; i < jobsJson.size(); i++) {
            jobLaunchingData.add(buildJobLaunchingData(jobsJson.get(i)));
        }
        return jobLaunchingData;
    }

    private static JobLaunchingData buildJobLaunchingData(final JsonNode jobJson) {
        final String jobName = jobJson.get("name").asText();
        final JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
        jobJson.fields().forEachRemaining(jobField -> {
            if (!jobField.getKey().equals("name")) {
                if (jobField.getValue() instanceof TextNode) {
                    jobParametersBuilder.addString(jobField.getKey(), jobField.getValue().asText());
                } else if (jobField.getValue() instanceof IntNode || jobField.getValue() instanceof LongNode) {
                    jobParametersBuilder.addLong(jobField.getKey(), jobField.getValue().asLong());
                } else {
                    System.err.println(String.format("Ignored job parameter \"%s\" because it could not be parsed", jobField));
                }
            }
        });
        return new JobLaunchingData(jobName, jobParametersBuilder.toJobParameters());
    }
}
