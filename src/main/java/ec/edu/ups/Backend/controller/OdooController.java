package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    @Autowired
    private DockerService dockerService;

    @PostMapping("/create")
    public Map<String, String> createInstance(@RequestBody InstanceRequest request) {
        // Llamar al servicio para crear la instancia de Odoo en la categoría indicada
        String url = dockerService.createOdooInstance(request.getName(), request.getCategory());

        // Crear la respuesta JSON
        Map<String, String> response = new HashMap<>();
        if (url != null && url.startsWith("http")) {
            response.put("message", "Instancia de Odoo creada con éxito en " + request.getCategory());
            response.put("url", url);
        } else {
            response.put("message", "Error: No se pudo crear la instancia en " + request.getCategory());
        }
        return response;
    }

    // Clase interna para manejar la petición
    static class InstanceRequest {
        private String name;
        private String category; // Nueva propiedad

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }


    @GetMapping("/instances")
    public List<OdooInstance> getInstances() {
        return dockerService.getAllInstances();
    }

    @PostMapping("/backup")
    public ResponseEntity<String> backupInstance(@RequestBody Map<String, String> request) {
        String instanceName = request.get("name");
        String category = request.get("category");

        boolean success = dockerService.backupOdooInstance(instanceName, category);
        if (success) {
            return ResponseEntity.ok("Backup realizado con éxito para la instancia: " + instanceName);
        } else {
            return ResponseEntity.status(500).body("Error al realizar el backup de la instancia: " + instanceName);
        }
    }
}
