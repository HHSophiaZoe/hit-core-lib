package com.hit.storage.impl;

import com.hit.storage.constant.FileExtensionEnum;
import com.hit.storage.data.FileEntryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageCommandImplTest {

    private final LocalStorageCommandImpl storage = new LocalStorageCommandImpl();

    @Test
    void writeListAppendMoveDelete(@TempDir Path dir) throws Exception {
        storage.writeFile(utf8("A"), dir.resolve("in/a.txt").toString());
        storage.writeFile(utf8("B"), dir.resolve("in/b.txt").toString());
        storage.writeFile(utf8("{}"), dir.resolve("in/c.json").toString());

        assertThat(storage.listFiles(dir.resolve("in").toString(), FileExtensionEnum.TXT).stream().map(FileEntryDTO::getFileName))
                .containsExactlyInAnyOrder("a.txt", "b.txt");

        storage.appendFiles(dir.resolve("out/ab.txt").toString(),
                List.of(dir.resolve("in/a.txt").toString(), dir.resolve("in/missing.txt").toString(), dir.resolve("in/b.txt").toString()));
        assertThat(Files.readString(dir.resolve("out/ab.txt"))).isEqualTo("AB");

        storage.moveFile(dir.resolve("in/c.json").toString(), dir.resolve("archive/c.json").toString());
        assertThat(dir.resolve("archive/c.json")).exists();
        assertThat(dir.resolve("in/c.json")).doesNotExist();

        storage.deleteFile(dir.resolve("archive/c.json").toString());
        assertThat(dir.resolve("archive/c.json")).doesNotExist();
    }

    private static ByteArrayInputStream utf8(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
