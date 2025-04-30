package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/instances")
public class OdooInstanceController {

    @Autowired
    private OdooInstanceRepository instanceRepo;

    @GetMapping
    public List<OdooInstance> getAll() {
        return instanceRepo.findAll();
    }

    @PostMapping
    public OdooInstance create(@RequestBody OdooInstance instance) {
        return instanceRepo.save(instance);
    }

    @GetMapping("/project/{projectId}")
    public List<OdooInstance> getByProject(@PathVariable Long projectId) {
        return instanceRepo.findByProjectId(projectId);
    }
}
