package com.hit.jpa.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@Setter
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class FlagUserDateAuditing extends UserDateAuditing {

    @Column(nullable = false)
    protected Boolean deleteFlag = Boolean.FALSE;

}
