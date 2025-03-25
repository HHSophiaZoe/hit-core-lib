package vn.tnteco.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import vn.tnteco.storage.config.StorageFileConfig;
import vn.tnteco.storage.impl.FtpStorageServiceImpl;
import vn.tnteco.storage.impl.MinioStorageServiceImpl;
import vn.tnteco.storage.impl.SftpStorageServiceImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFactory {

    public StorageService getStorage(StorageFileConfig config) {
        if (ObjectUtils.isEmpty(config)) {
            throw new IllegalArgumentException("Storage File Config required");
        }
        return switch (config.getStorage()) {
            case FTP -> new FtpStorageServiceImpl(config);
            case SFTP -> new SftpStorageServiceImpl();
            case MINIO -> new MinioStorageServiceImpl(config);
        };
    }
}