package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.ProjectRepository;
import ec.edu.ups.Backend.repository.UserRepository;
import ec.edu.ups.Backend.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private GithubService githubService;

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping
    public ResponseEntity<?> getProjectsForUser(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                System.err.println("⚠️ Autenticación no disponible.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
            }

            String username = authentication.getName();
            System.out.println("🔍 Usuario autenticado: " + username);

            Optional<User> userOpt = userRepo.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.err.println("⚠️ Usuario no encontrado en la base de datos.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no válido");
            }

            List<Project> userProjects = projectRepo.findByUser(userOpt.get());
            return ResponseEntity.ok(userProjects);

        } catch (Exception e) {
            System.err.println("❌ Error al obtener proyectos del usuario:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Project project, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepo.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no válido");
        }

        User user = userOpt.get();
        project.setUser(user);

        try {
            githubService.createRepository(user.getGithubToken(), project.getName());
        } catch (AuthenticationCredentialsNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de GitHub expirado o inválido. Vuelva a iniciar sesión.");
        } catch (Exception e) {
            System.err.println("❌ Error general al crear repositorio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creando repositorio en GitHub: " + e.getMessage());
        }

        Project savedProject = projectRepo.save(project);
        return ResponseEntity.ok(savedProject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        Optional<Project> projectOpt = projectRepo.findById(id);

        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Project project = projectOpt.get();
        String username = authentication.getName();

        if (!project.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        projectRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
