package com.zachary.distribproxy.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author zachary
 */
@Data
@RequiredArgsConstructor
public class Client {

    /**
     * 所属组
     */
    @NonNull
    private String groupId;
    @NonNull
    private String clientId;
    /**
     * 正在使用的proxy
     */
    private Proxy usingProxy;
    /**
     *  使用超时
     */
    private long timeoutMills = 0;

}
