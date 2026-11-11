plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow").version("8.3.9")
}

group = "us.ajg0702"
version = "2.11.0-liso.1"

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.ajg0702.us/releases/") }
    maven { url = uri("https://repo.codemc.io/repository/nms/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://repo.citizensnpcs.co/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }

    mavenLocal()
}

dependencies {
    testImplementation("junit:junit:4.12")
    testRuntimeOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.xerial:sqlite-jdbc:3.32.3.2")
    compileOnly("org.spongepowered:configurate-yaml:4.0.0")

    implementation("net.kyori:adventure-api:4.26.1")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")

    implementation("us.ajg0702:ajUtils:1.2.38")
    implementation("us.ajg0702.commands.platforms.bukkit:bukkit:1.0.0")
    implementation("us.ajg0702.commands.api:api:1.0.0")

    compileOnly("net.luckperms:api:5.4")

    implementation("io.papermc:paperlib:1.0.7")

    implementation(project(":nms:nms-legacy"))
    implementation(project(":nms:nms-19"))


    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("com.h2database:h2:2.1.214")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    //implementation("io.prometheus", "simpleclient", "0.9.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<ProcessResources> {
    include("**/*.yml")
    include("**/*.prop")
    include("**/*.zip")
    filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf(
                    "VERSION" to project.version.toString()
            )
    )
}

tasks.shadowJar {
    relocate("org.h2", "us.ajg0702.leaderboards.libs.h2")
    relocate("com.zaxxer.hikari", "us.ajg0702.leaderboards.libs.hikari")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
    relocate("us.ajg0702.utils", "us.ajg0702.leaderboards.libs.utils")
    relocate("us.ajg0702.commands", "us.ajg0702.leaderboards.commands.base")
    relocate("net.kyori", "us.ajg0702.leaderboards.libs.kyori")
    relocate("org.bstats", "us.ajg0702.leaderboards.libs.bstats")
    relocate("org.spongepowered", "us.ajg0702.leaderboards.libs")
    relocate("org.yaml", "us.ajg0702.leaderboards.libs")
    relocate("io.leangen", "us.ajg0702.leaderboards.libs")
    relocate("io.papermc.lib", "us.ajg0702.leaderboards.libs.paperlib")
    relocate("com.squareup", "us.ajg0702.leaderboards.libs")
    relocate("okhttp3", "us.ajg0702.leaderboards.libs.okhttp3")
    relocate("okio", "us.ajg0702.leaderboards.libs.okio")
    relocate("org.intellij", "us.ajg0702.leaderboards.libs.intellij")
    relocate("org.jetbrains", "us.ajg0702.leaderboards.libs.jetbrains")
    relocate("kotlin", "us.ajg0702.leaderboards.kotlin")

    archiveBaseName.set("ajLeaderboards")
    archiveClassifier.set("")
    exclude("junit/**/*")
    exclude("org/junit/**/*")
    exclude("org/slf4j/**/*")
    exclude("org/hamcrest/**/*")
    exclude("LICENSE-junit.txt")

    // JDBC, Kotlin and HTTP resources are loaded reflectively. Keep bundled runtime
    // dependencies intact instead of relying on static reachability analysis.

    // This is set to prevent paper from remapping the jar.
    // This is an issue because paper's remapper has a bug around 1.21.1-1.21.4 that causes some plugins to error for no reason
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang+yarn"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks["jar"])
        }
    }

    repositories {

        val mavenUrl = "https://repo.ajg0702.us/releases"

        if(!System.getenv("REPO_TOKEN").isNullOrEmpty()) {
            maven {
                url = uri(mavenUrl)
                name = "ajRepo"

                credentials {
                    username = "plugins"
                    password = System.getenv("REPO_TOKEN")
                }
            }
        }
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val verifyBundledRuntime by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Loads dependencies from the final plugin JAR in an isolated classloader."
    dependsOn(tasks.shadowJar, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("us.ajg0702.leaderboards.BundledRuntimeSmoke")
    doFirst { args(tasks.shadowJar.get().archiveFile.get().asFile.absolutePath) }
}

tasks.check { dependsOn(verifyBundledRuntime) }
