package com.hit.spring.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hit.spring.context.AppContext;
import com.hit.spring.core.exception.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonMapper {

    private static ObjectMapper objectMapper;

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = AppContext.getObjectMapper();
        }
        return objectMapper;
    }

    /**
     * Encode a Object to JSON using the underlying Jackson mapper.
     *
     * @param obj a Object
     * @return a String containing the JSON representation of the given Object.
     * @throws BusinessException if a property cannot be encoded.
     */
    public static String encode(Object obj) throws BusinessException {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("encode ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to encode as JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Encode a Object to JSON using the underlying Jackson mapper.
     *
     * @param obj a Object
     * @return a byte[] containing the JSON representation of the given Object.
     * @throws BusinessException if a property cannot be encoded.
     */
    public static byte[] encodeAsByte(Object obj) {
        try {
            return getObjectMapper().writeValueAsBytes(obj);
        } catch (Exception e) {
            log.error("encode ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to encode as byte: " + e.getMessage());
        }
    }

    /**
     * Decode a given JSON string to a Object of the given class type.
     *
     * @param str   the JSON string.
     * @param clazz the class to map to.
     * @param <T>   the generic type.
     * @return an instance of T
     * @throws BusinessException when there is a parsing or invalid mapping.
     */
    public static <T> T decodeValue(String str, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(str, clazz);
        } catch (JsonProcessingException e) {
            log.error("decodeValue ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to decode: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a given JSON string to a Object of the given type.
     *
     * @param str  the JSON string.
     * @param type the type to map to.
     * @param <T>  the generic type.
     * @return an instance of T
     * @throws BusinessException when there is a parsing or invalid mapping.
     */
    public static <T> T decodeValue(String str, TypeReference<T> type) {
        try {
            return getObjectMapper().readValue(str, type);
        } catch (Exception e) {
            log.error("decodeValue ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to decode: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a given JSON byte to a Object of the given type.
     *
     * @param src  the JSON byte.
     * @param type the type to map to.
     * @param <T>  the generic type.
     * @return an instance of T
     * @throws BusinessException when there is a parsing or invalid mapping.
     */
    public static <T> T decodeValue(byte[] src, Class<T> type) {
        try {
            return getObjectMapper().readValue(src, type);
        } catch (Exception e) {
            log.error("decodeValue ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to decode: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a given JSON byte to a Object of the given type.
     *
     * @param src  the JSON byte.
     * @param type the type to map to.
     * @param <T>  the generic type.
     * @return an instance of T
     * @throws BusinessException when there is a parsing or invalid mapping.
     */
    public static <T> T decodeValue(byte[] src, TypeReference<T> type) {
        try {
            return getObjectMapper().readValue(src, type);
        } catch (Exception e) {
            log.error("decodeValue ERROR {}", e.getMessage(), e);
            throw new BusinessException("Failed to decode: " + e.getMessage(), e);
        }
    }

}