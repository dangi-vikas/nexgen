package com.nexgen.user_service.config;

import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.LogoutService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private LogoutService logoutService;

    private JwtAuthFilter filter;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService, logoutService);
        user = User.builder()
                .username("john")
                .password("encoded")
                .role("ROLE_USER")
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setServletPath("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void malformedTokenReturns401AndDoesNotReachController() throws Exception {
        when(jwtService.extractUsername("bad")).thenThrow(new MalformedJwtException("bad"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (request, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("bad"), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));
        assertFalse(invoked.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        when(jwtService.extractUsername("expired"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("expired"), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));
        assertFalse(invoked.get());
    }

    @Test
    void tamperedTokenReturns401() throws Exception {
        when(jwtService.extractUsername("tampered"))
                .thenThrow(new SignatureException("bad signature"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("tampered"), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));
        assertFalse(invoked.get());
    }

    @Test
    void refreshTokenIsRejectedAsBearerToken() throws Exception {
        when(jwtService.extractUsername("refresh")).thenReturn("john");
        when(logoutService.isTokenBlacklisted("refresh")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.isAccessTokenValid("refresh", user)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (request, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("refresh"), response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(invoked.get());
    }

    @Test
    void validAccessTokenPopulatesSecurityContext() throws Exception {
        when(jwtService.extractUsername("good")).thenReturn("john");
        when(logoutService.isTokenBlacklisted("good")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.isAccessTokenValid("good", user)).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (request, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("good"), response, chain);

        assertTrue(invoked.get());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void blacklistedTokenReturns401() throws Exception {
        when(jwtService.extractUsername("revoked")).thenReturn("john");
        when(logoutService.isTokenBlacklisted("revoked")).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (request, res) -> invoked.set(true);

        filter.doFilter(requestWithToken("revoked"), response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(invoked.get());
    }

    @Test
    void requestWithoutTokenContinuesToAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setServletPath("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void loginIsSkippedByTheFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/login");
        request.setServletPath("/api/v1/users/login");
        request.addHeader("Authorization", "Bearer irrelevant");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }
}
