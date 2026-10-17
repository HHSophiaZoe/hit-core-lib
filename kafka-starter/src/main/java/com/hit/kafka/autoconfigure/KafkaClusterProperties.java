package com.hit.kafka.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * One Kafka cluster: the keys of spring.kafka.* that the starter wires (connection, SSL, security, producer,
 * consumer, listener, admin, template) plus typed fields for the producer and SASL settings that easily cause
 * errors when left unnoticed, so they need no properties[...] entry. Other Kafka settings go to properties[...].
 * <p>
 * Defaults follow Spring Boot: a field left unset is not sent, and the Kafka client uses its own default, given in
 * each description. Precedence, lowest first: cluster properties, typed fields, client properties
 * (e.g. producer.properties).
 */
@Getter
@Setter
public class KafkaClusterProperties {

    /**
     * Brokers for the first connection to the cluster, as host:port separated by commas, e.g.
     * kafka-1:9092,kafka-2:9092 (bootstrap.servers). Shared by the producer, consumer and admin unless they set their
     * own. Default: localhost:9092, as in Spring Boot.
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * Name of the clients in broker logs and quotas, shared unless a client sets its own (client.id).
     */
    private String clientId;

    /**
     * Raw Kafka settings for every client of the cluster; typed fields and client properties override them.
     */
    private final Map<String, String> properties = new HashMap<>();

    /**
     * SSL for every client of the cluster (ssl.*), or an SSL bundle with ssl.bundle.
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Ssl ssl = new KafkaProperties.Ssl();

    /**
     * Security protocol for every client of the cluster (security.protocol).
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Security security = new KafkaProperties.Security();

    /**
     * SASL authentication for every client of the cluster.
     */
    private final Sasl sasl = new Sasl();

    /**
     * Producer of the cluster.
     */
    private final Producer producer = new Producer();

    /**
     * Consumer of the cluster.
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Consumer consumer = new KafkaProperties.Consumer();

    /**
     * Listener container of the cluster.
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Listener listener = new KafkaProperties.Listener();

    /**
     * Admin client of the cluster (KafkaAdmin).
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Admin admin = new KafkaProperties.Admin();

    /**
     * KafkaTemplate of the cluster.
     */
    @NestedConfigurationProperty
    private final KafkaProperties.Template template = new KafkaProperties.Template();

    public Map<String, Object> buildProducerProperties() {
        Map<String, Object> config = this.buildCommonProperties();
        config.putAll(producer.buildExtraProperties());
        config.putAll(producer.buildProperties());
        return config;
    }

    public Map<String, Object> buildConsumerProperties() {
        Map<String, Object> config = this.buildCommonProperties();
        config.putAll(consumer.buildProperties());
        return config;
    }

    public Map<String, Object> buildAdminProperties() {
        Map<String, Object> config = this.buildCommonProperties();
        config.putAll(admin.buildProperties());
        return config;
    }

    private Map<String, Object> buildCommonProperties() {
        Map<String, Object> config = new HashMap<>();
        PropertyMapper map = PropertyMapper.get();
        map.from(bootstrapServers).to(value -> config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, value));
        map.from(clientId).to(value -> config.put(CommonClientConfigs.CLIENT_ID_CONFIG, value));
        config.putAll(ssl.buildProperties());
        config.putAll(security.buildProperties());
        map.from(sasl.getMechanism()).to(value -> config.put(SaslConfigs.SASL_MECHANISM, value));
        map.from(sasl.getJaasConfig()).to(value -> config.put(SaslConfigs.SASL_JAAS_CONFIG, value));
        config.putAll(properties);
        return config;
    }

    /**
     * SASL settings of the clients. Spring Boot only has spring.kafka.jaas, which is global to the JVM; these
     * are per cluster.
     */
    @Getter
    @Setter
    public static class Sasl {

        /**
         * SASL mechanism (sasl.mechanism). Kafka default: GSSAPI (Kerberos). Use PLAIN,
         * SCRAM-SHA-256 or SCRAM-SHA-512 for username/password clusters, OAUTHBEARER for
         * OAuth. Takes effect with security.protocol SASL_SSL or SASL_PLAINTEXT.
         */
        private String mechanism;

        /**
         * JAAS login of the clients (sasl.jaas.config), e.g.
         * org.apache.kafka.common.security.scram.ScramLoginModule required username="u" password="p";.
         * Kafka default: none. It holds the password, so read it from an environment variable.
         */
        private String jaasConfig;
    }

    /**
     * The Spring Boot producer settings (spring.kafka.producer.*) plus timeouts, batching, idempotence and
     * size limits.
     */
    @Getter
    @Setter
    public static class Producer extends KafkaProperties.Producer {

        /**
         * Upper bound for a send to succeed or fail after send() returns, retries included
         * (delivery.timeout.ms). Kafka default: 2m. Must be at least request-timeout + linger,
         * otherwise the producer refuses to start. A plain number is milliseconds.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration deliveryTimeout;

        /**
         * How long the producer waits for a broker response before retrying or failing (request.timeout.ms).
         * Kafka default: 30s. A plain number is milliseconds.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration requestTimeout;

        /**
         * How long the producer waits for more records to fill a batch before sending it (linger.ms). Kafka
         * default: 5ms. Higher means bigger batches and more throughput, at the cost of latency; 0 sends at once.
         * A plain number is milliseconds.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration linger;

        /**
         * How long send() may block the calling thread while the buffer is full or topic metadata is not
         * known yet (max.block.ms). Kafka default: 1m. Lower it so a broker outage does not hold request
         * threads. A plain number is milliseconds.
         */
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration maxBlock;

        /**
         * Whether retries never write a record twice and keep the order per partition (enable.idempotence).
         * Kafka default: true. Needs acks=all and max-in-flight-requests at most 5.
         */
        private Boolean enableIdempotence;

        /**
         * Unacknowledged requests the producer may send per broker connection
         * (max.in.flight.requests.per.connection). Kafka default: 5. Above 5 idempotence is off and retries
         * may reorder records; 1 keeps order without idempotence but lowers throughput.
         */
        private Integer maxInFlightRequests;

        /**
         * Largest request the producer sends, which caps the size of one record (max.request.size). Kafka
         * default: 1MB. The broker and topic limit (message.max.bytes, max.message.bytes) must allow
         * it too.
         */
        private DataSize maxRequestSize;

        Map<String, Object> buildExtraProperties() {
            Map<String, Object> config = new HashMap<>();
            PropertyMapper map = PropertyMapper.get();
            map.from(deliveryTimeout).asInt(Duration::toMillis).to(value -> config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, value));
            map.from(requestTimeout).asInt(Duration::toMillis).to(value -> config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, value));
            map.from(linger).asInt(Duration::toMillis).to(value -> config.put(ProducerConfig.LINGER_MS_CONFIG, value));
            map.from(maxBlock).as(Duration::toMillis).to(value -> config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, value));
            map.from(enableIdempotence).to(value -> config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, value));
            map.from(maxInFlightRequests).to(value -> config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, value));
            map.from(maxRequestSize).asInt(DataSize::toBytes).to(value -> config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, value));
            return config;
        }
    }
}
