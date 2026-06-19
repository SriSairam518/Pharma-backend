package com.sairam.pharma.config;

// ================================================================
// JpaConfig.java  —  JPA AUDITING CONFIGURATION
//
// WHAT IS THIS?
// Remember in Agency.java we used @CreatedDate and @LastModifiedDate?
// Those annotations only work if we ENABLE auditing here.
//
// Without this file:
//   createdAt → always null
//   updatedAt → always null
//
// With this file:
//   createdAt → automatically set to "now" when a record is created
//   updatedAt → automatically set to "now" every time a record is updated
//
// You never have to set these manually — Spring handles it.
// ================================================================

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // this one annotation activates @CreatedDate and @LastModifiedDate
public class JpaConfig {
    // No code needed — the annotation does all the work
}
