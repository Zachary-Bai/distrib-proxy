package com.zachary.distribproxy.config;

import com.zachary.distribproxy.manager.ProxyDistributeManager;
import com.zachary.distribproxy.model.Client;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.model.ProxyPool;
import com.zachary.distribproxy.util.ClientRedisOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @author zachary
 */
@Configuration
@EnableConfigurationProperties
public class ProxyPoolConfig {

    @Autowired
    private RedisTemplate redisTemplate;

    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    RedisTemplate redisTemplate() {
        return new RedisTemplate();
    }

    /**
     * 加载YML格式自定义配置文件
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        // class 引入
        yaml.setResources(new ClassPathResource("proxy-pool.yml"));
        configurer.setProperties(yaml.getObject());
        return configurer;
    }

    @Bean
    @ConfigurationProperties("proxy-pool")
    ProxyPool proxyPool() {
        return new ProxyPool();
    }

    @Bean
    SetOperations<String, Proxy> opsForSetProxy() {
        return redisTemplate.opsForSet();
    }

    @Bean
    ListOperations<String, Proxy> opsForListProxy() {
        return redisTemplate.opsForList();
    }

    @Bean
    ValueOperations<String, Client> opsForValueClient() {
        return redisTemplate.opsForValue();
    }

    @Bean
    ClientRedisOperation clientRedisOperation(ProxyPool proxyPool) {
        return new ClientRedisOperation(proxyPool);
    }

    @Bean(initMethod = "init")
    ProxyDistributeManager proxyDistributeManager() {
        return new ProxyDistributeManager();
    }

}
