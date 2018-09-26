package com.zachary.distribproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * @author zachary
 */
@Data
@AllArgsConstructor
public class TTLProxy implements Serializable {

    private static final long serialVersionUID = 156510468109665594L;

    private Proxy proxy;

    private Instant instant;

}
