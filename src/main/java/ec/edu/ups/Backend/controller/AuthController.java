package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.config.JwtUtil;
import ec.edu.ups.Backend.dto.AuthRequest;
import ec.edu.ups.Backend.dto.AuthResponse;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final String clientId = "Ov23li4hBkj65uMbYvc6";
    private final String clientSecret = "f392bcbcf77f0507955f489f20fa86125fcd01a2";



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
    public ResponseEntity<AuthResponse> githubCallback(@RequestParam String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Paso 1: Intercambiar el código por un access_token
        String tokenUrl = "https://github.com/login/oauth/access_token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

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

        // Paso 3: Generar JWT para nuestra app
        String token = jwtUtil.generateToken(username);

        // (Opcional) Registrar en base de datos si no existe
        userRepository.findByUsername(username).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(""); // Sin password ya que viene de GitHub
            return userRepository.save(newUser);
        });

        String avatarUrl = (String) userData.get("avatar_url");
        return ResponseEntity.ok(new AuthResponse(token, username, avatarUrl));
    }
}