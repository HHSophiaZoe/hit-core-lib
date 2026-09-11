package com.hit.storage.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntryDTO {

    private String fileName;

    private String filePath;

    private Instant lastModifiedAt;

}
