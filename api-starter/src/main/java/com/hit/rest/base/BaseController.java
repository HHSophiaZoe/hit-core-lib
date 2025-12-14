package com.hit.rest.base;

import com.hit.common.model.pagination.PageResModel;
import com.hit.common.model.pagination.PageableReqModel;
import com.hit.common.model.pagination.PageableSearchReqModel;
import com.hit.spring.annotation.PaginationParameter;
import com.hit.spring.core.mapper.ResponseMapper;
import com.hit.spring.core.factory.GeneralResponse;
import com.hit.spring.core.factory.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
public abstract class BaseController<M, ID, RES, Map extends ResponseMapper<M, RES>> {

    @Setter(onMethod_ = {@Autowired})
    protected Map mapper;

    protected final IService<M, ID> service;

    protected BaseController(IService<M, ID> service) {
        this.service = service;
    }

    @Operation(summary = "Get resource information by id")
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
    public ResponseEntity<GeneralResponse<RES>> getById(@PathVariable("id") ID id) {
        return ResponseFactory.success(mapper.toResponse(service.getById(id)));
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
    public ResponseEntity<GeneralResponse<PageResModel<RES>>>
    select(@PaginationParameter PageableReqModel request) {
        return ResponseFactory.success(service.select(request).map(mapper::toResponse));
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
    public ResponseEntity<GeneralResponse<PageResModel<RES>>>
    search(@RequestBody PageableSearchReqModel searchRequest) {
        return ResponseFactory.success(service.search(searchRequest).map(mapper::toResponse));
    }

    @Operation(summary = "Delete resource by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return object response",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Object.class))
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
    public ResponseEntity<GeneralResponse<Object>> delete(@PathVariable("id") ID id) {
        return ResponseFactory.success(service.deleteById(id));
    }

    @Operation(summary = "Delete resource by list id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return object response",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Object.class))
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
    ResponseEntity<GeneralResponse<Object>> delete(@RequestBody Set<ID> ids) {
        return ResponseFactory.success(service.deleteByIds(ids));
    }
}