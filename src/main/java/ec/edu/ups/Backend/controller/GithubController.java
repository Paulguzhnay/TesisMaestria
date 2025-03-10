package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@CrossOrigin(origins = "http://localhost:4200") // Permite conexión desde Angular
public class GithubController {

    @Autowired
    private GithubService githubService;

    @GetMapping("/branches")
    public ResponseEntity<List<String>> getBranches() {
        List<String> branches = githubService.getBranches();
        return ResponseEntity.ok(branches); // Devolver ramas
    }

    @GetMapping("/commits/{branch}")
    public ResponseEntity<List<String>> getCommits(@PathVariable String branch) {
        List<String> commits = githubService.getCommitsByBranch(branch);
        if (commits.isEmpty()) {
            return ResponseEntity.notFound().build(); // Si no hay commits
        }
        return ResponseEntity.ok(commits); // Si hay commits
    }
}
