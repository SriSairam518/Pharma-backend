package com.sairam.pharma.repository;

// ================================================================
// AgencyRepository.java  —  REPOSITORY (Layer 3)
//
// WHAT IS A REPOSITORY?
// A repository is the layer that talks directly to the database.
// You write the METHOD SIGNATURE and Spring writes the SQL for you.
//
// ANALOGY: Think of JpaRepository as a pre-built library.
// It already has findAll(), findById(), save(), delete().
// You just add the specific queries YOUR app needs.
//
// HOW DOES SPRING GENERATE SQL FROM METHOD NAMES?
// Spring reads the method name and creates SQL automatically:
//
//   findByName("Sun Pharma")
//   → SELECT * FROM agencies WHERE name = 'Sun Pharma'
//
//   findByNameContainingIgnoreCase("pharma")
//   → SELECT * FROM agencies WHERE LOWER(name) LIKE '%pharma%'
//
//   existsByPhone("9876543210")
//   → SELECT COUNT(*) > 0 FROM agencies WHERE phone = '9876543210'
//
// This is called "Derived Query Methods" — no SQL needed!
// ================================================================

import com.sairam.pharma.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// JpaRepository<Agency, Long>:
//   Agency = which entity this manages
//   Long   = the type of the primary key (id is a Long)
@Repository
public interface AgencyRepository extends JpaRepository<Agency, Long> {

    // ---- Spring auto-generates SQL for these ----

    // Check if an agency with this name already exists
    // → SELECT COUNT(*) > 0 FROM agencies WHERE name = ?
    boolean existsByNameIgnoreCase(String name);

    // Same check but EXCLUDE a specific ID (used when editing)
    // → SELECT COUNT(*) > 0 FROM agencies WHERE name = ? AND id != ?
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Find by phone number
    // Optional<> means "might return one, might return nothing"
    Optional<Agency> findByPhone(String phone);

    // ---- Custom JPQL query for search ----
    // JPQL = Java Persistence Query Language — like SQL but uses class/field names
    // :query is a named parameter, filled in at runtime
    @Query("""
        SELECT a FROM Agency a
        WHERE LOWER(a.name)          LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(a.contactPerson) LIKE LOWER(CONCAT('%', :query, '%'))
           OR a.phone                LIKE CONCAT('%', :query, '%')
           OR LOWER(a.email)         LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY a.name ASC
        """)
    List<Agency> searchAgencies(@Param("query") String query);

    // Find all, ordered by name alphabetically
    List<Agency> findAllByOrderByNameAsc();
}
