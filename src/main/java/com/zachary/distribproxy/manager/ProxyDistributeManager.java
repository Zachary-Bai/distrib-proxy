package com.zachary.distribproxy.manager;

import com.zachary.distribproxy.model.Client;
import com.zachary.distribproxy.model.Group;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.model.ProxyPool;
import com.zachary.distribproxy.util.ProxyRedisOperation;
import com.zachary.distribproxy.util.RedisLock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author zachary
 */
@RequiredArgsConstructor
public class ProxyDistributeManager {

    private static final Logger logger = LoggerFactory.getLogger(ProxyDistributeManager.class);

    private final ProxyPool proxyPool;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProxyRedisOperation proxyRedisOperation;


    /**
     * 初始化代理池
     */
    private void init() {
        List<Proxy> proxyList = proxyPool.getProxyList();
        Proxy[] proxies = new Proxy[proxyList.size()];
        proxyList.toArray(proxies);
        proxyRedisOperation.fillUpProxyPool(proxies);
    }

    /**
     * true 表示使用成功
     *
     * @param client
     * @param force
     * @return
     */
    public boolean useProxy(Client client, boolean force) {
        Group group = new Group(client.getGroupId(), proxyRedisOperation.obtainProxyPool(), proxyRedisOperation.hasUsedProxy(client.getGroupId()));
        RedisLock groupLock = new RedisLock(redisTemplate, group.getGroupId());
        Proxy wanted = client.getUsingProxy();
        // 指定想要使用的代理IP使用
        if (wanted != null) {
            // 强制使用
            if (force) {
                // ip池存在wanted
                if (group.getProxyPool().contains(wanted)) {
                    proxyRedisOperation.setProxy(client, wanted);
                    return true;
                } else {
                    // ip池不存在wanted
                    client.setUsingProxy(null);
                    return false;
                }
                // 非强制使用
            } else {
                try {
                    if (groupLock.lock()) {
                        // 重新获取已经使用的proxy
                        group.setUsedProxy(proxyRedisOperation.hasUsedProxy(group.getGroupId()));
                        // 已经被占用
                        if (group.getUsedProxy().contains(wanted)) {
                            client.setUsingProxy(null);
                            return false;
                            // 未被占用
                        } else {
                            proxyRedisOperation.setProxy(client, wanted);
                            // 加入使用池
                            group.getUsedProxy().add(wanted);
                            return true;
                        }
                    } else {
                        logger.warn("try lock 10s timeout, can not distribute a proxy for client: " + client.toString());
                        return false;
                    }
                } catch (InterruptedException e) {
                    logger.error("try lock InterruptedException: ", e);
                    return false;
                } finally {
                    groupLock.unlock();
                }
            }
            // 未指定想要使用的代理IP
        } else {
            // 之前使用的proxy未过期
            Proxy proxy = proxyRedisOperation.getProxy(client);
            if (proxy != null) {
                // 刷新超时时间
                proxyRedisOperation.setProxy(client, proxy);
                return true;
            } else {
                try {
                    if (groupLock.lock()) {
                        // 重新获取已经使用的proxy
                        group.setUsedProxy(proxyRedisOperation.hasUsedProxy(group.getGroupId()));
                        Set<Proxy> temp = new HashSet<>();
                        temp.addAll(group.getProxyPool());
                        // 剩余待使用的proxy
                        temp.removeAll(group.getUsedProxy());
                        proxy = randomGet(temp);

                        if (proxy == null) {
                            return false;
                        } else {
                            proxyRedisOperation.setProxy(client, proxy);
                            client.setUsingProxy(proxy);
                            return true;
                        }
                    } else {
                        logger.warn("try lock 10s timeout, can not distribute a proxy for client: " + client.toString());
                        return false;
                    }
                } catch (InterruptedException e) {
                    logger.error("try lock InterruptedException: ", e);
                    return false;
                } finally {
                    groupLock.unlock();
                }
            }
        }
    }

    /**
     * 从集合中随机取出一个未使用的proxy
     *
     * @param notUsedProxies
     * @return
     */
    private Proxy randomGet(Set<Proxy> notUsedProxies) {
        if (CollectionUtils.isEmpty(notUsedProxies)) {
            return null;
        }
        return new ArrayList<>(notUsedProxies).get((int) (Math.random() * notUsedProxies.size()));
    }


    /**
     * 归还代理
     *
     * @param client
     */
    public void returnProxy(Client client) {
        RedisLock groupLock = new RedisLock(redisTemplate, client.getGroupId());
        try {
            if (groupLock.lock()) {
                proxyRedisOperation.returnProxy(client);
            } else {
                logger.warn("try lock 10s timeout, can not distribute a proxy for client: " + client.toString());
            }
        } catch (InterruptedException e) {
            logger.error("try lock InterruptedException: ", e);
        } finally {
            groupLock.unlock();
        }
    }


}
