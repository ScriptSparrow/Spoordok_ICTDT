package nhl.stenden.spoordock.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.services.BuildingService;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping("list")
    public ResponseEntity<List<BuildingPolygonDTO>> listBuildings(@RequestParam(name="embedTypes", defaultValue = "false") boolean embedTypes) {
        var buildings = buildingService.getBuildingPolygons(embedTypes);
        return ResponseEntity.ok(buildings);
    }

    @PostMapping("building")
    @ResponseBody
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Building successfully created", content =  @Content(schema = @Schema(implementation = BuildingPolygonDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Building Type supplied", content = @Content)
    })
    public ResponseEntity<?> addBuilding(@RequestBody BuildingPolygonDTO buildingDTO) {

        if(buildingDTO == null) {
            return ResponseEntity
                .badRequest()
                .body("Building data is required in the request body");
        }

        //Validate if building type exists
        if(buildingService.buildingTypeExists(buildingDTO.getBuildingType()) == false) {
            return ResponseEntity
                .badRequest()
                .body("Building Type does not exist in the database. Building Type has to be created first");
        }

        return ResponseEntity.ok(buildingService.addBuilding(buildingDTO));
    }

    @PutMapping("building/{id}")
    public ResponseEntity<?> updateBuilding(@PathVariable UUID id, @RequestBody BuildingPolygonDTO buildingDTO) {

        if(buildingDTO == null) {
            return ResponseEntity
                .badRequest()
                .body("Building data is required in the request body");
        }

        if(!id.equals(buildingDTO.getBuildingId())) {
            buildingDTO.setBuildingId(id); //Put is done on a path variable, so we enforce that ID here
        }

        var updated = buildingService.updateBuilding(buildingDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("building/{id}")
    public ResponseEntity<?> getBuilding(@PathVariable UUID id) {
        
        var buildingOpt = buildingService.getBuildingById(id);

        if(buildingOpt.isPresent()) {
            return ResponseEntity.ok(buildingOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("building/{id}")
    public ResponseEntity<?> deleteBuilding(@PathVariable UUID id) {
        buildingService.deleteBuildingById(id);
        return ResponseEntity.ok(null);
    }

}
