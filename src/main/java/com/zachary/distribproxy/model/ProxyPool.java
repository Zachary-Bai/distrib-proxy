package com.zachary.distribproxy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zachary
 */
@Data
@NoArgsConstructor
public class ProxyPool {

    private static final int DEFAULT_PORT = 80;

    private int port = DEFAULT_PORT;

    private List<String> hosts;

    private String username;

    private String password;

    private List<Proxy> proxyList;

    public void setHosts(List<String> hosts) {
        if (hosts == null || hosts.size() <= 0) {
            return;
        }

        this.hosts = hosts;

        proxyList = new ArrayList<>();
        for (String host : hosts) {
            if (StringUtils.isEmpty(host)) {
                continue;
            }
            String[] line = host.split("\\s");
            String[] result = line[0].split(":");
            int tempPort = this.port;
            if (result.length >= 2) {
                tempPort = Integer.valueOf(result[1]);
            }
            Proxy tempProxy = new Proxy(result[0], tempPort);
            if (line.length >= 2) {
                tempProxy.setUsername(line[1]);
            }
            if (line.length >= 3) {
                tempProxy.setPassword(line[2]);
            }
            proxyList.add(tempProxy);
        }

    }

    public Proxy[] getProxiesArr() {
        Proxy[] proxies = new Proxy[proxyList.size()];
        proxyList.toArray(proxies);
        return proxies;
    }
}
