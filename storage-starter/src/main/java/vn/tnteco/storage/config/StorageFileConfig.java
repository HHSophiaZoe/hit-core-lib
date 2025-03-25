package vn.tnteco.storage.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.tnteco.storage.constant.FTPModeEnum;
import vn.tnteco.storage.constant.StorageEnum;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageFileConfig {

    private static final Integer DEFAULT_URL_EXPIRE_TIME = 7;

    private StorageEnum storage;

    private String endpoint;

    private String host;

    private Integer port;

    private String username;

    private String password;

    private FTPModeEnum ftpMode;

    private String minioBucketName;

    private Integer urlExpireTime;

    public FTPModeEnum getFtpMode() {
        return ftpMode == null ? FTPModeEnum.PASSIVE_MODE : ftpMode;
    }

    public Integer getUrlExpireTime() {
        return urlExpireTime == null ? DEFAULT_URL_EXPIRE_TIME : urlExpireTime;
    }
}
