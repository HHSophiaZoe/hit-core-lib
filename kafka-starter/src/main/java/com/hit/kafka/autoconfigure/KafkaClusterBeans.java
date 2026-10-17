package com.hit.kafka.autoconfigure;

import org.apache.kafka.common.config.SslConfigs;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Listener;
import org.springframework.boot.kafka.autoconfigure.SslBundleSslEngineFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.thread.Threading;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.micrometer.KafkaTemplateObservationConvention;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds the beans of one cluster the way Spring Boot builds them for {@code spring.kafka.*}: factory customizers,
 * error handler, interceptors, converters, rebalance listener, producer listener and virtual threads come from the
 * application beans and apply to every cluster.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class KafkaClusterBeans {

    private final String beanPrefix;

    private final KafkaClusterProperties cluster;

    private final boolean single;

    private final ListableBeanFactory beanFactory;

    private final Environment environment;

    /**
     * @param single whether it is the only cluster: its beans then also get the default names
     */
    KafkaClusterBeans(String name, KafkaClusterProperties cluster, boolean single, ListableBeanFactory beanFactory,
                      Environment environment) {
        this.beanPrefix = name + "Kafka";
        this.cluster = cluster;
        this.single = single;
        this.beanFactory = beanFactory;
        this.environment = environment;
    }

    /**
     * Registers {@code <name>KafkaProducerFactory}, {@code <name>KafkaConsumerFactory}, {@code <name>KafkaTemplate},
     * {@code <name>KafkaListenerContainerFactory} and {@code <name>KafkaAdmin}. The only cluster also answers to the
     * default names ({@code kafkaTemplate}, {@code kafkaListenerContainerFactory}...), so a {@code @KafkaListener}
     * needs no {@code containerFactory}; a bean the application declared under a default name is kept.
     */
    void register(BeanDefinitionRegistry registry) {
        this.register(registry, "ProducerFactory", DefaultKafkaProducerFactory.class, this::producerFactory);
        this.register(registry, "ConsumerFactory", DefaultKafkaConsumerFactory.class, this::consumerFactory);
        this.register(registry, "Template", KafkaTemplate.class, this::template);
        this.register(registry, "ListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class,
                this::listenerContainerFactory);
        this.register(registry, "Admin", KafkaAdmin.class, this::admin);
    }

    private <T> void register(BeanDefinitionRegistry registry, String suffix, Class<T> type, Supplier<T> supplier) {
        String beanName = beanPrefix + suffix;
        registry.registerBeanDefinition(beanName, new RootBeanDefinition(type, supplier));
        String defaultName = "kafka" + suffix;
        if (single && !registry.containsBeanDefinition(defaultName) && !registry.isAlias(defaultName)) {
            registry.registerAlias(beanName, defaultName);
        }
    }

    private String producerFactoryName() {
        return beanPrefix + "ProducerFactory";
    }

    private String consumerFactoryName() {
        return beanPrefix + "ConsumerFactory";
    }

    private String templateName() {
        return beanPrefix + "Template";
    }

    private DefaultKafkaProducerFactory<Object, Object> producerFactory() {
        Map<String, Object> config = cluster.buildProducerProperties();
        this.applySslBundle(config, cluster.getProducer().getSsl().getBundle());
        DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(config);
        PropertyMapper.get().from(cluster.getProducer().getTransactionIdPrefix()).to(factory::setTransactionIdPrefix);
        beanFactory.getBeanProvider(DefaultKafkaProducerFactoryCustomizer.class).orderedStream()
                .forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    private DefaultKafkaConsumerFactory<Object, Object> consumerFactory() {
        Map<String, Object> config = cluster.buildConsumerProperties();
        this.applySslBundle(config, cluster.getConsumer().getSsl().getBundle());
        DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(config);
        beanFactory.getBeanProvider(DefaultKafkaConsumerFactoryCustomizer.class).orderedStream()
                .forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    private KafkaTemplate<Object, Object> template() {
        KafkaTemplate<Object, Object> template = new KafkaTemplate<>(
                beanFactory.getBean(this.producerFactoryName(), DefaultKafkaProducerFactory.class));
        this.ifUnique(RecordMessageConverter.class, template::setMessageConverter);
        this.ifUnique(KafkaTemplateObservationConvention.class, template::setObservationConvention);
        this.ifUnique(ProducerListener.class, template::setProducerListener);
        PropertyMapper map = PropertyMapper.get();
        var properties = cluster.getTemplate();
        map.from(properties.getDefaultTopic()).to(template::setDefaultTopic);
        map.from(properties.getTransactionIdPrefix()).to(template::setTransactionIdPrefix);
        map.from(properties.getCloseTimeout()).to(template::setCloseTimeout);
        map.from(properties.isAllowNonTransactional()).to(template::setAllowNonTransactional);
        map.from(properties.isObservationEnabled()).to(template::setObservationEnabled);
        return template;
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        DefaultKafkaProducerFactory producerFactory = beanFactory.getBean(this.producerFactoryName(), DefaultKafkaProducerFactory.class);
        factory.setConsumerFactory(beanFactory.getBean(this.consumerFactoryName(), DefaultKafkaConsumerFactory.class));
        factory.setReplyTemplate(beanFactory.getBean(this.templateName(), KafkaTemplate.class));
        Listener listener = cluster.getListener();
        PropertyMapper map = PropertyMapper.get();
        map.from(listener::getConcurrency).to(factory::setConcurrency);
        map.from(listener::isAutoStartup).to(factory::setAutoStartup);
        map.from(listener::getType).whenEqualTo(Listener.Type.BATCH).toCall(() -> factory.setBatchListener(true));
        map.from(listener::getChangeConsumerThreadName).to(factory::setChangeConsumerThreadName);
        this.ifUnique(RecordMessageConverter.class, factory::setRecordMessageConverter);
        this.ifUnique(BatchMessageConverter.class, factory::setBatchMessageConverter);
        this.ifUnique(RecordFilterStrategy.class, factory::setRecordFilterStrategy);
        this.ifUnique(CommonErrorHandler.class, factory::setCommonErrorHandler);
        this.ifUnique(AfterRollbackProcessor.class, factory::setAfterRollbackProcessor);
        this.ifUnique(RecordInterceptor.class, factory::setRecordInterceptor);
        this.ifUnique(BatchInterceptor.class, factory::setBatchInterceptor);
        this.ifUnique(ContainerCustomizer.class, factory::setContainerCustomizer);

        ContainerProperties container = factory.getContainerProperties();
        map.from(listener::getAckMode).to(container::setAckMode);
        map.from(listener::getAsyncAcks).to(container::setAsyncAcks);
        map.from(listener::getClientId).to(container::setClientId);
        map.from(listener::getAckCount).to(container::setAckCount);
        map.from(listener::getAckTime).as(Duration::toMillis).to(container::setAckTime);
        map.from(listener::getPollTimeout).as(Duration::toMillis).to(container::setPollTimeout);
        map.from(listener::getNoPollThreshold).to(container::setNoPollThreshold);
        map.from(listener::getIdleBetweenPolls).as(Duration::toMillis).to(container::setIdleBetweenPolls);
        map.from(listener::getIdleEventInterval).as(Duration::toMillis).to(container::setIdleEventInterval);
        map.from(listener::getIdlePartitionEventInterval).as(Duration::toMillis).to(container::setIdlePartitionEventInterval);
        map.from(listener::getMonitorInterval).as(Duration::getSeconds).as(Number::intValue).to(container::setMonitorInterval);
        map.from(listener::getLogContainerConfig).to(container::setLogContainerConfig);
        map.from(listener::isMissingTopicsFatal).to(container::setMissingTopicsFatal);
        map.from(listener::isImmediateStop).to(container::setStopImmediate);
        map.from(listener::isObservationEnabled).to(container::setObservationEnabled);
        map.from(listener::getAuthExceptionRetryInterval).to(container::setAuthExceptionRetryInterval);
        this.ifUnique(ConsumerAwareRebalanceListener.class, container::setConsumerRebalanceListener);
        if (producerFactory.transactionCapable()) {
            // Not a bean: a second transaction manager would make Spring Boot drop the JPA one
            container.setKafkaAwareTransactionManager(new KafkaTransactionManager<>(producerFactory));
        }
        if (Threading.VIRTUAL.isActive(environment)) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(beanPrefix + "-");
            executor.setVirtualThreads(true);
            container.setListenerTaskExecutor(executor);
        }
        return factory;
    }

    private KafkaAdmin admin() {
        Map<String, Object> config = cluster.buildAdminProperties();
        this.applySslBundle(config, cluster.getAdmin().getSsl().getBundle());
        KafkaAdmin admin = new KafkaAdmin(config);
        var properties = cluster.getAdmin();
        PropertyMapper map = PropertyMapper.get();
        map.from(properties.getCloseTimeout()).asInt(Duration::getSeconds).to(admin::setCloseTimeout);
        map.from(properties.getOperationTimeout()).asInt(Duration::getSeconds).to(admin::setOperationTimeout);
        admin.setFatalIfBrokerNotAvailable(properties.isFailFast());
        admin.setModifyTopicConfigs(properties.isModifyTopicConfigs());
        // NewTopic beans carry no cluster: only the only cluster creates them (KafkaClustersRegistrar refuses them
        // when there are several)
        admin.setAutoCreate(single && properties.isAutoCreate());
        return admin;
    }

    /**
     * The bundle of the client ({@code producer.ssl.bundle}...) or else the cluster one ({@code ssl.bundle}).
     */
    private void applySslBundle(Map<String, Object> config, @Nullable String clientBundle) {
        String bundleName = clientBundle != null ? clientBundle : cluster.getSsl().getBundle();
        if (bundleName == null) {
            return;
        }
        SslBundle bundle = beanFactory.getBean(SslBundles.class).getBundle(bundleName);
        config.put(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG, SslBundleSslEngineFactory.class);
        config.put(SslBundle.class.getName(), bundle);
    }

    private <T> void ifUnique(Class<T> type, Consumer<T> action) {
        beanFactory.getBeanProvider(type).ifUnique(action);
    }
}
