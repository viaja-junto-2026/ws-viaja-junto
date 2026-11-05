package com.viajajunto.api.modules.auth.service;

import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.auth.dto.UpdateUserDTO;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Usuário dono do token do Firebase. O token só traz o {@code firebaseUid} (claim "sub"); a API
     * trabalha com o ID do banco. Ordem:
     * <ol>
     *   <li>já existe um usuário com esse {@code firebaseUid}: é ele;</li>
     *   <li>existe um usuário com o mesmo e-mail e sem {@code firebaseUid}: vincula (é assim que os
     *       usuários de exemplo viram contas reais);</li>
     *   <li>não existe: cria no primeiro acesso, com e-mail, nome e foto do token.</li>
     * </ol>
     * O e-mail precisa estar verificado no Firebase, tanto para vincular quanto para criar: sem isso,
     * alguém poderia registrar o e-mail de outra pessoa e assumir a conta dela.
     *
     * <p>Não é {@code @Transactional}: roda no filtro de autenticação e, se duas primeiras requisições
     * chegarem juntas, a segunda cai na restrição de unicidade e relê o usuário que a primeira criou.
     */
    public UserEntity resolveFromFirebaseToken(Jwt jwt) {
        String uid = jwt.getSubject();

        Optional<UserEntity> byUid = userRepository.findByFirebaseUid(uid);
        if (byUid.isPresent()) {
            return byUid.get();
        }

        String email = normalizeEmail(jwt.getClaimAsString("email"));
        if (email == null) {
            throw new InvalidBearerTokenException("O token não traz o e-mail do usuário.");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new InvalidBearerTokenException("O e-mail do usuário ainda não foi verificado.");
        }

        Optional<UserEntity> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            return linkToFirebaseUid(byEmail.get(), uid);
        }
        return createFromToken(jwt, uid, email);
    }

    @Transactional(readOnly = true)
    public UserDTO getProfile(Long userId) {
        return toDTO(findById(userId));
    }

    /** PATCH: só altera o que foi enviado. */
    @Transactional
    public UserDTO updateProfile(Long userId, UpdateUserDTO dto) {
        UserEntity user = findById(userId);
        if (dto.getNome() != null) {
            user.setNome(dto.getNome().trim());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl().isBlank() ? null : dto.getAvatarUrl().trim());
        }
        return toDTO(userRepository.save(user));
    }

    private UserEntity linkToFirebaseUid(UserEntity existing, String uid) {
        if (existing.getFirebaseUid() != null) {
            // Mesmo e-mail, outro uid: não é a mesma pessoa para a API.
            throw new InvalidBearerTokenException("O e-mail já está vinculado a outra conta.");
        }
        existing.setFirebaseUid(uid);
        log.info("Usuário {} vinculado ao Firebase.", existing.getId());
        return userRepository.save(existing);
    }

    private UserEntity createFromToken(Jwt jwt, String uid, String email) {
        UserEntity novo = UserEntity.builder()
                .firebaseUid(uid)
                .email(email)
                .nome(nameFrom(jwt, email))
                .avatarUrl(jwt.getClaimAsString("picture"))
                .build();
        try {
            UserEntity saved = userRepository.save(novo);
            log.info("Usuário {} criado a partir do Firebase.", saved.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByFirebaseUid(uid).orElseThrow(() -> e);
        }
    }

    private UserEntity findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private String nameFrom(Jwt jwt, String email) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return email.substring(0, email.indexOf('@'));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserDTO toDTO(UserEntity user) {
        return UserDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .dataCriacao(user.getDataCriacao())
                .build();
    }
}
