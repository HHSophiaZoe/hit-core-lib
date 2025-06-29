package com.hit.storage.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class FileUtils {

    public static final String FORWARD_SLASH = "/";

    public static String getFilename(String path) {
        return FilenameUtils.getName(path);
    }

    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static String removeExtension(String filename) {
        return FilenameUtils.removeExtension(filename);
    }

    public static String joinPaths(String... paths) {
        StringBuilder builder = new StringBuilder();
        int pathsLength = paths.length;
        for (int i = 0; i < pathsLength; i++) {
            String path = paths[i];
            if (path == null) path = StringUtils.EMPTY;

            if (i != 0 && path.startsWith(FORWARD_SLASH)) {
                path = path.substring(1);
            }

            builder.append(path);
            if (i < pathsLength - 1 && !path.endsWith(FORWARD_SLASH)) {
                builder.append(FORWARD_SLASH);
            }
        }
        return builder.toString();
    }

}
