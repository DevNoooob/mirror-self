package com.mirrorself.auth;

import com.mirrorself.config.MirrorProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** V0.1 single-owner authentication. No public sharing APIs are mounted. */
@Component
public final class OwnerAuthFilter extends OncePerRequestFilter {
    private final byte[] expected;

    public OwnerAuthFilter(MirrorProperties properties) {
        this.expected = properties.ownerToken().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("/api/v1/health".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String supplied = request.getHeader("X-Mirror-Token");
        if (supplied == null || !MessageDigest.isEqual(
                expected, supplied.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
