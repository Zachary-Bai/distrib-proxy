package com.zachary.distribproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Proxy implements Serializable {

    private static final long serialVersionUID = 8933762645438527323L;
    @NonNull
    private String host;
    @NonNull
    private int port;

    private String username;

    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Proxy proxy = (Proxy) o;
            if (this.port != proxy.port) {
                return false;
            }

            if (this.host != null) {
                if (!this.host.equals(proxy.host)) {
                    return false;
                }
            } else if (proxy.host != null) {
                return false;
            }

            if (this.username != null) {
                if (!this.username.equals(proxy.username)) {
                    return false;
                }
            } else if (proxy.username != null) {
                return false;
            }

            return this.password != null ? this.password.equals(proxy.password) : proxy.password == null;

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.host != null ? this.host.hashCode() : 0;
        result = 31 * result + this.port;
        result = 31 * result + (this.username != null ? this.username.hashCode() : 0);
        result = 31 * result + (this.password != null ? this.password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Proxy{host='" + this.host + '\'' + ", port=" + this.port + ", username='" + this.username + '\'' + ", password='" + this.password + '\'' + '}';
    }

}
