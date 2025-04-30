package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")

public class ProjectController {

    @Autowired
    private ProjectRepository projectRepo;

    @GetMapping
    public List<Project> getAll() {
        return projectRepo.findAll();
    }

    @PostMapping
    public Project create(@RequestBody Project project) {
        return projectRepo.save(project);
    }
}
