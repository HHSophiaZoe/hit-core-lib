package com.hit.storage;

import com.hit.storage.config.StorageFileConfig;
import com.hit.storage.constant.StorageEnum;
import com.hit.storage.impl.FtpStorageServiceImpl;
import com.hit.storage.impl.MinioStorageServiceImpl;
import com.hit.storage.impl.SftpStorageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFactory {

    public StorageService getStorage(StorageFileConfig config) {
        if (ObjectUtils.isEmpty(config)) {
            throw new IllegalArgumentException("Storage File Config required");
        }
        return switch (config.getStorage()) {
            case StorageEnum.FTP -> new FtpStorageServiceImpl(config);
            case StorageEnum.SFTP -> new SftpStorageServiceImpl();
            case StorageEnum.MINIO -> new MinioStorageServiceImpl(config);
        };
    }
}