package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    @Autowired
    private DockerService dockerService;

    @PostMapping("/create")
    public ResponseEntity<String> createOdooInstance(@RequestBody InstanceRequest request) {
        String name = request.getName();
        String category = request.getCategory();

        String result = dockerService.createOdooInstance(name, category);

        if (result.contains("http")) {
            return ResponseEntity.ok("✅ Instancia creada exitosamente en: " + result);
        } else {
            return ResponseEntity.status(500).body("❌ Error al crear instancia:\n" + result);
        }
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

    public String runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        // Para Windows: se usa cmd /c
        builder.command("cmd", "/c", command);
        builder.redirectErrorStream(true); // Combina stdout y stderr

        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error al ejecutar el comando: " + command + "\nSalida:\n" + output);
        }

        return output.toString();
    }


    private boolean containerExists(String containerName) throws IOException, InterruptedException {
        String command = "docker ps -a --format \\\"{{.Names}}\\\"";
        String output = runCommand(command);
        return Arrays.asList(output.split("\n")).contains(containerName);
    }


    @GetMapping("/instances")
    public List<OdooInstance> getInstances() {
        return dockerService.getAllInstances();
    }

    @GetMapping("/init-template")
    public ResponseEntity<String> initTemplate() {
        String result = dockerService.initializeTemplateDatabase();
        return result.startsWith("Funciono")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(500).body(result);
    }

    @GetMapping("/export-template")
    public ResponseEntity<String> exportTemplate() {
        String result = dockerService.exportTemplateDatabase();
        return result.startsWith("✅") ? ResponseEntity.ok(result) : ResponseEntity.status(500).body(result);
    }

    @PostMapping("/import-template")
    public ResponseEntity<String> importTemplate(@RequestBody Map<String, String> payload) {
        String fileName = payload.get("fileName");
        String result = dockerService.importTemplateDatabase(fileName);
        return result.startsWith("✅") ? ResponseEntity.ok(result) : ResponseEntity.status(500).body(result);
    }



}
