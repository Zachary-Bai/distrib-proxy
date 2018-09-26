package com.zachary.distribproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * @author zachary
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Group {

    @NonNull
    private String groupId;
    /**
     * 代理池
     */
    private Set<Proxy> proxyPool;
    /**
     * 已使用的proxy
     */
    private Set<Proxy> usedProxy;


}
