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

@Repository
public interface AgencyRepository extends JpaRepository<Agency, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Agency> findByPhone(String phone);

    @Query("""
        SELECT a FROM Agency a
        WHERE LOWER(a.name)          LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(a.contactPerson) LIKE LOWER(CONCAT('%', :query, '%'))
           OR a.phone                LIKE CONCAT('%', :query, '%')
           OR LOWER(a.email)         LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY a.name ASC
        """)
    List<Agency> searchAgencies(@Param("query") String query);

    List<Agency> findAllByOrderByNameAsc();
}
