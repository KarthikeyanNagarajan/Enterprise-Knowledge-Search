package com.karthik.eks.common.config;

/**
 * Security configuration is owned by the identity module to keep the common module
 * free of application-specific security dependencies.
 *
 * This placeholder avoids a circular dependency between the common and identity
 * modules while preserving the build for shared library usage.
 */
public final class SecurityConfiguration {
    private SecurityConfiguration() {
    }
}
