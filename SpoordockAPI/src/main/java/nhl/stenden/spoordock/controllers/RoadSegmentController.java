package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegmentDTO;
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
    public List<RoadSegmentDTO> getRoads() {
        return roadService.getRoadDTOs();
    }

    @PostMapping
    public ResponseEntity<?> addRoad(RoadSegmentDTO roadSegmentDTO) {
        if (roadSegmentDTO == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO cannot be null");
        }

        if(roadSegmentDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO ID cannot be null");
        }

        try {
            roadService.addRoadSegment(roadSegmentDTO);
            return ResponseEntity.status(201).body("Road segment created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateRoad(RoadSegmentDTO roadSegmentDTO) {
        if (roadSegmentDTO == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO cannot be null");
        }

        if(roadSegmentDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO ID cannot be null");
        }

        try {
            roadService.updateRoadSegment(roadSegmentDTO);
            return ResponseEntity.status(200).body("Road segment updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteRoad(RoadSegmentDTO roadSegmentDTO) {
        if (roadSegmentDTO == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO cannot be null");
        }

        if(roadSegmentDTO.getId() == null) {
            return ResponseEntity.status(400).body("RoadSegmentDTO ID cannot be null");
        }

        roadService.deleteRoadSegment(roadSegmentDTO);
        return ResponseEntity.status(200).body("Road segment deleted successfully");
    }

}