package com.viajajunto.api.core.security;

import com.viajajunto.api.modules.auth.entity.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Usuário autenticado pelo token do Firebase. Os controllers recebem este objeto com
 * {@code @AuthenticationPrincipal} e usam {@link #getId()}, o ID do banco: o token só traz o
 * {@code firebaseUid}, e quem faz a ponte entre os dois é o {@code FirebaseJwtAuthenticationConverter}.
 * A API não guarda nem recebe senha: as credenciais ficam no Firebase.
 */
@Getter
public class UserPrincipal {

    private final Long id;
    private final String nome;
    private final String email;
    private final String firebaseUid;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UserEntity user) {
        this.id = user.getId();
        this.nome = user.getNome();
        this.email = user.getEmail();
        this.firebaseUid = user.getFirebaseUid();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
