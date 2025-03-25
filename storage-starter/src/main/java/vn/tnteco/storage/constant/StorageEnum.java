package vn.tnteco.storage.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum StorageEnum {

    SFTP("sftp"),
    FTP("ftp"),
    MINIO("minio");

    private final String value;

    private static final Map<String, StorageEnum> mappingValue = new HashMap<>();

    static {
        for (StorageEnum e : StorageEnum.values()) {
            mappingValue.put(e.value, e);
        }
    }

    public static StorageEnum fromValue(String value) {
        return mappingValue.getOrDefault(value, null);
    }

}
