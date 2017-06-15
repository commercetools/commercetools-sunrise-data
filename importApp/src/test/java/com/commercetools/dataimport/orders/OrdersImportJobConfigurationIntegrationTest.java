package com.commercetools.dataimport.orders;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.TestConfiguration;
import com.commercetools.dataimport.IntegrationTest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.commercetools.dataimport.TestUtils.addCommercetoolsCredentialValues;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static com.commercetools.dataimport.TestUtils.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, OrdersImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class OrdersImportJobConfigurationIntegrationTest extends IntegrationTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("test")
    private BlockingSphereClient sphereClient;

    @Autowired
    private Environment env;

    @Bean
    Resource categoryCsvResource() {
        return new FileSystemResource("../orders/beveelorders.csv");
    }


    @Test
    public void jobCreatesOrders() throws Exception {

        Path csvPath = Paths.get("../orders/beveelorders.csv");

        List<String> skus = extractSkusFromCSV(csvPath);
        createProductWithSkus(sphereClient, new HashSet<>(skus));

        final Map<String, JobParameter> jobParametersMap = new HashMap<>();
        jobParametersMap.put("resource", new JobParameter("file://" + csvPath.toAbsolutePath()));
        addCommercetoolsCredentialValues(env, jobParametersMap);
        final JobParameters jobParameters = new JobParameters(jobParametersMap);
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);


        final OrderQuery orderQuery = OrderQuery.of().withLimit(0);
        final PagedQueryResult<Order> categoryPagedQueryResult = sphereClient.executeBlocking(orderQuery);

        assertThat(categoryPagedQueryResult.getTotal()).isEqualTo(ordersCountUsingEmailAndOrderId(csvPath));

        OrderQuery query = OrderQuery.of().withPredicates(model -> model.customerEmail().is("303"))
                .plusPredicates(model -> model.orderNumber().is("1"));
        final PagedQueryResult<Order> firstOrderResult = sphereClient.executeBlocking(query);

        assertThat(firstOrderResult.getCount()).isEqualTo(1);
        Order order = firstOrderResult.getResults().get(0);

        assertThat(order.getTotalPrice()).isEqualTo(MoneyImpl.ofCents(73375, "EUR"));

        assertThat(order.getLineItems()).extracting("variant")
                .extracting("sku")
                .containsExactly("A0E20000000252G", "M0E20000000DZKB", "A0E200000002AD2");

        assertThat(order.getLineItems()).extracting("quantity")
                .containsExactly(1L, 1L, 1L);

    }


    public static Product createProductWithSkus(final BlockingSphereClient sphereClient, Set<String> skus) {
        PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withCountry(CountryCode.DE);
        List<ProductVariantDraft> productVariantDrafts = skus.stream().map(sku -> ProductVariantDraftBuilder.of().sku(sku).build()).collect(Collectors.toList());

        final ProductType productType = createProductType(sphereClient);
        final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "product-name"),
                LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), productVariantDrafts);
        final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.publish(true).build()));
        final AddPrice addPrice = AddPrice.of(product.getMasterData().getCurrent().getMasterVariant().getId(), priceDraft);
        Product productWithPrice = sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));
        return productWithPrice;
    }

    private static List<String> extractSkusFromCSV(Path csvPath) throws IOException {
        return Files.newBufferedReader(csvPath)
                .lines()
                .skip(1)
                .filter(str -> !str.isEmpty())
                .map(line -> line.split(",")[2].trim())
                .collect(Collectors.toList());
    }

    private static long ordersCountUsingEmailAndOrderId(Path csvPath) throws IOException {
        return Files.newBufferedReader(csvPath)
                .lines()
                .skip(1)
                .filter(str -> !str.isEmpty())
                .map(line -> line.split(",")[0].trim() + "_" + line.split(",")[1].trim())
                .distinct()
                .count();
    }


}