package com.task.task.controller;

import com.task.task.model.User;
import com.task.task.model.dto.AuthDTO;
import com.task.task.repository.UserRepository;
import com.task.task.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.core.userdetails.User.withUsername;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();

        userRepository.save(user);

        UserDetails userDetails = withUsername(user.getEmail()).password(user.getPassword()).build();
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(AuthDTO.AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(@Valid @RequestBody AuthDTO.LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UserDetails userDetails = withUsername(user.getEmail()).password(user.getPassword()).build();
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(AuthDTO.AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .build());
    }
}