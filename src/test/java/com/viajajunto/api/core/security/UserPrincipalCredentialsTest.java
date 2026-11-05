package com.viajajunto.api.core.security;

import com.viajajunto.api.modules.auth.entity.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// A senha só serve ao login: não pode sobrar no usuário que vai para o contexto da requisição.
@ExtendWith(MockitoExtension.class)
class UserPrincipalCredentialsTest {

    private static final String HASH = "$2a$10$hash-da-senha";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Environment environment;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserPrincipal principal() {
        return new UserPrincipal(UserEntity.builder()
                .id(7L)
                .nome("Ana")
                .email("ana@viajajunto.com")
                .senha(HASH)
                .build());
    }

    @Test
    @DisplayName("Mantém a senha para o login e apaga quando as credenciais são apagadas")
    void shouldKeepPasswordUntilCredentialsAreErased() {
        UserPrincipal principal = principal();

        assertEquals(HASH, principal.getPassword());

        principal.eraseCredentials();

        assertNull(principal.getPassword());
        assertEquals("ana@viajajunto.com", principal.getUsername());
        assertEquals(7L, principal.getId());
    }

    @Test
    @DisplayName("O filtro JWT coloca no contexto da requisição um usuário sem o hash da senha")
    void shouldNotKeepPasswordInSecurityContextAfterJwtAuthentication() throws Exception {
        UserPrincipal principal = principal();
        when(jwtService.extractUsername("jwt-valido")).thenReturn("ana@viajajunto.com");
        when(userDetailsService.loadUserByUsername("ana@viajajunto.com")).thenReturn(principal);
        when(jwtService.isTokenValid(any(String.class), any(UserDetails.class))).thenReturn(true);

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService, environment);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/viagens");
        request.addHeader("Authorization", "Bearer jwt-valido");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UserPrincipal authenticated =
                (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(7L, authenticated.getId());
        assertNull(authenticated.getPassword());
    }
}
