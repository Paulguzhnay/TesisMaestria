package ec.edu.ups.Backend.service;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class GithubService {

    private final String GITHUB_API_URL = "https://api.github.com/repos/Paulguzhnay/TesisMaestria/branches";

    public List<String> getBranches() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(GITHUB_API_URL, String.class);

        JSONArray jsonArray = new JSONArray(response.getBody());
        List<String> branches = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            branches.add(jsonArray.getJSONObject(i).getString("name"));
        }
        return branches;
    }
}
