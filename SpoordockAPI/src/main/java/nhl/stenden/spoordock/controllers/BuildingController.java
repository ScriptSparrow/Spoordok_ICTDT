package nhl.stenden.spoordock.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {


    @GetMapping("list")
    public String listBuildings() {
        return "List of buildings"; //TODO: Implement actual logic
    }

    @PostMapping("building")
    public String addBuilding() {
        return "Add a new building"; //TODO: Implement actual logic
    }

    @PutMapping("building")
    public String updateBuilding(@PathVariable UUID id) {
        return "Update an existing building"; //TODO: Implement actual logic
    }

    @GetMapping("building/{id}")
    public String getBuilding(@PathVariable UUID id) {
        return "Get building details"; //TODO: Implement actual logic
    }

}
