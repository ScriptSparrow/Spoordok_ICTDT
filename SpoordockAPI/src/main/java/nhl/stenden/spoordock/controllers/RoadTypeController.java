package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.services.RoadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
"De RoadTypeController stelt een read-only API beschikbaar voor wegtypes en haalt deze via
de RoadService op in de vorm van DTO's, zonder dat de frontend afhankelijk is van database-details."
 */

@RestController
@RequestMapping("/api/roads/types")
public class RoadTypeController {

    private final RoadService roadService;

    public RoadTypeController(RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping("list")
    public List<RoadTypeDTO> getRoadTypes() {return roadService.getRoadTypeDTOs();}
}
