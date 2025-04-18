package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
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
        String instance = request.get("name");
        String category = request.get("category");
        String result = dockerService.createBackup(instance, category);
        return result.startsWith("Backup creado")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) throws Exception {
        File file = new File("C:\\Users\\paul-\\2025\\Maestria\\Tesis\\Backups", fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/restore-specific")
    public ResponseEntity<String> restoreSpecific(@RequestBody Map<String, String> payload) {
        String result = dockerService.restoreBackup(
                payload.get("name"),
                payload.get("category"),
                payload.get("dbBackupFileName"),
                payload.get("odooBackupFileName")
        );
        return result.startsWith("✅")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
