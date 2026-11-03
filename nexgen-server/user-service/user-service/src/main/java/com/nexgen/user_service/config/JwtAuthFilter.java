package com.nexgen.user_service.config;

import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.LogoutService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/users/login";
    private static final String REGISTER_PATH = "/api/v1/users/register";
    private static final String REFRESH_PATH = "/api/v1/users/refresh-token";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LogoutService logoutService;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         LogoutService logoutService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.logoutService = logoutService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.equals(LOGIN_PATH) || path.equals(REGISTER_PATH) || path.equals(REFRESH_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(jwt);

            if (logoutService.isTokenBlacklisted(jwt)) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "INVALID_TOKEN", "Token has been revoked");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!jwtService.isAccessTokenValid(jwt, userDetails)) {
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "INVALID_TOKEN", "Invalid or expired access token");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN", "Invalid or expired access token");
        } catch (DataAccessException e) {
            SecurityContextHolder.clearContext();
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AUTH_UNAVAILABLE", "Authentication is temporarily unavailable");
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now()
                        + "\",\"status\":" + status
                        + ",\"errorCode\":\"" + code
                        + "\",\"message\":\"" + message + "\"}");
    }
}
