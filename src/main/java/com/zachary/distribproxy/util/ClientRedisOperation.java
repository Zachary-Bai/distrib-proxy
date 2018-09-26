package com.zachary.distribproxy.util;

import com.zachary.distribproxy.constant.Constants;
import com.zachary.distribproxy.model.Client;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.model.ProxyPool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zachary
 */
@RequiredArgsConstructor
public class ClientRedisOperation {

    private final ProxyPool proxyPool;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SetOperations<String, Proxy> opsForSetProxy;

    @Autowired
    private ListOperations<String, Proxy> opsForListProxy;

    @Autowired
    private ValueOperations<String, Client> opsForValueClient;


    /**
     * 获取proxyPool
     *
     * @return
     */
    public Set<Proxy> obtainProxyPool() {
        Set<Proxy> proxies = opsForSetProxy.members(Constants.PROXY_POOL_KEY);
        if (proxies == null) {
            refreshProxyPool();
        }
        return opsForSetProxy.members(Constants.PROXY_POOL_KEY);
    }

    /**
     * 刷新proxyPool
     *
     * @return
     */
    public Long refreshProxyPool() {
        // 替换池
        redisTemplate.delete(Constants.PROXY_POOL_KEY);
        return opsForSetProxy.add(Constants.PROXY_POOL_KEY, proxyPool.getProxiesArr());
    }

    /**
     * 随机获取一个代理proxy
     *
     * @return
     */
    public Proxy randomGet() {
        return opsForSetProxy.randomMember(Constants.PROXY_POOL_KEY);
    }


    /**
     * @param groupId
     * @return
     */
    public Proxy groupGet(String groupId) {
        // 不存在key或size不对，则更新代理池
        if (!redisTemplate.hasKey(getGroupKey(groupId)) ||
                opsForListProxy.size(getGroupKey(groupId)) < obtainProxyPool().size()) {
            redisTemplate.delete(getGroupKey(groupId));
            opsForListProxy.leftPushAll(getGroupKey(groupId), obtainProxyPool());
            redisTemplate.expire(getGroupKey(groupId), 30, TimeUnit.DAYS);
        }
        return opsForListProxy.rightPopAndLeftPush(getGroupKey(groupId), getGroupKey(groupId));
    }

    /**
     * 获取当前group已经使用的proxy
     *
     * @param groupId
     * @return
     */
    @Deprecated
    public Set<Proxy> hasUsedProxies(String groupId) {
        Set<String> keys = redisTemplate.keys(Constants.GROUP_PROXY_PREFIX + groupId + "_*");
        return keys.stream().map(key -> opsForValueClient.get(key).getUsingProxy()).collect(Collectors.toSet());
    }


    /**
     * 设置client，表示正在使用
     *
     * @param client
     * @return
     */
    public Boolean setClient(Client client) {
        long timeoutMills = client.getTimeoutMills();
        if (timeoutMills <= 0) {
            timeoutMills = Constants.DEFAULT_TIME_OUT_MILLIS;
        }

        Boolean result = opsForValueClient.setIfAbsent(getProxyKey(client), client);
        if (!result) {
            return false;
        }
        opsForValueClient.set(getClientKey(client), client, timeoutMills, TimeUnit.MILLISECONDS);
        result = redisTemplate.expire(getProxyKey(client), timeoutMills, TimeUnit.MILLISECONDS);
        return result;
    }

    /**
     * 获取client，并刷新超时时间
     *
     * @param client
     * @return
     */
    public Boolean getClient(Client client) {
        Client clientGet = opsForValueClient.get(getClientKey(client));
        //  超时或不存在
        if (clientGet == null) {
            return false;
        }
        client.setUsingProxy(clientGet.getUsingProxy());
        return true;
    }

    /**
     * 刷新client
     *
     * @param client
     * @return
     */
    public Boolean refreshClient(Client client) {
        rmClient(client);
        return setClient(client);
    }

    /**
     * 删除client
     *
     * @param client
     * @return
     */
    public Boolean rmClient(Client client) {
        getClient(client);
        Boolean result = redisTemplate.delete(getProxyKey(client));
        result = result && redisTemplate.delete(getClientKey(client));
        return result;
    }

    private String getGroupKey(String groupId) {
        return Constants.GROUP_PREFIX + groupId;
    }

    private String getProxyKey(Client client) {
        return Constants.GROUP_PROXY_PREFIX + client.getGroupId() + "_" + client.getUsingProxy().getHost();
    }

    private String getClientKey(Client client) {
        return Constants.GROUP_CLIENT_PREFIX + client.getGroupId() + "_" + client.getClientId();
    }

}
