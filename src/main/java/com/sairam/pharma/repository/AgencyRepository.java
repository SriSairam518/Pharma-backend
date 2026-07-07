package com.sairam.pharma.repository;

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
