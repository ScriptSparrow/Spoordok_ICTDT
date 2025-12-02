package nhl.stenden.spoordock.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Spoordock API is running smoothly.");
    }

    @GetMapping("/ready")
    public ResponseEntity<String> readinessCheck() {   
        return ResponseEntity.ok("Spoordock API is ready to handle requests 8.");
    }

}
