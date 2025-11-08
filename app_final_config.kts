// OWASP Dependency-Check Configuration (minimal setup)
dependencyCheck {
    format = "ALL"
}

// Configure ktlint task
tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
    this.exclude {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask> {
    this.exclude {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Ensure ktlint runs on build
tasks.check.dependsOn("ktlintCheck")
