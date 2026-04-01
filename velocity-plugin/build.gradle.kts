plugins {
    java
    id("com.gradleup.shadow")
}

group = "space.blockway.social"
version = "1.0.0"

configurations {
    annotationProcessor
}

dependencies {
    // Shared module
    implementation(project(":shared"))

    // Velocity API (compileOnly — provided by proxy)
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    // Adventure (bundled in Velocity, compileOnly)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // Javalin 7 — embedded HTTP server (shaded)
    implementation("io.javalin:javalin:6.3.0")

    // SLF4J — provided by Velocity, must NOT be shaded
    compileOnly("org.slf4j:slf4j-api:2.0.16")

    // Jackson — REST API JSON (shaded)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")

    // HikariCP — connection pooling (shaded)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // SQLite JDBC (shaded)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // MySQL Connector/J (shaded)
    implementation("com.mysql:mysql-connector-j:9.1.0")

    // SnakeYAML for config parsing (shaded)
    implementation("org.yaml:snakeyaml:2.3")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("blockway-social-velocity")

    // Relocate all shaded packages to avoid classloader conflicts
    relocate("io.javalin", "space.blockway.social.shadow.javalin")
    relocate("org.eclipse.jetty", "space.blockway.social.shadow.jetty")
    relocate("jakarta", "space.blockway.social.shadow.jakarta")
    relocate("com.fasterxml.jackson", "space.blockway.social.shadow.jackson")
    relocate("com.zaxxer.hikari", "space.blockway.social.shadow.hikari")
    relocate("org.sqlite", "space.blockway.social.shadow.sqlite")
    relocate("org.xerial", "space.blockway.social.shadow.xerial")
    relocate("com.mysql", "space.blockway.social.shadow.mysql")
    relocate("org.yaml.snakeyaml", "space.blockway.social.shadow.snakeyaml")

    // Exclude SLF4J from the fat JAR — Velocity provides it at runtime
    exclude("org/slf4j/**")
    exclude("META-INF/services/org.slf4j.*")

    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
