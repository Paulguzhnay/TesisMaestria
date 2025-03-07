package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.SidebarItem;
import ec.edu.ups.Backend.service.SidebarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sidebar")
@CrossOrigin(origins = "*")
public class SidebarController {

    private final SidebarService sidebarService;

    public SidebarController(SidebarService sidebarService) {
        this.sidebarService = sidebarService;
    }

    @GetMapping
    public List<SidebarItem> getSidebarItems() {
        return sidebarService.getSidebarItems();
    }

    @PostMapping
    public ResponseEntity<List<SidebarItem>> saveSidebarItems(@RequestBody List<SidebarItem> items) {
        List<SidebarItem> savedItems = sidebarService.saveAll(items);
        return ResponseEntity.ok(savedItems);
    }

}