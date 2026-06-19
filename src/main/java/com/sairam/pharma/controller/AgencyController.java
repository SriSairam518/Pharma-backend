package com.sairam.pharma.controller;

// ================================================================
// AgencyController.java  —  CONTROLLER (Layer 1)
//
// WHAT IS A CONTROLLER?
// The controller is the front door of your backend.
// It receives HTTP requests from React and sends back responses.
//
// EVERY method here = ONE API endpoint.
//
// HTTP METHODS explained simply:
//   GET    → "give me data"    (reading)
//   POST   → "create new data" (creating)
//   PUT    → "replace data"    (updating)
//   DELETE → "remove data"     (deleting)
//
// @RequestMapping("/api/agencies") means EVERY endpoint in
// this class starts with /api/agencies
// ================================================================

import com.sairam.pharma.dto.AgencyDto;
import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {
    private final AgencyService agencyService;

    // ----------------------------------------------------------------
    // GET /api/agencies
    // GET /api/agencies?search=sun
    // Returns all agencies (or filtered if search param given)
    // ---------------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyDto.Response>>> getAllAgencies(@RequestParam(required = false) String search){
        List<AgencyDto.Response> agencies = (search != null && !search.isBlank())
                ? agencyService.searchAgencies(search)
                : agencyService.getAllAgencies();

        return ResponseEntity.ok(ApiResponse.success("Agencies fetched successfully", agencies));
    }

    // ----------------------------------------------------------------
    // GET /api/agencies/5
    // Returns one agency by ID
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgencyDto.Response>> getAgencyById(@PathVariable Long id){
        AgencyDto.Response agency = agencyService.getAgencyById(id);
        return ResponseEntity.ok(ApiResponse.success("Agency fetched successfully", agency));
    }

    // ----------------------------------------------------------------
    // POST /api/agencies
    // Creates a new agency
    // Body: { "name": "...", "phone": "...", ... }
    // ----------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ApiResponse<AgencyDto.Response>> createAgency(@Valid @RequestBody AgencyDto.Request request){
        AgencyDto.Response created = agencyService.createAgency(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Agency created successfully", created));
    }

    // ----------------------------------------------------------------
    // PUT /api/agencies/5
    // Updates an existing agency
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgencyDto.Response>> updateAgency(@PathVariable Long id, @Valid @RequestBody AgencyDto.Request request){
        AgencyDto.Response updated  = agencyService.updateAgency(id, request);
        return ResponseEntity.ok(ApiResponse.success("Agency updated successfully", updated));
    }

    // ----------------------------------------------------------------
    // DELETE /api/agencies/5
    // Deletes an agency
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgency(@PathVariable Long id){
        agencyService.deleteAgency(id);
        return ResponseEntity.ok(
                ApiResponse.success("Agency deleted successfully")
        );
    }

}
