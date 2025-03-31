package com.hit.spring.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hit.spring.core.converter.DataConverter;
import com.hit.spring.service.cloudinary.CloudinaryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AppContext {

    @Setter
    @Getter
    private static ObjectMapper objectMapper;

    @Setter
    @Getter
    private static DataConverter dataConverter;


    @Autowired
    AppContext(@Qualifier("dataConverter") DataConverter dataConverter, ObjectMapper objectMapper) {
        setDataConverter(dataConverter);
        setObjectMapper(objectMapper);
    }

}
