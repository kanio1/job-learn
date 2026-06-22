package lab.paymentquality.testing.internal.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("seed & !prod")
class SeedRunner implements ApplicationRunner {

    private final DeterministicDataset dataset;

    SeedRunner(DeterministicDataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void run(ApplicationArguments args) {
        dataset.seed();
    }
}
