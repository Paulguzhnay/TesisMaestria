package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shell")
public class ShellDBController {

    @Autowired
    private DockerService dockerService;

    @PostMapping("/db")
    public ResponseEntity<String> executeDbCommand(@RequestBody Map<String, String> payload) {
        String command = payload.get("command");
        String instanceName = payload.get("name");
        String category = payload.get("category");

        if (command == null || instanceName == null || category == null) {
            return ResponseEntity.badRequest().body("❌ Faltan datos requeridos.");
        }

        String output = dockerService.executeSqlCommandInInstance(command, instanceName, category);
        return ResponseEntity.ok(output);
    }


}
