package com.zachary.distribproxy.config;

import com.zachary.distribproxy.manager.ProxyDistributeManager;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.model.ProxyPool;
import com.zachary.distribproxy.model.TTLProxy;
import com.zachary.distribproxy.util.ProxyRedisOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

/**
 * @author zachary
 */
@Configuration
@EnableConfigurationProperties
public class ProxyPoolConfig {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加载YML格式自定义配置文件
     *
     * @return
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        // class引入
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
    SetOperations<String, Proxy> opsForSet() {
        return redisTemplate.opsForSet();
    }

    @Bean
    HashOperations<String, String, TTLProxy> opsForHash() {
        return redisTemplate.opsForHash();
    }

    @Bean
    ProxyRedisOperation proxyRedisOperation() {
        return new ProxyRedisOperation();
    }

    @Bean(initMethod = "init")
    ProxyDistributeManager proxyDistributeManager(ProxyPool proxyPool) {
        return new ProxyDistributeManager(proxyPool);
    }


}
