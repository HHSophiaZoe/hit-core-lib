package com.hit.kafka.autoconfigure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registers the beans of every cluster in messaging.kafka.clusters. With several clusters there is no default
 * cluster: KafkaTemplate injections need a qualifier, KafkaListeners a containerFactory, and NewTopic beans are
 * refused since they carry no cluster.
 */
class KafkaClustersRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private Environment environment;

    private ListableBeanFactory beanFactory;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        HitKafkaProperties properties = Binder.get(environment)
                .bind(HitKafkaProperties.PREFIX, HitKafkaProperties.class)
                .orElseGet(HitKafkaProperties::new);
        if (properties.getClusters().isEmpty()) {
            return;
        }
        boolean single = properties.getClusters().size() == 1;
        properties.getClusters().forEach((name, cluster) ->
                new KafkaClusterBeans(name, cluster, single, beanFactory, environment).register(registry));
        if (!single) {
            List<String> admins = properties.getClusters().keySet().stream().map(name -> name + "KafkaAdmin").toList();
            registry.registerBeanDefinition("kafkaClustersNewTopicGuard",
                    new RootBeanDefinition(NewTopicGuard.class, () -> new NewTopicGuard(beanFactory, admins)));
        }
    }

    /**
     * Fails the startup when NewTopic beans meet several clusters, instead of creating them nowhere.
     */
    static final class NewTopicGuard implements SmartInitializingSingleton {

        private final ListableBeanFactory beanFactory;

        private final List<String> admins;

        NewTopicGuard(ListableBeanFactory beanFactory, List<String> admins) {
            this.beanFactory = beanFactory;
            this.admins = admins;
        }

        @Override
        public void afterSingletonsInstantiated() {
            List<String> topics = new ArrayList<>(Arrays.asList(beanFactory.getBeanNamesForType(NewTopic.class)));
            topics.addAll(Arrays.asList(beanFactory.getBeanNamesForType(KafkaAdmin.NewTopics.class)));
            if (!topics.isEmpty()) {
                throw new IllegalStateException(("NewTopic beans %s cannot be used with several Kafka clusters, since "
                        + "they do not say on which cluster; create the topics with createOrModifyTopics(...) on one "
                        + "of %s").formatted(topics, admins));
            }
        }
    }
}
