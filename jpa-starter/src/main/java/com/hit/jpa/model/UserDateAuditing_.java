package com.hit.jpa.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserDateAuditing.class)
public abstract class UserDateAuditing_ extends DateAuditing_ {

    public static volatile SingularAttribute<UserDateAuditing, String> createdBy;
    public static volatile SingularAttribute<UserDateAuditing, String> lastModifiedBy;

    public static final String CREATED_BY = "createdBy";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";
}
