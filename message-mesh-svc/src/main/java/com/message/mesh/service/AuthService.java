package com.message.mesh.service;

import com.message.mesh.domain.User;
import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.LoginRequest;
import com.message.mesh.dto.RegisterRequest;
import com.message.mesh.dto.UserDto;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BadRequestException("Username already taken: " + req.username());
        }
        User user = User.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .build();
        userRepository.save(user);
        log.info("Registered new user '{}'", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getTokenVersion());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        String token = jwtUtil.generateToken(user.getUsername(), user.getTokenVersion());
        log.info("User '{}' logged in", user.getUsername());
        return new AuthResponse(token, UserDto.from(user));
    }
}
