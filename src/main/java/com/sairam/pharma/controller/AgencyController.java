package com.sairam.pharma.controller;

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyDto.Response>>> getAllAgencies(@RequestParam(required = false) String search){
        List<AgencyDto.Response> agencies = (search != null && !search.isBlank())
                ? agencyService.searchAgencies(search)
                : agencyService.getAllAgencies();

        return ResponseEntity.ok(ApiResponse.success("Agencies fetched successfully", agencies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgencyDto.Response>> getAgencyById(@PathVariable Long id){
        AgencyDto.Response agency = agencyService.getAgencyById(id);
        return ResponseEntity.ok(ApiResponse.success("Agency fetched successfully", agency));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgencyDto.Response>> createAgency(@Valid @RequestBody AgencyDto.Request request){
        AgencyDto.Response created = agencyService.createAgency(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Agency created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgencyDto.Response>> updateAgency(@PathVariable Long id, @Valid @RequestBody AgencyDto.Request request){
        AgencyDto.Response updated  = agencyService.updateAgency(id, request);
        return ResponseEntity.ok(ApiResponse.success("Agency updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgency(@PathVariable Long id){
        agencyService.deleteAgency(id);
        return ResponseEntity.ok(
                ApiResponse.success("Agency deleted successfully")
        );
    }

}
