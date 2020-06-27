plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.3.7")
    implementation(group = "dev.dirs", name = "directories", version = "20")
    implementation(group = "org.apache.lucene", name = "lucene-core", version = "8.5.2")
    implementation(group = "org.apache.lucene", name = "lucene-queryparser", version = "8.5.2")
    implementation(group = "org.apache.tika", name = "tika-core", version = "1.24.1")
    implementation(group = "org.apache.tika", name = "tika-parsers", version = "1.24.1")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "2.0.0-alpha1")
    runtimeOnly(group = "org.slf4j", name = "slf4j-simple", version = "2.0.0-alpha1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    shadowJar {
        manifest {
            attributes["Main-Class"] = "org.example.mediaindex.MainKt"
            attributes["Implementation-Title"] = "Music Media Indexer"
        }
    }
}
