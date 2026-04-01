plugins {
    java
    id("com.gradleup.shadow")
}

group = "space.blockway.social"
version = "1.0.0"

dependencies {
    // Shared module (shaded in for standalone deployment)
    implementation(project(":shared"))

    // Paper API (compileOnly — provided by server)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Adventure (bundled in Paper, compileOnly)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // Jackson annotations (compileOnly — only for shared DTOs)
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.18.2")

    // Gson (compileOnly — bundled in Paper, used for plugin messaging serialization)
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("blockway-social-paper")
    // Only shades the shared module — no external dep conflicts to worry about
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
