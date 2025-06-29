package com.hit.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface BaseJPARepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>, QuerydslPredicateExecutor<T> {

    List<T> findAllByIdIn(Collection<ID> ids);

}
