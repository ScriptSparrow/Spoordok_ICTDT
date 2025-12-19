package nhl.stenden.spoordock.controllers;

import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.services.RoadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roads") // kan deze hetzelfde zijn als de RoadSementController
public class RoadTypeController {

    private final RoadTypeRepository roadTypeRepository;
    private final RoadService roadService;

    public RoadTypeController(RoadTypeRepository roadTypeRepository, RoadService roadService) {
        this.roadTypeRepository = roadTypeRepository;
        this.roadService = roadService;
    }

    @GetMapping("list")
    public List<RoadTypeDTO> getRoadTypes() {return roadService.getRoadTypeDTOs();}
}
