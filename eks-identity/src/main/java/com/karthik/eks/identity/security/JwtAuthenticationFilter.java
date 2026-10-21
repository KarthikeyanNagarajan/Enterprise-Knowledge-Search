package com.karthik.eks.identity.security;

import com.karthik.eks.identity.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter - Validates JWT tokens on every request.
 * 
 * This filter:
 * - Extracts JWT token from Authorization header (Bearer <token>)
 * - Validates token signature and expiration
 * - Extracts userId and tenantId from token claims
 * - Sets SecurityContext with authenticated user
 * - Runs ONCE per request regardless of servlet dispatching
 * 
 * Flow:
 * 1. Extract Authorization header
 * 2. Check for "Bearer " prefix
 * 3. Validate token with JwtTokenProvider
 * 4. Load user from database
 * 5. Set SecurityContext with user details and authorities
 * 6. Continue chain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractTokenFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                // Extract username and IDs from token
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);

                log.debug("Token validated for user: {} (tenantId: {})", username, tenantId);

                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails != null) {
                    // Create authentication token with user details and authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Store userId and tenantId in request attributes for later access
                    request.setAttribute("userId", userId);
                    request.setAttribute("tenantId", tenantId);
                    request.setAttribute("username", username);

                    log.debug("SecurityContext set for user: {} with roles: {}", username, userDetails.getAuthorities());
                } else {
                    log.warn("User details not found for username: {}", username);
                }
            } else if (jwt != null) {
                log.warn("Invalid or expired JWT token");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in the security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Authorization: Bearer <token>"
     * 
     * @param request the HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
