package com.karthik.eks.identity.security;

import com.karthik.eks.identity.entity.User;
import com.karthik.eks.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Custom UserDetailsService implementation.
 * 
 * Loads user details from the database for Spring Security integration.
 * Used by:
 * - AuthenticationManager during login
 * - JWT filter to populate SecurityContext
 * 
 * Returns a UserDetails object with username, password, authorities, and account status.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username from the database.
     * 
     * @param username the username to load
     * @return UserDetails object with user information
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.getIsActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                getAuthorities(user)
        );
    }

    /**
     * Convert User role to Spring Security authorities.
     * 
     * @param user the user entity
     * @return list of GrantedAuthority
     */
    private List<GrantedAuthority> getAuthorities(User user) {
        String authority = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}
