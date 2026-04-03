package com.festapp.dashboar.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                Long userId = jwtProvider.getUserIdFromJWT(jwt);
                String username = jwtProvider.getUsernameFromJWT(jwt);
                String role = jwtProvider.getRoleFromJWT(jwt);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                CustomUserDetails userDetails = CustomUserDetails.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .isActive(true)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.singletonList(authority));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Secured {} {} for user: {} (userId: {})", request.getMethod(), request.getRequestURI(), username, userId);
            } else {
                log.debug("No valid JWT token found in request");
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}



