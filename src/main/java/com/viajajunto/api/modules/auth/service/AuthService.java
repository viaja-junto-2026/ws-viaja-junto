package com.viajajunto.api.modules.auth.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.JwtService;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.auth.dto.AuthResponseDTO;
import com.viajajunto.api.modules.auth.dto.ForgotPasswordDTO;
import com.viajajunto.api.modules.auth.dto.LoginRequestDTO;
import com.viajajunto.api.modules.auth.dto.RegisterDTO;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("O e-mail informado já está em uso.");
        }

        UserEntity user = UserEntity.builder()
                .nome(dto.getNome())
                .email(dto.getEmail().toLowerCase().trim())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .build();

        UserEntity savedUser = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(savedUser);
        String token = jwtService.generateToken(principal, savedUser.getId(), savedUser.getNome());

        return AuthResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .nome(savedUser.getNome())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail().toLowerCase().trim(), dto.getSenha())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        String token = jwtService.generateToken(principal, user.getId(), user.getNome());

        return AuthResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public UserDTO getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        return UserDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .dataCriacao(user.getDataCriacao())
                .build();
    }

    public void forgotPassword(ForgotPasswordDTO dto) {
        // RF03: Stub de envio de e-mail com token/instruções para recuperação de senha
        userRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .ifPresent(user -> {
                    // Simulação do envio de e-mail de recuperação
                });
    }
}
