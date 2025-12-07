package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadDTO;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.services.RoadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/roads") // /api om verschil te maken tussen ... en html pages
public class RoadController {

    private final RoadTypeRepository roadTypeRepository;
    private final RoadService roadService;

    public RoadController(RoadTypeRepository roadTypeRepository, RoadService roadService) {
        this.roadTypeRepository = roadTypeRepository;
        this.roadService = roadService;
    }

    @GetMapping("list") // "list" ipv "/list", anders vanaf de root ipv "/api/road/list"
    public List<RoadDTO> getRoads() {
        return roadService.getRoadDTOs();
    }
}
