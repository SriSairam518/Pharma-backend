package com.sairam.pharma.service;

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

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;

    @Transactional(readOnly = true)
    public List<AgencyDto.Response> getAllAgencies() {
        return agencyRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgencyDto.Response getAgencyById(Long id) {
        Agency agency = findAgencyOrThrow(id);
        return toResponse(agency);
    }

    @Transactional
    public AgencyDto.Response createAgency(AgencyDto.Request request) {
        if (agencyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "Agency with name '" + request.getName() + "' already exists"
            );
        }

        Agency agency = Agency.builder()
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .gstin(request.getGstin() != null ? request.getGstin().trim().toUpperCase() : null)
                .build();

        Agency saved = agencyRepository.save(agency);
        return toResponse(saved);
    }

    @Transactional
    public AgencyDto.Response updateAgency(Long id, AgencyDto.Request request) {
        Agency agency = findAgencyOrThrow(id);

        if (agencyRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException(
                    "Another agency with name '" + request.getName() + "' already exists"
            );
        }

        agency.setName(request.getName().trim());
        agency.setContactPerson(request.getContactPerson().trim());
        agency.setPhone(request.getPhone().trim());
        agency.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        agency.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        agency.setGstin(request.getGstin() != null ? request.getGstin().trim().toUpperCase() : null);

        Agency updated = agencyRepository.save(agency);
        return toResponse(updated);
    }

    @Transactional
    public void deleteAgency(Long id) {
        Agency agency = findAgencyOrThrow(id);

        agencyRepository.delete(agency);
    }

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

    private Agency findAgencyOrThrow(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Agency not found with id: " + id)
                );
    }

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