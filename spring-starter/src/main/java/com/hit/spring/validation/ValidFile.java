package com.hit.spring.validation;

import com.hit.spring.validation.validator.FileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Constraint(validatedBy = {FileValidator.class})
public @interface ValidFile {

    String message() default "Invalid file";

    FileExtensionEnum[] extensions() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
