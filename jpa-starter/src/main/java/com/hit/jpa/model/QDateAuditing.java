package com.hit.jpa.model;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;

import javax.annotation.processing.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QDateAuditing is a Querydsl query type for DateAuditing
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QDateAuditing extends EntityPathBase<DateAuditing> {

    private static final long serialVersionUID = -1266626315L;

    public static final QDateAuditing dateAuditing = new QDateAuditing("dateAuditing");

    public final DateTimePath<java.time.LocalDateTime> createdDate = createDateTime("createdDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> lastModifiedDate = createDateTime("lastModifiedDate", java.time.LocalDateTime.class);

    public QDateAuditing(String variable) {
        super(DateAuditing.class, forVariable(variable));
    }

    public QDateAuditing(Path<? extends DateAuditing> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDateAuditing(PathMetadata metadata) {
        super(DateAuditing.class, metadata);
    }

}

