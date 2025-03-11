package ec.edu.ups.Backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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

    //--------
    @Value("${github.local.repo.path}") // Ruta donde está clonado el repo
    private String localRepoPath;

    @Value("${github.username}")
    private String gitUsername;

    @Value("${github.token}") // Usa un token de acceso personal en lugar de contraseña
    private String gitToken;

    public String pull() {
        try {
            Git git = Git.open(new File(localRepoPath));
            git.pull()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitToken))
                    .call();
            return "Pull exitoso";
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
            return "Error al hacer pull: " + e.getMessage();
        }
    }

    public String push() {
        try {
            Git git = Git.open(new File(localRepoPath));
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitToken))
                    .call();
            return "Push exitoso";
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
            return "Error al hacer push: " + e.getMessage();
        }
    }

    //-------------
    public String mergeBranches(String fromBranch, String toBranch) {
        try {
            Git git = Git.open(new File(localRepoPath));

            // Traer actualizaciones del remoto antes del merge
            git.fetch().setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitToken)).call();

            // Verificar si las ramas existen
            if (git.getRepository().resolve(fromBranch) == null) {
                return "Error: La rama " + fromBranch + " no existe.";
            }
            if (git.getRepository().resolve(toBranch) == null) {
                return "Error: La rama " + toBranch + " no existe.";
            }

            // Checkout a la rama destino
            git.checkout().setName(toBranch).call();

            // Merge de la rama origen
            MergeResult mergeResult = git.merge()
                    .include(git.getRepository().resolve(fromBranch))
                    .setCommit(true)
                    .call();

            if (mergeResult.getMergeStatus().isSuccessful()) {
                return "Merge exitoso de " + fromBranch + " en " + toBranch;
            } else {
                return "Conflicto al hacer merge: " + mergeResult.getMergeStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error en el merge: " + e.getMessage();
        }
    }


}
