package ec.edu.ups.Backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

@Service
public class GithubService {

    private final String GITHUB_API_URL = "https://api.github.com/repos/Paulguzhnay/TesisMaestria";

    private List<String> fetchFromGitHub(String url) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Realizamos la solicitud HTTP
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Verificamos el estado de la respuesta
            if (response.getStatusCode() != HttpStatus.OK) {
                return new ArrayList<>();
            }

            // Convertimos la respuesta a JSONArray y luego extraemos los valores como String
            JSONArray jsonArray = new JSONArray(response.getBody());
            List<String> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                result.add(jsonArray.getJSONObject(i).toString());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Obtener commits de una rama específica
    public List<String> getCommitsByBranch(String branch) {
        String url = GITHUB_API_URL + "/commits?sha=" + branch;
        List<String> commits = new ArrayList<>();

        // Ahora extraemos solo los mensajes y SHA de los commits
        for (String obj : fetchFromGitHub(url)) {
            JSONObject commit = new JSONObject(obj);
            String message = commit.getJSONObject("commit").getString("message");
            String sha = commit.getString("sha").substring(0, 7);
            commits.add(sha + ": " + message);
        }

        return commits;
    }

    // Obtener ramas del repositorio
    public List<String> getBranches() {
        String url = GITHUB_API_URL + "/branches";
        List<String> branches = new ArrayList<>();

        // Extraemos solo los nombres de las ramas
        for (String obj : fetchFromGitHub(url)) {
            branches.add(new JSONObject(obj).getString("name"));
        }

        return branches;
    }
}
