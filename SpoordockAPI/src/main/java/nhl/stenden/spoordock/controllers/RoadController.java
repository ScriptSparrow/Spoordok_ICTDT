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
@RequestMapping("/swagger")
public class RoadController {

    private final RoadTypeRepository roadTypeRepository;
    private final RoadService roadService;

    public RoadController(RoadTypeRepository roadTypeRepository, RoadService roadService) {
        this.roadTypeRepository = roadTypeRepository;
        this.roadService = roadService;
    }

//    @GetMapping("/roads")
//    public List<RoadDTO> getRoads() {
//        return roadService.toDTO (roadTypeRepository.findAll());
//    }

    @GetMapping("/roads")
    public List<RoadDTO> getRoads() {
        return Collections.singletonList(roadService.toDTO((RoadSegment) roadTypeRepository.findAll()));
    }
}
