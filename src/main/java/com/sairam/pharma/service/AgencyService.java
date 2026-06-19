package com.sairam.pharma.service;

// ================================================================
// AgencyService.java  —  SERVICE LAYER (Layer 2)
//
// WHAT IS THE SERVICE LAYER?
// This is where ALL business logic lives.
//
// WHAT IS "BUSINESS LOGIC"?
// Rules specific to your app:
//   - "You can't have two agencies with the same name"
//   - "When you delete an agency, check if it has unpaid bills first"
//   - "Convert the Entity into a DTO before sending to the controller"
//
// ANALOGY: The Controller is a waiter (takes orders, delivers food).
// The Service is the chef (does the actual work).
// The Repository is the fridge (just stores and retrieves things).
//
// @Transactional means:
//   "Run this method as one database transaction.
//    If anything fails, roll back ALL changes." — like Ctrl+Z for the DB.
// ================================================================

import com.sairam.pharma.dto.AgencyDto;
import com.sairam.pharma.entity.Agency;
import com.sairam.pharma.exception.DuplicateResourceException;
import com.sairam.pharma.exception.ResourceNotFoundException;
import com.sairam.pharma.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// @Service     → marks this as a Spring service bean (Spring manages its lifecycle)
// @RequiredArgsConstructor → Lombok generates a constructor for all 'final' fields
//                            This is how we do "Constructor Injection" — the modern way
//                            to inject dependencies (better than @Autowired on fields)
@Service
@RequiredArgsConstructor
public class AgencyService {

    // Spring injects this automatically because of @RequiredArgsConstructor
    private final AgencyRepository agencyRepository;

    // ---- GET ALL AGENCIES ----
    // @Transactional(readOnly = true) → optimizes read-only DB operations
    @Transactional(readOnly = true)
    public List<AgencyDto.Response> getAllAgencies() {
        return agencyRepository.findAllByOrderByNameAsc()
                .stream()
                // Convert each Agency entity to a Response DTO
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---- GET ONE AGENCY BY ID ----
    @Transactional(readOnly = true)
    public AgencyDto.Response getAgencyById(Long id) {
        Agency agency = findAgencyOrThrow(id);
        return toResponse(agency);
    }

    // ---- CREATE AGENCY ----
    @Transactional
    public AgencyDto.Response createAgency(AgencyDto.Request request) {
        // Business rule: agency name must be unique
        if (agencyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "Agency with name '" + request.getName() + "' already exists"
            );
        }

        // Build entity from DTO (never set ID — DB auto-generates it)
        Agency agency = Agency.builder()
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .gstin(request.getGstin() != null ? request.getGstin().trim().toUpperCase() : null)
                .build();

        // save() inserts into DB and returns the saved entity (with ID)
        Agency saved = agencyRepository.save(agency);
        return toResponse(saved);
    }

    // ---- UPDATE AGENCY ----
    @Transactional
    public AgencyDto.Response updateAgency(Long id, AgencyDto.Request request) {
        // 1. Make sure the agency exists first
        Agency agency = findAgencyOrThrow(id);

        // 2. Check name uniqueness — but EXCLUDE this agency's own ID
        //    (you should be allowed to "update" with the same name)
        if (agencyRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException(
                    "Another agency with name '" + request.getName() + "' already exists"
            );
        }

        // 3. Update the fields
        agency.setName(request.getName().trim());
        agency.setContactPerson(request.getContactPerson().trim());
        agency.setPhone(request.getPhone().trim());
        agency.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        agency.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        agency.setGstin(request.getGstin() != null ? request.getGstin().trim().toUpperCase() : null);

        // save() on an existing entity (one with an ID) performs UPDATE not INSERT
        Agency updated = agencyRepository.save(agency);
        return toResponse(updated);
    }

    // ---- DELETE AGENCY ----
    @Transactional
    public void deleteAgency(Long id) {
        // Check it exists before deleting
        Agency agency = findAgencyOrThrow(id);

        // TODO (Phase 3): Before deleting, check if this agency has unpaid bills.
        // If it does, throw a BusinessException("Cannot delete agency with unpaid bills")

        agencyRepository.delete(agency);
    }

    // ---- SEARCH AGENCIES ----
    @Transactional(readOnly = true)
    public List<AgencyDto.Response> searchAgencies(String query) {
        if (query == null || query.isBlank()) {
            return getAllAgencies();
        }
        return agencyRepository.searchAgencies(query.trim())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // PRIVATE HELPERS — not accessible outside this class
    // ================================================================

    // Finds an agency by ID or throws a clean 404 exception
    // This pattern (findXOrThrow) is a common service-layer convention
    private Agency findAgencyOrThrow(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Agency not found with id: " + id)
                );
    }

    // Converts an Agency entity → AgencyDto.Response
    // This is called a "mapper" method. Later we could use MapStruct library.
    private AgencyDto.Response toResponse(Agency agency) {
        return AgencyDto.Response.builder()
                .id(agency.getId())
                .name(agency.getName())
                .contactPerson(agency.getContactPerson())
                .phone(agency.getPhone())
                .email(agency.getEmail())
                .address(agency.getAddress())
                .gstin(agency.getGstin())
                .createdAt(agency.getCreatedAt())
                .updatedAt(agency.getUpdatedAt())
                .build();
    }
}