package com.sairam.pharma.entity;

// ================================================================
// Agency.java  —  ENTITY (Layer 4)
//
// WHAT IS AN ENTITY?
// An entity is a Java class that maps directly to a database table.
// Every field = one column. Every object of this class = one row.
//
// ANALOGY: Think of this class as a "blueprint" for one row
// in your agencies table. JPA reads this class and automatically
// creates/manages the SQL table for you.
//
// KEY ANNOTATIONS EXPLAINED:
//   @Entity       → tells Spring "this class is a database table"
//   @Table        → lets you set the exact table name
//   @Id           → marks the primary key field
//   @GeneratedValue → auto-increments the ID (1, 2, 3...)
//   @Column       → customizes column properties (name, nullable, length)
//   @CreatedDate  → auto-fills with the current timestamp on insert
//   @UpdatedDate  → auto-fills with the current timestamp on update
// ================================================================

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// @Data         → Lombok generates getters, setters, toString, equals, hashCode
// @Builder      → lets you use Agency.builder().name("...").build() pattern
// @NoArgsConstructor → generates empty constructor (required by JPA)
// @AllArgsConstructor → generates constructor with all fields (used by @Builder)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "agencies",
        // Unique constraint: no two agencies can have the same name
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agency_name",
                columnNames = "name"
        )
)

@EntityListeners(AuditingEntityListener.class)
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment: 1, 2, 3...
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 15)
    private String gstin;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
