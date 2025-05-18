package ec.edu.ups.Backend.controller;
import ec.edu.ups.Backend.config.JwtUtil;
import ec.edu.ups.Backend.dto.AuthRequest;
import ec.edu.ups.Backend.dto.AuthResponse;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


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

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(token);
    }

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
}
