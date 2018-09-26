package com.zachary.distribproxy.manager;

import com.zachary.distribproxy.model.Client;
import com.zachary.distribproxy.model.Group;
import com.zachary.distribproxy.model.Proxy;
import com.zachary.distribproxy.util.ClientRedisOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zachary
 */
public class ProxyDistributeManager {

    private static final Logger logger = LoggerFactory.getLogger(ProxyDistributeManager.class);

    @Autowired
    private ClientRedisOperation clientRedisOperation;

    /**
     * 初始化代理池
     */
    private void init() {
        clientRedisOperation.refreshProxyPool();
    }

    /**
     * 使用proxy
     *
     * @param client
     * @return
     */
    public boolean useProxy(Client client) {
        checkClient(client);
        // 获取到client
        if (clientRedisOperation.getClient(client)) {
            // 刷新超时时间
            clientRedisOperation.refreshClient(client);
            return true;
        } else {
            Proxy proxy = clientRedisOperation.groupGet(client.getGroupId());
            if (proxy == null) {
                logger.warn("---- rightPopLeftPush proxy from group [{}] failed", client.getGroupId());
                return false;
            } else {
                client.setUsingProxy(proxy);
                // set 成功
                if (clientRedisOperation.setClient(client)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }


    /**
     * 归还proxy
     *
     * @param client
     * @return
     */
    public boolean returnProxy(Client client) {
        checkClient(client);
        return clientRedisOperation.rmClient(client);
    }

    private void checkClient(Client client) {
        if (StringUtils.isEmpty(client.getGroupId()) || StringUtils.isEmpty(client.getClientId())) {
            throw new IllegalArgumentException("client groupId or clientId not exist");
        }
    }

    @Deprecated
    private void updateGroup(Group group) {
        //  重新获取已经使用的proxy
        group.setUsedProxy(clientRedisOperation.hasUsedProxies(group.getGroupId()));
    }

    /**
     * 从集合中随机取出一个未使用的proxy
     *
     * @param group
     * @return
     */
    @Deprecated
    private Proxy randomGet(Group group) {
        updateGroup(group);
        Set<Proxy> notUsedProxies = new HashSet<>();
        notUsedProxies.addAll(group.getProxyPool());
        // 剩余待使用的proxy
        notUsedProxies.removeAll(group.getUsedProxy());
        if (CollectionUtils.isEmpty(notUsedProxies)) {
            return null;
        }
        return new ArrayList<>(notUsedProxies).get((int) (Math.random() * notUsedProxies.size()));
    }

}
