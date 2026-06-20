package com.rockstarpainkiller.ampbreaker.controller;

import com.rockstarpainkiller.ampbreaker.model.SurvivalGuide;
import com.rockstarpainkiller.ampbreaker.model.SurvivalRequest;
import com.rockstarpainkiller.ampbreaker.service.AdhrustadevathaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survival")
@CrossOrigin(origins = "*")
public class SurvivalController {

    private static final Logger log = LoggerFactory.getLogger(SurvivalController.class);

    @Autowired
    private AdhrustadevathaService adhrustadevathaService;

    @PostMapping("/generate")
    public ResponseEntity<SurvivalGuide> generateGuide(
            @RequestBody SurvivalRequest request,
            @RequestHeader(value = "X-Provider", required = false) String provider,
            @RequestHeader(value = "X-API-Key", required = false) String customApiKey,
            @RequestHeader(value = "X-Model", required = false) String customModel,
            @RequestHeader(value = "X-Language", required = false) String language) {
        log.info("Received request to generate survival guide. Scenario: '{}', Location: '{}', Provider: {}, CustomKey: {}, Model: {}, Language: {}", 
                request.getScenario(), request.getLocation(), provider, (customApiKey != null && !customApiKey.isEmpty()) ? "PRESENT" : "ABSENT", customModel, language);
        
        try {
            SurvivalGuide guide = adhrustadevathaService.generateSurvivalGuide(request, provider, customApiKey, customModel, language);
            return ResponseEntity.ok(guide);
        } catch (Exception e) {
            log.error("Failed to generate survival guide", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
