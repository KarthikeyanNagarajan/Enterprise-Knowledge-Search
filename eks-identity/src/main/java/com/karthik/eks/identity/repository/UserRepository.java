package com.karthik.eks.identity.repository;

import com.karthik.eks.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 * 
 * Provides database access methods using Spring Data JPA.
 * JpaRepository provides standard CRUD operations:
 * - findAll(), findById(), save(), delete(), etc.
 * 
 * Custom methods:
 * - findByUsername: Find user by username (for login)
 * - findByEmail: Find user by email (for password reset)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     * 
     * @param username the username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     * 
     * @param email the user's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by username.
     * 
     * @param username the username
     * @return true if user exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if user exists by email.
     * 
     * @param email the email
     * @return true if user exists
     */
    boolean existsByEmail(String email);
}
