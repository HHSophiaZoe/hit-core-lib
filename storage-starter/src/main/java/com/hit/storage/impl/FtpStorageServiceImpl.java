package com.hit.storage.impl;

import com.hit.storage.util.FileUtils;
import io.micrometer.common.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import com.hit.storage.StorageService;
import com.hit.storage.config.StorageFileConfig;
import com.hit.storage.constant.FileExtensionEnum;
import com.hit.storage.data.FileEntryDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FtpStorageServiceImpl implements StorageService, AutoCloseable {

    private final Session<FTPFile> session;

    public FtpStorageServiceImpl(StorageFileConfig config) {
        log.info("====> connect ftp {}", config.getHost());
        DefaultFtpSessionFactory ftpFileSessionFactory = new DefaultFtpSessionFactory();
        ftpFileSessionFactory.setHost(config.getHost());
        ftpFileSessionFactory.setPort(config.getPort());
        ftpFileSessionFactory.setUsername(config.getUsername());
        ftpFileSessionFactory.setPassword(config.getPassword());
        ftpFileSessionFactory.setClientMode(config.getFtpMode().value());
        ftpFileSessionFactory.setDefaultTimeout(30000); // set timeout for control connection
        ftpFileSessionFactory.setDataTimeout(60000); // set timeout for data transfer
        ftpFileSessionFactory.setControlEncoding("UTF-8");
        session = new FtpRemoteFileTemplate(ftpFileSessionFactory).getSession();
    }

    @Override
    @SneakyThrows
    public List<FileEntryDTO> listFiles(String path) {
        if (!session.exists(path)) {
            log.info("[FTP] Not found path: {}", path);
            return Collections.emptyList();
        }
        return Arrays.stream(session.list(path))
                .filter(FTPFile::isFile)
                .map(file -> FileEntryDTO.builder()
                        .fileName(file.getName())
                        .filePath(FileUtils.joinPaths(path, file.getName()))
                        .lastModifiedDate(file.getTimestamp().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime())
                        .build())
                .toList();
    }

    @Override
    @SneakyThrows
    public List<FileEntryDTO> listFiles(String path, FileExtensionEnum extension) {
        if (!session.exists(path)) {
            log.info("[FTP] Not found path: {}", path);
            return Collections.emptyList();
        }
        return Arrays.stream(session.list(path))
                .filter(file -> file.isFile() && FileUtils.getExtension(file.getName()).equals(extension.value()))
                .map(file -> FileEntryDTO.builder()
                        .fileName(file.getName())
                        .filePath(FileUtils.joinPaths(path, file.getName()))
                        .lastModifiedDate(file.getTimestamp().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime())
                        .build())
                .toList();
    }

    @Override
    @SneakyThrows
    public void makeDirectory(String dirPath) {
        StringBuilder sb = new StringBuilder();
        String[] dirs = dirPath.split(FileUtils.FORWARD_SLASH);
        try {
            for (String dir : dirs) {
                if (StringUtils.isBlank(dir)) continue;
                sb.append(FileUtils.FORWARD_SLASH).append(dir);
                String currentDir = sb.toString();
                if (session.exists(currentDir)) {
                    log.info("[FTP] server path {} existed", currentDir);
                } else {
                    session.mkdir(currentDir);
                }
            }
        } catch (Exception ex) {
            log.error("[FTP] Error while making file FTP server: ", ex);
            throw ex;
        }
    }

    @Override
    @SneakyThrows
    public ByteArrayResource readFile(String filePath) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            session.read(filePath, os);
            return new ByteArrayResource(os.toByteArray()) {
                @Override
                public String getFilename() {
                    return FileUtils.getFilename(filePath);
                }
            };
        } catch (Exception ex) {
            log.error("[FTP] Error downloading file {} from FTP server: {}", filePath, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public String readFileAsUrl(String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeFile(InputStream is, String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile(String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendFiles(String outputFilePath, List<String> filePaths) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SneakyThrows
    public void moveFile(String sourceFilePath, String destinationFilePath) {
        try {
            if (!session.exists(sourceFilePath)) {
                log.info("[FTP] Source file {} does not exist on FTP server.", sourceFilePath);
                return;
            }
            // Tạo thư mục đích đến file nếu chưa tồn tại
            String destinationDir = destinationFilePath.substring(0, destinationFilePath.lastIndexOf(FileUtils.FORWARD_SLASH));
            this.makeDirectory(destinationDir);
            session.rename(sourceFilePath, destinationFilePath);
        } catch (IOException ex) {
            log.error("[FTP] Error move file FTP server from {} to {}: {}", sourceFilePath, destinationFilePath, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void close() {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
