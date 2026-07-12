package lab.paymentquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.core.EventSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableScheduling
public class PaymentQualityApplication {

    @Bean
    EventSerializer eventSerializer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new EventSerializer() {
            @Override
            public Object serialize(Object event) {
                try {
                    return objectMapperProvider.getObject().writeValueAsString(event);
                } catch (JacksonException exception) {
                    throw new IllegalStateException("Failed to serialize application event", exception);
                }
            }

            @Override
            public <T> T deserialize(Object serializedEvent, Class<T> eventType) {
                if (!(serializedEvent instanceof String json)) {
                    throw new IllegalArgumentException("Serialized application event must be JSON text");
                }

                try {
                    return objectMapperProvider.getObject().readValue(json, eventType);
                } catch (JacksonException exception) {
                    throw new IllegalStateException("Failed to deserialize application event", exception);
                }
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentQualityApplication.class, args);
    }
}
