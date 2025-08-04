package com.hit.test.repository;

import com.hit.test.entity.TestEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Profile("test")
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
}
