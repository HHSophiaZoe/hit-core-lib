package com.hit.spring.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hit.spring.context.AppContext;
import com.hit.spring.core.constant.CommonConstant;
import com.hit.spring.core.json.JsonMapper;
import com.hit.spring.core.wrapper.CachedBodyRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@UtilityClass
public class LoggingUtils {

    private final Map<String, String> REPLACE_CHARS_ERROR = Map.of("\u0000", CommonConstant.EMPTY_STRING);

    private static final String MASKED = "***MASKED***";

    private static final String EMPTY = "[Empty]";

    public static String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();
        return queryString == null ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
    }

    /*
     * Headers
     *
     * */
    public static String getHeaders(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headerMap.put(headerName, headerValue);
            }
        }
        return formatHeaders(headerMap);
    }

    public static String getHeaders(HttpServletResponse response) {
        // Get important response headers
        String[] importantHeaders = {"Content-Type", "Content-Length", "Cache-Control",
                "Set-Cookie", "Location", "ETag", "Last-Modified"};

        Map<String, String> headerMap = new HashMap<>();
        for (String headerName : importantHeaders) {
            String headerValue = response.getHeader(headerName);
            if (headerValue != null) {
                headerMap.put(headerName, headerValue);
            }
        }
        return formatHeaders(headerMap);
    }

    private static String formatHeaders(Map<String, String> headers) {
        if (headers.isEmpty()) {
            return "[No headers]";
        }
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, value) -> {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }


    /*
     * Body
     *
     * */
    public static String getRequestBody(CachedBodyRequestWrapper requestWrapper) {
        byte[] content = requestWrapper.getCachedBody();
        if (content.length > 0) {
            return LoggingUtils.formatRequestBody(sanitizeString(new String(content, StandardCharsets.UTF_8)), requestWrapper.getContentType());
        }
        return EMPTY;
    }

    public static String getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        byte[] content = responseWrapper.getContentAsByteArray();
        if (content.length > 0) {
            return sanitizeString(new String(content, StandardCharsets.UTF_8));
        }
        return EMPTY;
    }

    private static String formatRequestBody(String rawBody, String contentType) {
        if (!StringUtils.hasText(rawBody)) {
            return EMPTY;
        }

        if (contentType == null) {
            contentType = "[Unknown]";
        }
        contentType = contentType.toLowerCase();
        try {
            if (MediaType.APPLICATION_JSON_VALUE.equals(contentType) || MediaType.APPLICATION_JSON_UTF8_VALUE.equals(contentType)) {
                return formatJsonBody(rawBody);
            } else if (contentType.contains("multipart/form-data")) {
                return formatMultipartFormDataBody(rawBody, contentType);
            } else if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(contentType)) {
                return formatFormUrlencodedBody(rawBody, contentType);
            } else if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType) ||
                    contentType.contains("image/") || contentType.contains("video/") || contentType.contains("audio/")) {
                return formatBinaryBody(rawBody.length(), contentType);
            } else {
                return rawBody;
            }
        } catch (Exception ex) {
            log.warn("Failed to format httpRequest body for content type: {}", contentType, ex);
            return rawBody;
        }
    }

    private static String formatJsonBody(String body) throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getObjectMapper();
        JsonNode rootNode = objectMapper.readTree(body);
        maskNodeRecursively(rootNode, AppContext.getSecurityProperties().getSensitiveFieldLogRequest());
        return JsonMapper.encode(rootNode);
    }

    private static void maskNodeRecursively(JsonNode node, Set<String> sensitiveFields) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            sensitiveFields.forEach(field -> {
                if (objectNode.has(field)) {
                    objectNode.put(field, MASKED);
                }
            });

            objectNode.fields().forEachRemaining(entry -> {
                maskNodeRecursively(entry.getValue(), sensitiveFields);
            });

        } else if (node.isArray()) {
            node.forEach(arrayNode -> maskNodeRecursively(arrayNode, sensitiveFields));
        }
    }

    private String formatFormUrlencodedBody(String formBody, String contentType) {
        try {
            StringBuilder formatted = new StringBuilder("[FORM-URLENCODED] ");
            String[] pairs = formBody.split("&");

            for (int i = 0; i < pairs.length; i++) {
                if (i > 0) formatted.append(", ");

                String[] keyValue = pairs[i].split("=", 2);
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : CommonConstant.EMPTY_STRING;
                if (isSensitiveField(key)) {
                    value = MASKED;
                }
                formatted.append(key).append("=").append(value);
            }
            return formatted.toString();
        } catch (Exception ex) {
            log.warn("Failed to format httpRequest body for content type: {}", contentType, ex);
            return "[FORM-URLENCODED] " + formBody;
        }
    }

    private String formatMultipartFormDataBody(String multipartBody, String contentType) {
        try {
            // Extract boundary from content type
            String boundary = extractBoundary(contentType);
            if (boundary == null) {
                return "[MULTIPART-FORM] Unable to parse - no boundary found";
            }

            StringBuilder formatted = new StringBuilder("[MULTIPART-FORM] ");
            String[] parts = multipartBody.split("--" + boundary);
            int fieldCount = 0;
            int fileCount = 0;

            for (String part : parts) {
                if (part.trim().isEmpty() || part.equals("--")) continue;

                if (part.contains("Content-Disposition: form-data")) {
                    if (part.contains("filename=")) {
                        fileCount++;
                    } else {
                        String fieldName = extractFieldName(part);
                        if (fieldName != null) {
                            fieldCount++;
                            String fieldValue = extractFieldValue(part);

                            if (formatted.length() > "[MULTIPART-FORM] ".length()) {
                                formatted.append(", ");
                            }

                            if (isSensitiveField(fieldName)) {
                                formatted.append(fieldName).append("=").append(MASKED);
                            } else {
                                formatted.append(fieldName).append("=").append(
                                        fieldValue.length() > 100 ?
                                                fieldValue.substring(0, 100) + "..." : fieldValue
                                );
                            }
                        }
                    }
                }
            }

            if (fileCount > 0) {
                formatted.append(" [").append(fileCount).append(" file(s)]");
            }
            if (fieldCount == 0 && fileCount == 0) {
                formatted.append("No readable fields found");
            }

            return formatted.toString();
        } catch (Exception ex) {
            return "[MULTIPART-FORM] Unable to parse multipart data";
        }
    }

    private String formatBinaryBody(int length, String contentType) {
        return String.format("[BINARY] %s (%d bytes)", contentType, length);
    }

    private String sanitizeString(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        String sanitized = input;
        for (Map.Entry<String, String> entry : REPLACE_CHARS_ERROR.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }
        return sanitized;
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        String lowerFieldName = fieldName.toLowerCase();
        return AppContext.getSecurityProperties().getSensitiveFieldLogRequest().contains(lowerFieldName);
    }

    private String extractBoundary(String contentType) {
        if (contentType == null) return null;
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring(9);
            }
        }
        return null;
    }

    private String extractFieldName(String part) {
        try {
            int nameStart = part.indexOf("name=\"");
            if (nameStart == -1) return null;
            nameStart += 6;
            int nameEnd = part.indexOf("\"", nameStart);
            if (nameEnd == -1) return null;
            return part.substring(nameStart, nameEnd);
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractFieldValue(String part) {
        try {
            int valueStart = part.indexOf("\r\n\r\n");
            if (valueStart == -1) {
                valueStart = part.indexOf("\n\n");
            }
            if (valueStart == -1) return CommonConstant.EMPTY_STRING;

            valueStart += 4;
            return part.substring(valueStart).trim();
        } catch (Exception ex) {
            return CommonConstant.EMPTY_STRING;
        }
    }

}
