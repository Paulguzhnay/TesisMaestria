package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.ProjectRepository;
import ec.edu.ups.Backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GithubService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    @Value("${github.api.url:https://api.github.com}")
    private String githubApiUrl;

    // -------------------------------------
    public void createRepository(String accessToken, String repoName) {
        String url = githubApiUrl + "/user/repos";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", repoName);
        body.put("auto_init", true); // ✅ Crear README automáticamente
        body.put("private", true);   // Opcional: lo hace privado

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Repositorio creado correctamente con README.");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("⚠️ El repositorio ya existe.");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationCredentialsNotFoundException("Token inválido o expirado.");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error al crear repositorio: " + e.getMessage());
        }
    }


    public void createBranch(String accessToken, String owner, String repo, String newBranchName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // Obtener SHA del branch principal
            String sha;
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        githubApiUrl + "/repos/" + owner + "/" + repo + "/git/ref/heads/main",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );
                sha = ((Map<?, ?>) response.getBody().get("object")).get("sha").toString();
            } catch (HttpClientErrorException.NotFound e) {
                throw new IllegalStateException("❌ El repositorio está vacío. No se puede crear una rama sin un commit inicial.");
            }

            // Crear nueva rama
            Map<String, Object> branchBody = new HashMap<>();
            branchBody.put("ref", "refs/heads/" + newBranchName);
            branchBody.put("sha", sha);

            HttpEntity<Map<String, Object>> branchEntity = new HttpEntity<>(branchBody, headers);
            restTemplate.postForEntity(githubApiUrl + "/repos/" + owner + "/" + repo + "/git/refs", branchEntity, String.class);

        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("⚠️ La rama '" + newBranchName + "' ya existe.");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationCredentialsNotFoundException("Token de GitHub inválido o expirado.");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error general al crear rama en GitHub: " + e.getMessage());
        }
    }


}
