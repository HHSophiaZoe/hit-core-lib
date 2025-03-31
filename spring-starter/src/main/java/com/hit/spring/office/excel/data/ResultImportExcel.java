package com.hit.spring.office.excel.data;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultImportExcel<T> {

    private List<T> data;

    private byte[] fileError;

    public boolean isError() {
        return fileError != null && fileError.length != 0;
    }
}
