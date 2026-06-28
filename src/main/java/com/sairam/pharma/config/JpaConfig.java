package com.sairam.pharma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // this one annotation activates @CreatedDate and @LastModifiedDate
public class JpaConfig {
    // No code needed — the annotation does all the work
}
