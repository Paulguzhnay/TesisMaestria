package ec.edu.ups.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api/shell")

public class ShellController {
    @GetMapping("/logs")
    public ResponseEntity<String> getLogs(@RequestParam String containerName) {
        try {
            System.out.println("Obteniendo logs de: " + containerName);
            System.out.println("Solicitando logs del contenedor: " + containerName);
            Process process = new ProcessBuilder("docker", "logs", "--tail", "100", containerName)
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return ResponseEntity.ok(output.toString());
        } catch (IOException e) {
            e.printStackTrace(); // Agrega esto para ver el error exacto
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener logs: " + e.getMessage());
        }
    }

    @PostMapping("/exec")
    public ResponseEntity<String> execCommandInContainer(@RequestParam String containerName, @RequestBody String command) {
        try {
            System.out.println("🔧 Ejecutando en " + containerName + ": " + command);
            ProcessBuilder pb = new ProcessBuilder("docker", "exec", containerName, "bash", "-c", command);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error al ejecutar el comando.");
            }

            return ResponseEntity.ok(output.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error: " + e.getMessage());
        }
    }

}
