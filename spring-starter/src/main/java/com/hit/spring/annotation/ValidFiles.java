package com.hit.spring.annotation;

import com.hit.spring.annotation.validator.FilesValidator;
import com.hit.spring.core.constant.enums.FileExtensionEnum;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Constraint(validatedBy = {FilesValidator.class})
public @interface ValidFiles {

    String message() default "Invalid files";

    FileExtensionEnum[] extensions() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
