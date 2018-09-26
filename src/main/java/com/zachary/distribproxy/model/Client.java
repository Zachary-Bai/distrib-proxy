package com.zachary.distribproxy.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * @author zachary
 */
@Data
@RequiredArgsConstructor
public class Client implements Serializable {

    private static final long serialVersionUID = -6933096063299705128L;

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
     * 使用超时
     */
    private long timeoutMills = 0;

}
