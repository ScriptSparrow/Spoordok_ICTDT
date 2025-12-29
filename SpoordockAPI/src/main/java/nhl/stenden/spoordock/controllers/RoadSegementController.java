package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.services.RoadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roads") // /api om verschil te maken tussen ... en html pages
public class RoadSegementController {

    private final RoadService roadService;

    public RoadSegementController(RoadTypeRepository roadTypeRepository, RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping("list") // "list" ipv "/list", anders vanaf de root ipv "/api/road/list"
    public List<RoadSegementDTO> getRoads() {
        return roadService.getRoadDTOs();
    }
}
