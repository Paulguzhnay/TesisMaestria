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



    // Nuevo endpoint para crear un backup
//    @PostMapping("/backups")
//    public ResponseEntity<String> createBackup() {
//        String backup = dockerService.createBackup();
//        if (backup == null || backup.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear backup");
//           }
//        return ResponseEntity.ok(backup);
//    }

    @GetMapping("/backups")
    public List<BackupInfo> getBackups() {
        return dockerService.getBackups();
    }

    @PostMapping("/backup")

    public ResponseEntity<String> backupInstance(@RequestBody Map<String, String> request) {
        System.out.println("Request recibida: " + request); // ✅ Agregar esto para debug
        String instanceName = request.get("name");
        String category = request.get("category");

        String response = dockerService.createBackup(instanceName, category);
        if (response.startsWith("Backup creado")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) throws IOException {
        File file = new File("C:\\Users\\paul-\\2025\\Maestria\\Tesis\\Backups", fileName);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    //----------
    @PostMapping("/restore")
    public ResponseEntity<String> restoreBackup(@RequestBody Map<String, String> request) {
        String instanceName = request.get("name");
        String category = request.get("category");
        String fileName = request.get("file");

        String result = dockerService.restoreBackup(instanceName, category, fileName);
        return ResponseEntity.ok(result);
    }







}