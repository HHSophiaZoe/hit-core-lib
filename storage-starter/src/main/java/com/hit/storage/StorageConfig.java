//package com.hit.storage;
//
//import com.hit.storage.config.StorageFileConfig;
//import com.hit.storage.constant.StorageEnum;
//import com.hit.storage.impl.FtpStorageCommandImpl;
//import com.hit.storage.impl.MinioStorageCommandImpl;
//import com.hit.storage.impl.SftpStorageCommandImpl;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.ObjectUtils;
//
//@Configuration
//public class StorageConfig {
//
//    public BaseStorageCommand getStorage(StorageFileConfig config) {
//        if (ObjectUtils.isEmpty(config)) {
//            throw new IllegalArgumentException("Storage File Config required");
//        }
//        return switch (config.getStorage()) {
//            case StorageEnum.FTP -> new FtpStorageCommandImpl(config);
//            case StorageEnum.SFTP -> new SftpStorageCommandImpl(config);
//            case StorageEnum.MINIO -> new MinioStorageCommandImpl(config);
//        };
//    }
//
//}
