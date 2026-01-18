package nhl.stenden.spoordock.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nhl.stenden.spoordock.controllers.dtos.StatisticsDTO;
import nhl.stenden.spoordock.services.StatisticsService;

/**
 * REST controller voor het Gegevens panel statistieken.
 */
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Gegevens panel statistieken")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    @Operation(summary = "Haal alle statistieken op voor het Gegevens panel")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO stats = statisticsService.calculateStatistics();
        return ResponseEntity.ok(stats);
    }
}
