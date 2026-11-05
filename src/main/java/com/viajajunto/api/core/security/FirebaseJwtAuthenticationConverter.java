package com.viajajunto.api.core.security;

import com.viajajunto.api.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Transforma o token do Firebase já validado em um {@link UserPrincipal}. É a ponte entre o
 * {@code firebaseUid} do token e o usuário do banco (criado ou vinculado no primeiro acesso), para
 * que os controllers continuem usando {@code @AuthenticationPrincipal UserPrincipal}.
 */
@Component
@RequiredArgsConstructor
public class FirebaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserPrincipal principal = new UserPrincipal(userService.resolveFromFirebaseToken(jwt));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
