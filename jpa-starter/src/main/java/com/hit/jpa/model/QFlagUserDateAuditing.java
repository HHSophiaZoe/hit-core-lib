package com.hit.jpa.model;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;

import javax.annotation.processing.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QFlagUserDateAuditing is a Querydsl query type for FlagUserDateAuditing
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QFlagUserDateAuditing extends EntityPathBase<FlagUserDateAuditing> {

    private static final long serialVersionUID = 1261413356L;

    public static final QFlagUserDateAuditing flagUserDateAuditing = new QFlagUserDateAuditing("flagUserDateAuditing");

    public final QUserDateAuditing _super = new QUserDateAuditing(this);

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final BooleanPath deleteFlag = createBoolean("deleteFlag");

    //inherited
    public final StringPath lastModifiedBy = _super.lastModifiedBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifiedDate = _super.lastModifiedDate;

    public QFlagUserDateAuditing(String variable) {
        super(FlagUserDateAuditing.class, forVariable(variable));
    }

    public QFlagUserDateAuditing(Path<? extends FlagUserDateAuditing> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFlagUserDateAuditing(PathMetadata metadata) {
        super(FlagUserDateAuditing.class, metadata);
    }

}

