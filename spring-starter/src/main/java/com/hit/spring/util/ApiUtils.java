package com.hit.spring.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hit.spring.core.exception.*;
import com.hit.spring.core.extension.SupplierThrowable;
import com.hit.spring.core.factory.GeneralResponse;
import com.hit.spring.core.factory.InternalResponse;
import com.hit.spring.core.json.JsonMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@UtilityClass
public class ApiUtils {

    public static <T> InternalResponse<GeneralResponse<T>> handleResponseInternal(SupplierThrowable<GeneralResponse<T>> supplierCallAPI) {
        try {
            return new InternalResponse<GeneralResponse<T>>()
                    .setHttpStatus(HttpStatus.OK)
                    .setResponse(supplierCallAPI.get());
        } catch (ResourceAccessException ex) {
            log.error("handleResponseInternal TIMEOUT", ex);
            throw new BaseResponseException(ResponseStatusCodeEnum.INTERNAL_GENERAL_SERVER_ERROR);
        } catch (HttpStatusCodeException ex) {
            TypeReference<GeneralResponse<T>> responseType = new TypeReference<>() {
            };
            try {
                return new InternalResponse<GeneralResponse<T>>()
                        .setHttpStatus(HttpStatus.valueOf(ex.getStatusCode().value()))
                        .setResponse(JsonMapper.getObjectMapper().readValue(ex.getResponseBodyAsString(), responseType));
            } catch (JsonProcessingException e) {
                throw new HttpResponseInvalidException(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("handleResponseInternal error", e);
            throw new BusinessException(e);
        }
    }

    public static <T> T handleResponseInternalReturnData(SupplierThrowable<GeneralResponse<T>> supplierCallAPI) {
        try {
            return supplierCallAPI.get().getData();
        } catch (ResourceAccessException ex) {
            log.error("handleResponseInternalReturnData TIMEOUT", ex);
            throw new BaseResponseException(ResponseStatusCodeEnum.INTERNAL_GENERAL_SERVER_ERROR);
        } catch (HttpStatusCodeException ex) {
            TypeReference<GeneralResponse<T>> responseType = new TypeReference<>() {
            };
            try {
                GeneralResponse<T> generalResponse = JsonMapper.getObjectMapper().readValue(ex.getResponseBodyAsString(), responseType);
                throw new ServiceException(generalResponse.getStatus().getCode(), generalResponse.getStatus().getMessage());
            } catch (JsonProcessingException e) {
                throw new HttpResponseInvalidException(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("handleResponseInternalReturnData error", e);
            throw new BusinessException(e);
        }
    }

}
