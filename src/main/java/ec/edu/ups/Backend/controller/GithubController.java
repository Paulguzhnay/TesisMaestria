package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
@CrossOrigin(origins = "http://localhost:4200") // Permite conexión desde Angular
public class GithubController {

    @Autowired
    private GithubService githubService;





}
