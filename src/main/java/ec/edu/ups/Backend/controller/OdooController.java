package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    @Autowired
    private DockerService dockerService;

    @PostMapping("/create")
    public Map<String, String> createInstance(@RequestBody InstanceRequest request) {
        String message = dockerService.createOdooInstance(request.getName());

        // Crear un objeto JSON en formato Map
        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return response;
    }

    static class InstanceRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
