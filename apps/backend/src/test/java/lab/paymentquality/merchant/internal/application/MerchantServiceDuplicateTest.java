package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MerchantServiceDuplicateTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer<?> postgres = newPostgresContainer("merchant_duplicate_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    MerchantService service;

    @Test
    void nearSimultaneousDuplicateReferenceHasOneWinner() throws Exception {
        String reference = "MERCH-CONCURRENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return service.create(reference, "Concurrent Merchant");
                } catch (DuplicateMerchantReferenceException e) {
                    return e;
                }
            }));
        }

        ready.await();
        start.countDown();

        List<Object> results = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();

        assertThat(results).filteredOn(DuplicateMerchantReferenceException.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(result -> !DuplicateMerchantReferenceException.class.isInstance(result)).hasSize(1);

        executor.shutdownNow();
    }
}
