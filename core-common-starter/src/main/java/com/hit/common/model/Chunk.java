package com.hit.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Chunk<T> {
    private int size;
    /**
     * inclusive start index
     */
    private int from;
    /**
     * exclusive end index
     */
    private int to;
    private List<T> items;
}
