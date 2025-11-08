// OWASP Dependency-Check Configuration
dependencyCheck {
    // Minimal configuration - most features work with defaults
    format = "ALL"
}

// Ktlint Configuration
ktlint {
    android = true
}

// Add ktlint check to build task
tasks.check.dependsOn("ktlintCheck")
