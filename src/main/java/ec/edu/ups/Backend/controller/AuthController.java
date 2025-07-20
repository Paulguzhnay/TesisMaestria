package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.config.JwtUtil;
import ec.edu.ups.Backend.dto.AuthRequest;
import ec.edu.ups.Backend.dto.AuthResponse;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Value("${github.client.secret}")
    private String clientSecret;

    private final String clientId = "Ov23li4hBkj65uMbYvc6";





    @PostMapping("/register")
    public String register(@RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "Usuario ya existe";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.getPassword()));
        userRepository.save(user);
        return "Usuario registrado correctamente";
    }

    @GetMapping("/login/github")
    public void redirectToGitHub(HttpServletResponse response) throws IOException {
        String githubAuthUrl = "https://github.com/login/oauth/authorize?client_id=" + clientId + "&scope=read:user,repo";
        response.sendRedirect(githubAuthUrl);
    }

    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Paso 1: Intercambiar el código por un access_token
        String tokenUrl = "https://github.com/login/oauth/access_token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || !response.getBody().containsKey("access_token")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = (String) response.getBody().get("access_token");

        // Paso 2: Obtener información del usuario
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                userRequest,
                Map.class
        );

        Map userData = userInfoResponse.getBody();
        String username = (String) userData.get("login");
        String avatarUrl = (String) userData.get("avatar_url");

        // Paso 3: Generar JWT para nuestra app
        String token = jwtUtil.generateToken(username);

        // Registrar o actualizar en base de datos
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(""); // GitHub login
            return newUser;
        });

        user.setGithubToken(accessToken);
        userRepository.save(user);

        // Redirección al frontend con parámetros
        String redirectUrl = "http://localhost:4200/github-callback" +
                "?token=" + token +
                "&username=" + username +
                "&avatar=" + avatarUrl;

        HttpHeaders redirectHeaders = new HttpHeaders();
        redirectHeaders.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);
    }

}