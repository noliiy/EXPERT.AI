import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("application")
    id("java")
}

group = "com.experts"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Discord API
    implementation("net.dv8tion:JDA:5.0.0-beta.23")

    // JSON con Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Cliente HTTP para OpenAI
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.5.4")

    // Connection Pool
    implementation("com.zaxxer:HikariCP:5.0.1")

    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox:2.0.30")
}

application {
    // Clase principal del bot
    mainClass.set("bot.BotMain")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// ✅ Soporte para emojis y caracteres especiales (UTF-8)
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
