package com.zachary.distribproxy.util;

import com.zachary.distribproxy.constant.Constants;
import com.zachary.distribproxy.model.Client;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.model.TTLProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zachary
 */
public class ProxyRedisOperation {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SetOperations<String, Proxy> opsForSet;

    @Autowired
    private HashOperations<String, String, TTLProxy> opsForHash;

    /**
     * 装填proxyPool
     *
     * @param proxies
     * @return
     */
    public Long fillUpProxyPool(Proxy... proxies) {
        Set<Proxy> proxySet = new HashSet<>(Arrays.asList(proxies));
        Set<Proxy> proxyPool = obtainProxyPool();
        if (proxySet.equals(proxyPool)) {
            return 0L;
        }
        // 替换池
        redisTemplate.delete(Constants.PROXY_POOL_KEY);
        return opsForSet.add(Constants.PROXY_POOL_KEY, proxies);
    }

    /**
     * 获取proxyPool
     *
     * @return
     */
    public Set<Proxy> obtainProxyPool() {
        return opsForSet.members(Constants.PROXY_POOL_KEY);
    }

    /**
     * 获取client正在使用的proxy，若获取到proxy则填充到client
     *
     * @param client
     * @return
     */
    public Proxy getProxy(Client client) {
        if (isTimeoutOrNx(client)) {
            return null;
        }
        return client.getUsingProxy();
    }

    /**
     * 设置client使用的proxy
     *
     * @param client
     * @param proxy
     */
    public void setProxy(Client client, Proxy proxy) {
        long timeoutMills = client.getTimeoutMills();
        if (timeoutMills <= 0) {
            timeoutMills = Constants.DEFAULT_TIME_OUT_MILLIS;
        }
        opsForHash.put(getGroupKey(client.getGroupId()), client.getClientId(), new TTLProxy(proxy, Instant.now().plusMillis(timeoutMills)));
    }

    /**
     * 当前group中所有client已经使用的proxy
     *
     * @param groupId
     * @return
     */
    public Set<Proxy> hasUsedProxy(String groupId) {
        Set<String> clientIds = clientIds(groupId);
        if (CollectionUtils.isEmpty(clientIds)) {
            return Collections.EMPTY_SET;
        }
        return clientIds.stream().map(clientId -> new Client(groupId, clientId)).filter(client -> !isTimeoutOrNx(client))
                .map(client -> client.getUsingProxy()).collect(Collectors.toSet());
    }

    /**
     * 当前group的所有client
     *
     * @param groupId
     * @return
     */
    public Set<String> clientIds(String groupId) {
        Set<String> clientIds = opsForHash.keys(getGroupKey(groupId));
        return clientIds;
    }

    private String getGroupKey(String groupId) {
        return Constants.GROUP_PREFIX + groupId;
    }


    /**
     * 判断某个client对应正在使用的proxy是否超时或不存在
     *
     * @param client
     * @return
     */
    private boolean isTimeoutOrNx(Client client) {
        if (!opsForHash.hasKey(getGroupKey(client.getGroupId()), client.getClientId())) {
            return true;
        }
        TTLProxy ttlProxy = opsForHash.get(getGroupKey(client.getGroupId()), client.getClientId());
        if (ttlProxy == null) {
            return true;
        }
        client.setUsingProxy(ttlProxy.getProxy());
        // value超时
        if (Instant.now().isAfter(ttlProxy.getInstant())) {
            // 归还proxy
            returnProxy(client);
            return true;
        }
        return false;
    }

    /**
     * 归还proxy
     *
     * @param client
     */
    public void returnProxy(Client client) {
        if (!opsForHash.hasKey(getGroupKey(client.getGroupId()), client.getClientId())) {
            return;
        }
        // 删掉key
        opsForHash.delete(getGroupKey(client.getGroupId()), client.getClientId());
    }

}
