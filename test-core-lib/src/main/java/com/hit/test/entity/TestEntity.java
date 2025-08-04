package com.hit.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.context.annotation.Profile;

@Setter
@Getter
@Builder
@ToString
@Profile("test")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "test")
@Entity(name = "TestEntity")
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

}
