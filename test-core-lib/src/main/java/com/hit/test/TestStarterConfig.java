package com.hit.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
@EnableAutoConfiguration
@ComponentScan("com.hit.test")
public class TestStarterConfig {
}
