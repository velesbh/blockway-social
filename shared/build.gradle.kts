plugins {
    java
}

group = "space.blockway.social"
version = "1.0.0"

dependencies {
    // Jackson annotations for DTOs (Velocity side will shade Jackson runtime)
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
    // Gson annotations for Paper side compatibility
    compileOnly("com.google.code.gson:gson:2.10.1")
}
