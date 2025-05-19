package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.MergeRequest;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    private final DockerService dockerService;
    private final OdooInstanceRepository odooInstanceRepository;

    public OdooController(DockerService dockerService, OdooInstanceRepository odooInstanceRepository) {
        this.dockerService = dockerService;
        this.odooInstanceRepository = odooInstanceRepository;
    }

    @PostMapping("/create")
    public Map<String, String> createInstance(@RequestBody InstanceRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            String url = dockerService.createOdooInstance(
                    request.getName(),
                    request.getCategory(),
                    request.getProjectId()
            );

            if (url != null && url.startsWith("http")) {
                response.put("message", "Instancia de Odoo creada con éxito en " + request.getCategory());
                response.put("url", url);
            } else {
                response.put("message", "Error: No se pudo crear la instancia.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Error inesperado: " + e.getMessage());
        }

        return response;
    }

    static class InstanceRequest {
        private String name;
        private String category;
        private Long projectId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
    }

    @GetMapping("/instances")
    public ResponseEntity<List<OdooInstance>> getInstances(Authentication auth) {
        try {
            String username = auth.getName();
            List<OdooInstance> instances = odooInstanceRepository.findByProjectUserUsername(username);
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<String> mergeInstances(@RequestBody MergeRequest request) {
        try {
            String result = dockerService.mergeOdooInstances(
                    request.getProjectId(),
                    request.getSource(),
                    request.getTarget()
            );

            return result.startsWith("✅")
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(500).body(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error inesperado: " + e.getMessage());
        }
    }

    @GetMapping("/instances/by-project/{projectName}")
    public ResponseEntity<List<OdooInstance>> getInstancesByProject(@PathVariable String projectName) {
        try {
            List<OdooInstance> instances = dockerService.getInstancesByProject(projectName);
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }



}
