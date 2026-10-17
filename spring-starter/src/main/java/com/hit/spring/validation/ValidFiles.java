package com.hit.spring.validation;

import com.hit.spring.validation.validator.FilesValidator;
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
