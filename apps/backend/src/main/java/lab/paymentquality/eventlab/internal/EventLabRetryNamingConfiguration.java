package lab.paymentquality.eventlab.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.retrytopic.DestinationTopic;
import org.springframework.kafka.retrytopic.RetryTopicComponentFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.kafka.retrytopic.RetryTopicNamesProviderFactory;
import org.springframework.kafka.retrytopic.SuffixingRetryTopicNamesProviderFactory;

/**
 * Renames the {@code @RetryableTopic} dead-letter destination to the contract
 * topic {@link EventLabTopics#DLT}. Retry topics keep the default "-retry"
 * suffix. Without this override Spring forwards poison to
 * {@code lab.auditable-actions.v1-dlt}, not the lab DLT.
 */
@Configuration
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
class EventLabRetryNamingConfiguration extends RetryTopicConfigurationSupport {

    @Override
    protected RetryTopicComponentFactory createComponentFactory() {
        return new RetryTopicComponentFactory() {
            @Override
            public RetryTopicNamesProviderFactory retryTopicNamesProviderFactory() {
                SuffixingRetryTopicNamesProviderFactory delegate =
                        new SuffixingRetryTopicNamesProviderFactory();
                return properties -> {
                    if (properties.isDltTopic()) {
                        // Rename every DLT destination to the single contract topic.
                        return new RetryTopicNamesProviderFactory.RetryTopicNamesProvider() {
                            @Override
                            public String getTopicName(String topic) {
                                return EventLabTopics.DLT;
                            }

                            @Override
                            public String getEndpointId(org.springframework.kafka.config.KafkaListenerEndpoint endpoint) {
                                return delegate.createRetryTopicNamesProvider(properties).getEndpointId(endpoint);
                            }

                            @Override
                            public String getGroupId(org.springframework.kafka.config.KafkaListenerEndpoint endpoint) {
                                return delegate.createRetryTopicNamesProvider(properties).getGroupId(endpoint);
                            }

                            @Override
                            public String getClientIdPrefix(org.springframework.kafka.config.KafkaListenerEndpoint endpoint) {
                                return delegate.createRetryTopicNamesProvider(properties).getClientIdPrefix(endpoint);
                            }

                            @Override
                            public String getGroup(org.springframework.kafka.config.KafkaListenerEndpoint endpoint) {
                                return delegate.createRetryTopicNamesProvider(properties).getGroup(endpoint);
                            }
                        };
                    }
                    return delegate.createRetryTopicNamesProvider(properties);
                };
            }
        };
    }
}
