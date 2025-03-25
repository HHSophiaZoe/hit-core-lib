package vn.tnteco.storage.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FileExtensionEnum {

    TXT("txt"),
    DOC("doc"),
    DOCX("docx"),
    PDF("pdf"),
    XLSX("xlsx"),
    XLS("xls"),
    PNG("png"),
    JPG("jpg"),
    JPEG("jpeg"),
    GIF("gif"),
    WEBP("webp"),
    MP4("mp4"),
    WEBM("webm");

    private final String extension;

    public String value() {
        return extension;
    }
}
