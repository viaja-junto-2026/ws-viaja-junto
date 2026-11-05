package com.viajajunto.api.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.auth.dto.UpdateUserDTO;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Jwt.Builder jwt(String uid) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(uid);
    }

    private Jwt verified(String uid, String email) {
        return jwt(uid).claim("email", email).claim("email_verified", true).build();
    }

    @Test
    @DisplayName("Usuário já conhecido pelo firebaseUid é devolvido sem olhar o e-mail")
    void resolvesByFirebaseUid() {
        UserEntity existing = UserEntity.builder().id(7L).email("ana@email.com").firebaseUid("uid-1").build();
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(existing));

        UserEntity resolved = userService.resolveFromFirebaseToken(
                jwt("uid-1").claim("email_verified", false).build());

        assertSame(existing, resolved);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vincula o firebaseUid a um usuário existente com o mesmo e-mail verificado")
    void linksExistingUserByVerifiedEmail() {
        UserEntity existing = UserEntity.builder().id(7L).email("ana@email.com").build();
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserEntity resolved = userService.resolveFromFirebaseToken(verified("uid-1", "ana@email.com"));

        assertEquals("uid-1", resolved.getFirebaseUid());
        assertEquals(7L, resolved.getId());
    }

    @Test
    @DisplayName("Normaliza o e-mail do token (minúsculas e sem espaços) antes de procurar")
    void normalizesEmail() {
        UserEntity existing = UserEntity.builder().id(7L).email("ana@email.com").build();
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserEntity resolved = userService.resolveFromFirebaseToken(verified("uid-1", "  Ana@Email.COM "));

        assertEquals(7L, resolved.getId());
    }

    @Test
    @DisplayName("Recusa quando o e-mail já pertence a outra conta Firebase")
    void rejectsEmailLinkedToAnotherUid() {
        UserEntity existing = UserEntity.builder().id(7L).email("ana@email.com").firebaseUid("outro-uid").build();
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(existing));

        assertThrows(InvalidBearerTokenException.class,
                () -> userService.resolveFromFirebaseToken(verified("uid-1", "ana@email.com")));

        assertEquals("outro-uid", existing.getFirebaseUid());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cria o usuário no primeiro acesso com nome, e-mail e foto do token")
    void createsUserOnFirstAccess() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nova@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        userService.resolveFromFirebaseToken(jwt("uid-1")
                .claim("email", "nova@email.com")
                .claim("email_verified", true)
                .claim("name", "  Nova Pessoa ")
                .claim("picture", "https://exemplo.com/foto.png")
                .build());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity created = captor.getValue();
        assertEquals("uid-1", created.getFirebaseUid());
        assertEquals("nova@email.com", created.getEmail());
        assertEquals("Nova Pessoa", created.getNome());
        assertEquals("https://exemplo.com/foto.png", created.getAvatarUrl());
    }

    @Test
    @DisplayName("Sem nome no token, usa a parte do e-mail antes do @")
    void usesEmailNameWhenTokenHasNoName() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("joao.silva@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserEntity created = userService.resolveFromFirebaseToken(verified("uid-1", "joao.silva@email.com"));

        assertEquals("joao.silva", created.getNome());
        assertNull(created.getAvatarUrl());
    }

    @Test
    @DisplayName("Recusa token sem e-mail")
    void rejectsTokenWithoutEmail() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());

        assertThrows(InvalidBearerTokenException.class,
                () -> userService.resolveFromFirebaseToken(jwt("uid-1").claim("email_verified", true).build()));
    }

    @Test
    @DisplayName("Recusa e-mail não verificado, tanto para vincular quanto para criar")
    void rejectsUnverifiedEmail() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());

        assertThrows(InvalidBearerTokenException.class, () -> userService.resolveFromFirebaseToken(
                jwt("uid-1").claim("email", "ana@email.com").claim("email_verified", false).build()));
        assertThrows(InvalidBearerTokenException.class, () -> userService.resolveFromFirebaseToken(
                jwt("uid-1").claim("email", "ana@email.com").build()));

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Se duas primeiras requisições criam o mesmo usuário, a segunda relê o que a primeira criou")
    void rereadsUserWhenConcurrentCreationHitsUniqueConstraint() {
        UserEntity criadoPelaOutra = UserEntity.builder().id(9L).email("nova@email.com").firebaseUid("uid-1").build();
        when(userRepository.findByFirebaseUid("uid-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(criadoPelaOutra));
        when(userRepository.findByEmail("nova@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        UserEntity resolved = userService.resolveFromFirebaseToken(verified("uid-1", "nova@email.com"));

        assertSame(criadoPelaOutra, resolved);
    }

    @Test
    @DisplayName("getProfile devolve os dados do usuário")
    void getsProfile() {
        UserEntity user = UserEntity.builder().id(7L).nome("Ana").email("ana@email.com").avatarUrl("http://a").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserDTO profile = userService.getProfile(7L);

        assertEquals(7L, profile.getId());
        assertEquals("Ana", profile.getNome());
        assertEquals("ana@email.com", profile.getEmail());
        assertEquals("http://a", profile.getAvatarUrl());
    }

    @Test
    @DisplayName("getProfile de usuário inexistente lança ResourceNotFoundException")
    void getProfileOfUnknownUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(7L));
    }

    @Test
    @DisplayName("updateProfile altera só os campos enviados e remove espaços das pontas")
    void updatesOnlySentFields() {
        UserEntity user = UserEntity.builder().id(7L).nome("Ana").email("ana@email.com").avatarUrl("http://a").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDTO updated = userService.updateProfile(7L, UpdateUserDTO.builder().nome("  Ana Maria ").build());

        assertEquals("Ana Maria", updated.getNome());
        assertEquals("http://a", updated.getAvatarUrl());
    }

    @Test
    @DisplayName("updateProfile com avatar em branco remove o avatar")
    void blankAvatarRemovesAvatar() {
        UserEntity user = UserEntity.builder().id(7L).nome("Ana").email("ana@email.com").avatarUrl("http://a").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDTO updated = userService.updateProfile(7L, UpdateUserDTO.builder().avatarUrl("  ").build());

        assertNull(updated.getAvatarUrl());
        assertEquals("Ana", updated.getNome());
    }
}
