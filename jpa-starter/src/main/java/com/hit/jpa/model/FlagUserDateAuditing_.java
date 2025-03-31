package com.hit.jpa.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(FlagUserDateAuditing.class)
public abstract class FlagUserDateAuditing_ extends UserDateAuditing_ {

    public static volatile SingularAttribute<FlagUserDateAuditing, Boolean> deleteFlag;

    public static final String DELETE_FLAG = "deleteFlag";

}
