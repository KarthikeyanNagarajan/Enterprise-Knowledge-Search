package com.karthik.eks.identity.entity;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * User entity representing a system user.
 * 
 * Fields:
 * - username: Unique identifier for login
 * - email: User's email address (unique, validated)
 * - passwordHash: BCrypt hashed password (never store plain text)
 * - firstName: User's first name
 * - lastName: User's last name
 * - role: User's role (ADMIN, USER, VIEWER)
 * - isActive: Whether user account is active
 * 
 * Audit fields inherited from BaseEntity:
 * - id, createdAt, updatedAt, createdBy, updatedBy
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * User roles in the system.
     */
    public enum UserRole {
        ADMIN,      // Full system access
        USER,       // Normal user access
        VIEWER      // Read-only access
    }

    /**
     * Get user's full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
