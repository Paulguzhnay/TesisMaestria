package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import ec.edu.ups.Backend.repository.ProjectRepository;
import ec.edu.ups.Backend.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import static org.json.XMLTokener.entity;

@RestController
@RequestMapping("/api/instances")
public class OdooInstanceController {
    @Autowired
    private GithubService githubService;
    @Autowired
    private ProjectRepository projectRepository;


    @Autowired
    private OdooInstanceRepository instanceRepo;

    @GetMapping
    public List<OdooInstance> getAll() {
        return instanceRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<OdooInstance> create(@RequestBody OdooInstance instance) {
        OdooInstance saved = instanceRepo.save(instance);

        // Obtener proyecto y usuario
        Project project = saved.getProject();
        if (project == null || project.getUser() == null) {
            return ResponseEntity.status(400).body(null);
        }

        String repoName = project.getName();
        String owner = project.getUser().getUsername();
        String token = project.getUser().getGithubToken();

        // Crear nueva rama en GitHub
        try {
            githubService.createBranch(token, owner, repoName, saved.getName());
        } catch (Exception e) {
            System.err.println("❌ Error al crear rama en GitHub:");
            e.printStackTrace();
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/project/{projectId}")
    public List<OdooInstance> getByProject(@PathVariable Long projectId) {
        return instanceRepo.findByProjectId(projectId);
    }

}
