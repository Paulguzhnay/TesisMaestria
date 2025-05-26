
package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.dto.BackupRequest;
import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.UserRepository;
import ec.edu.ups.Backend.service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/docker")
public class DockerController {
    @Autowired
    private DockerService dockerService;

    @Autowired
    private UserRepository userRepo;





    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/backups/instance")
    public ResponseEntity<List<BackupInfo>> getBackupsForInstance(
            @RequestParam String instanceName,
            @RequestParam String category,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            List<BackupInfo> backups = dockerService.getBackupsForInstance(username, instanceName, category);
            System.out.println("backups "+backups);
            return ResponseEntity.ok(backups);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/backup")
    public ResponseEntity<Map<String, String>> backupInstance(@RequestBody BackupRequest request, Authentication authentication) {
        Map<String, String> responseMap = new HashMap<>();
        try {
            String username = authentication.getName();
            String result = dockerService.createBackup(username, request.getProjectId(), request.getName(), request.getCategory());

            responseMap.put("message", result);
            return result.startsWith("✅")
                    ? ResponseEntity.ok(responseMap)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);

        } catch (Exception e) {
            e.printStackTrace();
            responseMap.put("message", "❌ Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseMap);
        }
    }

    @PostMapping("/restore-specific")
    public ResponseEntity<String> restoreSpecific(@RequestBody Map<String, String> payload, Authentication authentication) {
        try {
            Long projectId = Long.parseLong(payload.get("projectId"));
            String instance = payload.get("name");
            String category = payload.get("category");
            String dbFile = payload.get("dbBackupFileName");
            String odooFile = payload.get("odooBackupFileName");

            System.out.println("projectId "+projectId);
            System.out.println(" instance "+instance);
            System.out.println("category "+category);
            System.out.println("dbFile "+dbFile);
            System.out.println(" odooFile "+odooFile);

            String username = authentication.getName(); //  Extraer nombre del usuario autenticado

            String result = dockerService.restoreBackup(username, projectId, instance, category, dbFile, odooFile);

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
            System.out.println("🛠️ Recibido archivo: " + file.getOriginalFilename());
            System.out.println("Instancia destino: " + name + " | Categoría: " + category);

            String result = dockerService.importDatabaseFromFile(file, name, category);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error al importar base de datos: " + e.getMessage());
        }
    }

    //-------------------------------
    @PostMapping("/import-db-advanced")
    public ResponseEntity<String> importDbAndOdoo(
            @RequestParam("dbFile") MultipartFile dbFile,
            @RequestParam(value = "odooFile", required = false) MultipartFile odooFile,
            @RequestParam("name") String name,
            @RequestParam("category") String category) {

        try {
            String result = dockerService.importDatabaseAndOdooFiles(dbFile, odooFile, name, category);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error durante importación: " + e.getMessage());
        }
    }

}
