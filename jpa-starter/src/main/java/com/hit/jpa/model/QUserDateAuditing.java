package com.hit.jpa.model;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;

import javax.annotation.processing.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QUserDateAuditing is a Querydsl query type for UserDateAuditing
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QUserDateAuditing extends EntityPathBase<UserDateAuditing> {

    private static final long serialVersionUID = 1120417888L;

    public static final QUserDateAuditing userDateAuditing = new QUserDateAuditing("userDateAuditing");

    public final QDateAuditing _super = new QDateAuditing(this);

    public final StringPath createdBy = createString("createdBy");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath lastModifiedBy = createString("lastModifiedBy");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifiedDate = _super.lastModifiedDate;

    public QUserDateAuditing(String variable) {
        super(UserDateAuditing.class, forVariable(variable));
    }

    public QUserDateAuditing(Path<? extends UserDateAuditing> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserDateAuditing(PathMetadata metadata) {
        super(UserDateAuditing.class, metadata);
    }

}

