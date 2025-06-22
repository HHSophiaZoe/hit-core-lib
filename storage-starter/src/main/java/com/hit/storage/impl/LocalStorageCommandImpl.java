package com.hit.storage.impl;

import com.hit.storage.BaseStorageCommand;
import com.hit.storage.constant.FileExtensionEnum;
import com.hit.storage.data.FileEntryDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SuppressWarnings({"java:S112"})
public class LocalStorageCommandImpl implements BaseStorageCommand {

    @Override
    public List<FileEntryDTO> listFiles(String path) {
        Objects.requireNonNull(path, "Directory path must not be null");
        Path dirPath = Paths.get(path);

        if (!Files.exists(dirPath)) {
            log.warn("Directory does not exist: {}", path);
            return Collections.emptyList();
        }

        if (!Files.isDirectory(dirPath)) {
            log.warn("Path is not a directory: {}", path);
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream.filter(Files::isRegularFile)
                    .map(this::mapToFileEntryDTO)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error listing files in directory: {}", path, e);
            throw new RuntimeException("Failed to list files", e);
        }
    }

    @SneakyThrows
    private FileEntryDTO mapToFileEntryDTO(Path path) {
        return FileEntryDTO.builder()
                .fileName(path.getFileName().toString())
                .filePath(path.toAbsolutePath().toString())
                .lastModifiedDate(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()))
                .build();
    }

    @Override
    public List<FileEntryDTO> listFiles(String path, FileExtensionEnum extension) {
        Objects.requireNonNull(path, "Directory path must not be null");
        Objects.requireNonNull(extension, "File extension must not be null");
        return this.listFiles(path).stream()
                .filter(file -> file.getFileName().toLowerCase()
                        .endsWith("." + extension.name().toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public void makeDirectory(String dirPath) {
        Objects.requireNonNull(dirPath, "Directory path must not be null");
        try {
            Path path = Paths.get(dirPath);
            Files.createDirectories(path);
            log.info("Directory created successfully: {}", dirPath);
        } catch (IOException e) {
            log.error("Error creating directory: {}", dirPath, e);
            throw new RuntimeException("Failed to create directory", e);
        }
    }

    @Override
    @SneakyThrows
    public ByteArrayResource readFile(String filePath) {
        Objects.requireNonNull(filePath, "File path must not be null");
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }

        byte[] fileContent = Files.readAllBytes(path);
        log.info("File read successfully: {} ({} bytes)", filePath, fileContent.length);

        return new ByteArrayResource(fileContent);
    }

    @Override
    @SneakyThrows
    public String readFileAsUrl(String filePath) {
        Objects.requireNonNull(filePath, "File path must not be null");

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        // Convert to absolute path and return as file URL
        String fileUrl = path.toAbsolutePath().toUri().toString();
        log.info("File URL generated: {}", fileUrl);

        return fileUrl;
    }

    @Override
    @SneakyThrows
    public void writeFile(InputStream is, String filePath) {
        Objects.requireNonNull(is, "Input stream must not be null");
        Objects.requireNonNull(filePath, "File path must not be null");

        Path path = Paths.get(filePath);

        // Create parent directories if they don't exist
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        try (InputStream inputStream = is) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            log.info("File written successfully: {}", filePath);
        } catch (IOException e) {
            log.error("Error writing file: {}", filePath, e);
            throw new RuntimeException("Failed to write file", e);
        }
    }

    @Override
    @SneakyThrows
    public void deleteFile(String filePath) {
        Objects.requireNonNull(filePath, "File path must not be null");

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            log.warn("File does not exist: {}", filePath);
            return;
        }

        Files.delete(path);
        log.info("File deleted successfully: {}", filePath);
    }

    @Override
    @SneakyThrows
    public void appendFiles(String outputFilePath, List<String> filePaths) {
        Objects.requireNonNull(outputFilePath, "Output file path must not be null");
        Objects.requireNonNull(filePaths, "File paths list must not be null");

        if (filePaths.isEmpty()) {
            log.warn("No files to append");
            return;
        }

        Path outputPath = Paths.get(outputFilePath);

        // Create parent directories if they don't exist
        Path parentDir = outputPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        try (OutputStream os = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String filePath : filePaths) {
                Path inputPath = Paths.get(filePath);

                if (!Files.exists(inputPath)) {
                    log.warn("File does not exist, skipping: {}", filePath);
                    continue;
                }

                if (!Files.isRegularFile(inputPath)) {
                    log.warn("Path is not a regular file, skipping: {}", filePath);
                    continue;
                }

                try (InputStream is = Files.newInputStream(inputPath)) {
                    is.transferTo(os);
                    log.debug("Appended file: {}", filePath);
                }
            }
            log.info("Files appended successfully to: {}", outputFilePath);
        }
    }

    @Override
    @SneakyThrows
    public void moveFile(String sourceFilePath, String destinationFilePath) {
        Objects.requireNonNull(sourceFilePath, "Source file path must not be null");
        Objects.requireNonNull(destinationFilePath, "Destination file path must not be null");

        Path sourcePath = Paths.get(sourceFilePath);
        Path destinationPath = Paths.get(destinationFilePath);

        if (!Files.exists(sourcePath)) {
            throw new FileNotFoundException("Source file not found: " + sourceFilePath);
        }

        // Create parent directories if they don't exist
        Path parentDir = destinationPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File moved successfully from {} to {}", sourceFilePath, destinationFilePath);
    }

    @Override
    public void close() {
        log.debug("LocalStorageCommandImpl closed");
    }
}
