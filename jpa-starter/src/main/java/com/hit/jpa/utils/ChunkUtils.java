package com.hit.jpa.utils;

import com.hit.coremodel.model.Chunk;
import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class ChunkUtils {

    public static <T> List<Chunk<T>> toChunks(Collection<T> collection, int size) {
        if (CollectionUtils.isEmpty(collection) || size <= 0) return new ArrayList<>();

        if (collection instanceof List) {
            return toChunks((List<T>) collection, size);
        }

        return toChunks(new ArrayList<>(collection), size);
    }

    public static <T> List<Chunk<T>> toChunks(List<T> l, int size) {
        if (CollectionUtils.isEmpty(l) || size <= 0) return new ArrayList<>();
        else {
            int lsize = l.size();
            int n = (lsize % size == 0) ? lsize / size : lsize / size + 1;
            List<Chunk<T>> c = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int f = i * size;
                int t = Math.min(f + size, lsize);
                c.add(new Chunk<>(size, f, t, l.subList(f, t)));
            }
            return c;
        }
    }

}
