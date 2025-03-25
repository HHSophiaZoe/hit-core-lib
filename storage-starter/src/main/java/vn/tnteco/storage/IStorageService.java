package vn.tnteco.storage;

import org.springframework.core.io.ByteArrayResource;
import vn.tnteco.storage.constant.FileExtensionEnum;
import vn.tnteco.storage.data.FileEntryDTO;

import java.io.InputStream;
import java.util.List;

public interface IStorageService extends AutoCloseable {

    List<FileEntryDTO> listFiles(String path);

    List<FileEntryDTO> listFiles(String path, FileExtensionEnum extension);

    void makeDirectory(String dirPath);

    ByteArrayResource readFile(String filePath);

    String readFileAsUrl(String filePath);

    void writeFile(InputStream is, String filePath);

    void deleteFile(String filePath);

    void appendFiles(String outputFilePath, List<String> filePaths);

    void moveFile(String sourceFilePath, String destinationFilePath);

}
