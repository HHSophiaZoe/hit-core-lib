package com.hit.rest.base;

import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationResponse;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.hit.spring.annotation.PaginationParameter;
import com.hit.spring.core.data.response.CommonResponse;
import com.hit.spring.core.factory.GeneralResponse;
import com.hit.spring.core.factory.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
public abstract class BaseController<RS, ID> {

    protected final IService<RS, ID> service;

    protected BaseController(IService<RS, ID> service) {
        this.service = service;
    }

    @Operation(summary = "Get detailed resource information by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return resource detail",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
    })
    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponse<RS>> detail(@PathVariable("id") ID id) {
        return ResponseFactory.success(service.getById(id));
    }

    @Operation(summary = "Select resource information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return list resource",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
    })
    @GetMapping("/select")
    public ResponseEntity<GeneralResponse<PaginationResponse<RS>>>
    select(@PaginationParameter PaginationRequest request) {
        return ResponseFactory.success(service.select(request));
    }

    @Operation(summary = "Search resource information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return list resource",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
    })
    @PostMapping("/search")
    public ResponseEntity<GeneralResponse<PaginationResponse<RS>>>
    search(@RequestBody PaginationSearchRequest searchRequest) {
        return ResponseFactory.success(service.search(searchRequest));
    }

    @Operation(summary = "Delete resource by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return common response",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponse<CommonResponse>> delete(@PathVariable("id") ID id) {
        return ResponseFactory.success(service.deleteById(id));
    }

    @Operation(summary = "Delete resource by list id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return common response",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GeneralResponse.class))
            }),
    })
    @DeleteMapping
    ResponseEntity<GeneralResponse<CommonResponse>> delete(@RequestBody Set<ID> ids) {
        return ResponseFactory.success(service.deleteByIds(ids));
    }
}