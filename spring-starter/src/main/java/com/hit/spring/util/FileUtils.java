package com.hit.spring.util;

import com.hit.spring.core.constant.CommonConstant;
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

    /**
     * @param folderPath Location of folder, example: "upload/xxx/xxx"
     */
    public static boolean isFolderExists(String folderPath) {
        Path path = Paths.get(folderPath);
        return Files.exists(path);
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

    /**
     * @param pathFile Location to get file, example: "upload/xxx/xxx/fileName.xxx"
     */
    public static File getFile(String pathFile) {
        Path path = Paths.get(pathFile);
        return path.toFile();
    }

    /**
     * @param pathFile Location to get file, example: "upload/xxx/xxx/fileName.xxx"
     */
    public static byte[] getBytesFile(String pathFile) throws IOException {
        Path path = Paths.get(pathFile);
        return Files.readAllBytes(path);
    }

    public List<Path> createDirectories(String... paths) throws IOException {
        List<Path> pathList = new ArrayList<>();
        for (String pathFolder : paths) {
            pathList.add(createDirectories(pathFolder));
        }
        return pathList;
    }

    public Path createDirectories(String path) throws IOException {
        Path pathResult = Paths.get(validPath(path));
        if (!Files.exists(pathResult)) {
            Files.createDirectories(pathResult);
        }
        return pathResult;
    }

    /**
     * @param inputStream File data to save
     * @param uploadPath  Location to save file, example: "upload/xxx/xxx"
     * @param filename    File name to save, example: "example.txt"
     */
    public static Path saveFile(InputStream inputStream, String uploadPath, String filename) throws IOException {
        Path path = createDirectories(uploadPath);
        return saveFile(inputStream, path, filename);
    }

    /**
     * @param inputStream File data to save
     * @param uploadPath  Location to save file
     * @param filename    File name to save, example: "example.txt"
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
     * Zip a folder or multiple folder
     * @param sourceFolderPaths Array of folder paths to zip, example: ["upload/xxx/xxx", "upload/xxx/xxx"]
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
     * @param filePaths Array of file paths to zip, example: ["upload/xxx/fileName.xxx", "upload/xxx/fileName.xxx"]
     */
    public static byte[] zipFileByPath(String... filePaths) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        try {
            for (String pathFile : filePaths) {
                File file = getFile(pathFile);
                addToZip(file.getParentFile().getPath(), zipOutputStream, file);
            }
            zipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioe) {
            throw new IOException("Something went wrong, file cannot be compressed", ioe);
        }
    }

    /**
     * Tạo và thêm các entry là file or folder vào zip
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
