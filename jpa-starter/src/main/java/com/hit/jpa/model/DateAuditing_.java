package com.hit.jpa.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DateAuditing.class)
public abstract class DateAuditing_ {

    public static volatile SingularAttribute<DateAuditing, LocalDateTime> createdDate;
    public static volatile SingularAttribute<DateAuditing, LocalDateTime> lastModifiedDate;

    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
}
