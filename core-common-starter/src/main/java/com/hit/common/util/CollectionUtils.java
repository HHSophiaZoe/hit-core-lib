package com.hit.common.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class CollectionUtils {

    public static <K, V> Map<K, V> toMap(Iterable<V> values, Function<? super V, K> keyFunction) {
        return toMap(values.iterator(), keyFunction);
    }

    public static <K, V> Map<K, V> toMap(Iterator<V> values, Function<? super V, K> keyFunction) {
        Map<K, V> result = new LinkedHashMap<>();
        while (values.hasNext()) {
            V value = values.next();
            result.put(keyFunction.apply(value), value);
        }
        return new HashMap<>(result);
    }

    public static <K, V> Map<K, V> toMap(V[] values, Function<? super V, K> keyFunction) {
        Map<K, V> result = new LinkedHashMap<>();
        for (V value : values) {
            result.put(keyFunction.apply(value), value);
        }
        return new HashMap<>(result);
    }

}
