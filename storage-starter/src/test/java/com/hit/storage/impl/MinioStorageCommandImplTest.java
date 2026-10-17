package com.hit.storage.impl;

import com.hit.storage.config.StorageFileConfig;
import com.hit.storage.constant.FileExtensionEnum;
import com.hit.storage.constant.StorageEnum;
import com.hit.storage.data.FileEntryDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MinIO client 9 against an S3-compatible server (MinIO no longer publishes server images).
 */
@Testcontainers
class MinioStorageCommandImplTest {

    private static final String BUCKET = "hit-storage-it";

    // Started and closed by the @Testcontainers extension
    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> S3 = new GenericContainer<>("adobe/s3mock:5.2.3")
            .withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", BUCKET)
            .withExposedPorts(9090)
            .waitingFor(Wait.forHttp("/").forPort(9090).forStatusCodeMatching(code -> code < 500));

    private MinioStorageCommandImpl storage;

    @BeforeEach
    void connect() {
        storage = new MinioStorageCommandImpl(StorageFileConfig.builder()
                .storage(StorageEnum.MINIO)
                .endpoint("http://" + S3.getHost() + ":" + S3.getMappedPort(9090))
                .username("access")
                .password("secret-key")
                .minioBucketName(BUCKET)
                .build());
    }

    @AfterEach
    void close() throws Exception {
        storage.close();
    }

    @Test
    void writeReadListMoveAndDelete() {
        storage.writeFile(utf8("hello minio"), "reports/2026/a.txt");
        storage.writeFile(utf8("{}"), "reports/2026/b.json");

        assertThat(storage.readFile("reports/2026/a.txt").getByteArray())
                .isEqualTo("hello minio".getBytes(StandardCharsets.UTF_8));
        assertThat(storage.readFile("reports/2026/a.txt").getFilename()).isEqualTo("a.txt");
        assertThat(storage.listFiles("reports/2026").stream().map(FileEntryDTO::getFileName))
                .containsExactlyInAnyOrder("a.txt", "b.json");
        assertThat(storage.listFiles("reports/2026", FileExtensionEnum.TXT).stream().map(FileEntryDTO::getFilePath))
                .containsExactly("reports/2026/a.txt");

        storage.moveFile("reports/2026/a.txt", "archive/a.txt");
        assertThat(storage.listFiles("reports/2026").stream().map(FileEntryDTO::getFileName)).containsExactly("b.json");
        assertThat(storage.readFile("archive/a.txt").getByteArray()).isEqualTo("hello minio".getBytes(StandardCharsets.UTF_8));

        storage.deleteFile("archive/a.txt");
        assertThat(storage.listFiles("archive")).isEmpty();
    }

    @Test
    void streamOfUnknownLengthIsUploadedWhole() {
        byte[] content = "x".repeat(200_000).getBytes(StandardCharsets.UTF_8);

        storage.writeFile(new ByteArrayInputStream(content) {
            @Override
            public synchronized int available() {
                return 0; // a network stream does not know its length
            }
        }, "big/data.txt");

        assertThat(storage.readFile("big/data.txt").getByteArray()).isEqualTo(content);
    }

    @Test
    void presignedUrlDownloadsTheObject() throws Exception {
        storage.writeFile(utf8("signed"), "shared/s.txt");

        String url = storage.readFileAsUrl("shared/s.txt");

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("signed");
        }
    }

    private static ByteArrayInputStream utf8(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
