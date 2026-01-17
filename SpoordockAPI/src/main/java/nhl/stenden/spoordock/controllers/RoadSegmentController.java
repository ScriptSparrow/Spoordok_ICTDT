package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.services.RoadService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roads") // /api om verschil te maken tussen ... en html pages
public class RoadSegmentController {

    private final RoadService roadService;

    public RoadSegmentController(RoadTypeRepository roadTypeRepository, RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping("list") // "list" ipv "/list", anders vanaf de root ipv "/api/road/list"
    public List<RoadSegementDTO> getRoads() {
        return roadService.getRoadDTOs();
    }

    @PostMapping
    public ResponseEntity<?> addRoad(RoadSegementDTO roadSegementDTO) {
        if (roadSegementDTO == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO cannot be null");
        }

        if(roadSegementDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO ID cannot be null");
        }

        try {
            roadService.addRoadSegment(roadSegementDTO);
            return ResponseEntity.status(201).body("Road segment created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateRoad(RoadSegementDTO roadSegementDTO) {
        if (roadSegementDTO == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO cannot be null");
        }

        if(roadSegementDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO ID cannot be null");
        }

        try {
            roadService.updateRoadSegment(roadSegementDTO);
            return ResponseEntity.status(200).body("Road segment updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteRoad(RoadSegementDTO roadSegementDTO) {
        if (roadSegementDTO == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO cannot be null");
        }

        if(roadSegementDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegementDTO ID cannot be null");
        }

        roadService.deleteRoadSegment(roadSegementDTO);
        return ResponseEntity.status(200).body("Road segment deleted successfully");
    }

}