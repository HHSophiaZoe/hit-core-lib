package com.hit.spring.utils;

import com.hit.spring.core.constants.CommonConstant;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@UtilityClass
public class FileUtils {

    public static String validPath(String path) {
        if (path.startsWith(CommonConstant.CommonSymbol.FORWARD_SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    public static String getFilename(String path) {
        return FilenameUtils.getName(path);
    }

    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static String removeExtension(String filename) {
        return FilenameUtils.removeExtension(filename);
    }

    public void createDirectories(String... paths) throws IOException {
        for (String pathFolder : paths) {
            Path path = Paths.get(validPath(pathFolder));
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        }
    }

    public Path createDirectories(String path) throws IOException {
        Path pathResult = Paths.get(validPath(path));
        if (!Files.exists(pathResult)) {
            Files.createDirectories(pathResult);
        }
        return pathResult;
    }

    /**
     * Zip a folder or multiple folder
     *
     * @param sourceFolderPaths Mảng các đường dẫn của folder cần zip (trong phạm vi folder resources),
     *                          Example: ["upload/xxx/xxx", "upload/xxx/xxx"]
     * @return byte[]
     */
    public static byte[] zipFolder(String... sourceFolderPaths) throws IOException {
        List<File> fileList = new ArrayList<>();
        for (String pathFolder : sourceFolderPaths) {
            File sourceFolder = new File(pathFolder);
            fileList.add(sourceFolder);
        }

        if (CollectionUtils.isNotEmpty(fileList)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
            try {
                for (File fileFolder : fileList) {
                    addToZip(fileFolder.getParentFile().getPath(), zipOutputStream, fileFolder);
                }
                zipOutputStream.close();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException ioe) {
                throw new IOException("Something went wrong, file cannot be compressed", ioe);
            }
        }
        return new byte[0];
    }

    /**
     * Zip a file or multiple file
     *
     * @param filePaths Mảng các đường dẫn của file cần zip (trong phạm vi folder resources),
     *                  Example: ["upload/xxx/fileName.xxx", "upload/xxx/fileName.xxx"]
     * @return byte[]
     */
    public static byte[] zipFileByPath(String... filePaths) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        try {
            for (String pathFile : filePaths) {
                File file = getFileByPath(pathFile);
                addToZip(file.getParentFile().getPath(), zipOutputStream, file);
            }
            zipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioe) {
            throw new IOException("Something went wrong, file cannot be compressed", ioe);
        }
    }

    /**
     * Save file upload to Resources
     *
     * @param inputStream File cần lưu
     * @param uploadPath  Vị trí cần lưu (trong phạm vi folder resources), example: "upload/xxx/xxx"
     * @param filename Tên file để lưu, example: "example.txt"
     * @return String
     */
    public static Path saveFile(InputStream inputStream, String uploadPath, String filename) throws IOException {
        Path path = createDirectories(uploadPath);
        return saveFile(inputStream, path, filename);
    }

    /**
     * Save file upload to Resources
     *
     * @param inputStream File cần lưu
     * @param uploadPath  Vị trí cần lưu
     * @param filename Tên file để lưu, example: "example.txt"
     * @return String
     */
    public static Path saveFile(InputStream inputStream, Path uploadPath, String filename) throws IOException {
        try (InputStream is = inputStream) {
            Path filePath = uploadPath.resolve(filename);
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath;
        } catch (IOException ioe) {
            throw new IOException("Could not save file: " + filename);
        }
    }

    /**
     * Check folder in Resources
     *
     * @param folderPath Đường dẫn của folder (trong phạm vi folder resources), example: "upload/xxx/xxx"
     * @return boolean
     */
    public static boolean isFolderExists(String folderPath) {
        Path path = Paths.get(folderPath);
        return Files.exists(path);
    }

    /**
     * Lấy ra file ở trong thư mục resources theo đường dẫn
     *
     * @param pathFile Đường dẫn file cần lấy (trong phạm vi folder resources), example: "upload/xxx/fileName.xxx"
     * @return File
     */
    public static File getFileByPath(String pathFile) {
        Path path = Paths.get(pathFile);
        return path.toFile();
    }

    /**
     * Lấy ra dữ liệu file ở trong thư mục resources theo đường dẫn
     *
     * @param pathFile - đường dẫn file cần lấy (trong phạm vi folder resources), example: "upload/xxx/fileName.xxx"
     * @return byte[]
     */
    @SneakyThrows
    public static byte[] getBytesFileByPath(String pathFile) {
        Path path = Paths.get(pathFile);
        return Files.readAllBytes(path);
    }

    /**
     * Tạo và thêm các entry là file hoặc folder vào zip
     *
     * @param basePath Base path file
     * @param toAdd    File đưa vào entry
     */
    private static void addToZip(String basePath, ZipOutputStream zos, File toAdd) throws IOException {
        if (toAdd.isDirectory()) {
            // Nếu file là folder thì lấy tất cả các file và tiếp tục add
            File[] files = toAdd.listFiles();
            if (files != null) {
                for (File file : files) {
                    addToZip(basePath, zos, file);
                }
            }
        } else {
            // Bỏ phần basePath để lấy name file
            String name = toAdd.getPath().substring(basePath.length() + 1);
            ZipEntry entry = new ZipEntry(name);
            zos.putNextEntry(entry);
            // Copy file vào entry trong zip output vừa put
            Files.copy(Paths.get(toAdd.getPath()), zos);
            zos.closeEntry();
        }
    }
}
