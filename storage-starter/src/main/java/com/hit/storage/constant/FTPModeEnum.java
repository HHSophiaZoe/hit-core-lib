package com.hit.storage.constant;

import lombok.AllArgsConstructor;
import org.apache.commons.net.ftp.FTPClient;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum FTPModeEnum {

    PASSIVE_MODE("PASSIVE", FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE), // use local passive mode to pass firewall
    ACTIVE_MODE("ACTIVE", FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE);

    private final String mode;
    private final Integer value;

    public Integer value() {
        return value;
    }

    private static final Map<String, FTPModeEnum> mappingMode = new HashMap<>();

    static {
        for (FTPModeEnum e : FTPModeEnum.values()) {
            mappingMode.put(e.mode, e);
        }
    }

    public static FTPModeEnum fromMode(String value) {
        return mappingMode.getOrDefault(value, null);
    }

}
