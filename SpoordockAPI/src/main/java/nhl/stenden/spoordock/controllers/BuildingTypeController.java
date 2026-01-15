package nhl.stenden.spoordock.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.services.BuildingTypeService;

@RestController
@RequestMapping("/api/building/types")
public class BuildingTypeController {

    private final BuildingTypeService buildingTypeService;

    public BuildingTypeController(BuildingTypeService buildingTypeService) {
        this.buildingTypeService = buildingTypeService;
    }

    @GetMapping("list")
    public ResponseEntity<List<BuildingTypeDTO>> getBuildingTypes() {
        var buildingTypes = buildingTypeService.getBuildingTypes();
        return ResponseEntity.ok(buildingTypes);
    }
}