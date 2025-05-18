
package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/docker")
public class DockerController {

    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/backups")
    public List<BackupInfo> getBackups() {
        return dockerService.getBackups();
    }

    @PostMapping("/backup")
    public ResponseEntity<String> backupInstance(@RequestBody Map<String, String> request) {
        System.out.println("📥 Request recibida: " + request);

        try {
            Long projectId = Long.parseLong(request.get("projectId"));
            String instanceName = request.get("name");
            String category = request.get("category");

            String response = dockerService.createBackup(projectId, instanceName, category);
            return response.startsWith("Backup creado")
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Parámetros inválidos o incompletos.");
        }
    }

    @PostMapping("/restore-specific")
    public ResponseEntity<String> restoreSpecific(@RequestBody Map<String, String> payload) {
        System.out.println("📥 Payload: " + payload);

        try {
            Long projectId = Long.parseLong(payload.get("projectId"));
            String instance = payload.get("name");
            String category = payload.get("category");
            String dbFile = payload.get("dbBackupFileName");
            String odooFile = payload.get("odooBackupFileName");

            String result = dockerService.restoreBackup(projectId, instance, category, dbFile, odooFile);
            return result.startsWith("✅")
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Error en los datos enviados.");
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) throws IOException {
        File file = new File("C:\\Users\\paul-\\2025\\Maestria\\Tesis\\Backups", fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteBackup(@PathVariable String fileName) {
        try {
            File file = new File("C:\\Users\\paul-\\2025\\Maestria\\Tesis\\Backups", fileName);
            return (file.exists() && file.delete())
                    ? ResponseEntity.ok("Backup eliminado exitosamente.")
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Backup no encontrado.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al eliminar backup.");
        }
    }

    @PostMapping("/import-db")
    public ResponseEntity<String> uploadAndRestoreDb(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("category") String category) {
        try {
            String result = dockerService.importDatabaseFromFile(file, name, category);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error al importar base de datos: " + e.getMessage());
        }
    }

}
