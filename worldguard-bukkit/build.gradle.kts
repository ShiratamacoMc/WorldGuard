import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    `java-library`
    id("buildlogic.platform")
}

dependencies {
    "api"(project(":worldguard-core"))
    "api"(libs.worldedit.bukkit) { isTransitive = false }
    "compileOnly"(libs.commandbook) { isTransitive = false }

    "compileOnly"(libs.jetbrains.annotations) {
        because("Resolving Spigot annotations")
    }
    "testCompileOnly"(libs.jetbrains.annotations) {
        because("Resolving Spigot annotations")
    }
    "compileOnly"(libs.paperApi) {
        exclude("org.slf4j", "slf4j-api")
        exclude("junit", "junit")
    }

    "implementation"(libs.paperLib)
    "implementation"(libs.bstats.bukkit)
    
    // Adventure dependencies for MiniMessage support
    "implementation"(libs.adventure.api)
    "implementation"(libs.adventure.text.minimessage)
    "implementation"(libs.adventure.platform.bukkit)
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand("internalVersion" to internalVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency(":worldguard-core"))
        include(dependency("org.bstats:"))
        include(dependency("io.papermc:paperlib"))
        
        // Adventure Core
        include(dependency("net.kyori:adventure-api"))
        include(dependency("net.kyori:adventure-key"))
        include(dependency("net.kyori:adventure-nbt"))
        include(dependency("net.kyori:adventure-text-minimessage"))
        
        // Adventure Platform
        include(dependency("net.kyori:adventure-platform-bukkit"))
        include(dependency("net.kyori:adventure-platform-api"))
        include(dependency("net.kyori:adventure-platform-facet"))
        include(dependency("net.kyori:adventure-platform-viaversion"))
        
        // Adventure Serializers
        include(dependency("net.kyori:adventure-text-serializer-legacy"))
        include(dependency("net.kyori:adventure-text-serializer-gson"))
        include(dependency("net.kyori:adventure-text-serializer-gson-legacy-impl"))
        include(dependency("net.kyori:adventure-text-serializer-bungeecord"))
        include(dependency("net.kyori:adventure-text-serializer-commons"))
        include(dependency("net.kyori:adventure-text-serializer-json"))
        
        // Examination
        include(dependency("net.kyori:examination-api"))
        include(dependency("net.kyori:examination-string"))

        relocate("org.bstats", "com.sk89q.worldguard.bukkit.bstats")
        relocate("io.papermc.lib", "com.sk89q.worldguard.bukkit.paperlib")
        relocate("net.kyori", "com.sk89q.worldguard.bukkit.libs.kyori")
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}

// Disable checkstyle and test tasks
tasks.withType<Checkstyle>().configureEach {
    enabled = false
}

tasks.named<Test>("test") {
    enabled = false
}