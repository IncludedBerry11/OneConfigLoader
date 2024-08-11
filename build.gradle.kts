import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("org.polyfrost.loom") version "1.6.polyfrost.3" apply false
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

allprojects {
    apply(plugin = "maven-publish")
    group = "cc.polyfrost"
    version = "1.0.0-beta17"
    repositories {
        mavenCentral()
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "releases"
                setUrl("https://repo.polyfrost.org/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            maven {
                name = "snapshots"
                setUrl("https://repo.polyfrost.org/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            maven {
                name = "private"
                setUrl("https://repo.polyfrost.org/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("private")
                }
            }
        }
    }

}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "com.github.johnrengelman.shadow")
    val common = project.name.contains("common")
    val loader = project.name.contains("loader")
    if (!common) {
        apply(plugin = "org.polyfrost.loom")
    }

    val shade: Configuration by configurations.creating {
        configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).get().extendsFrom(this)
        configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).get().extendsFrom(this)
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }

    if (!common) {
        configure<LoomGradleExtensionAPI> {
            forge {
                pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
            }
        }

        dependencies {
            "minecraft"("com.mojang:minecraft:1.8.9")
            "mappings"("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
            "forge"("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
            shade(project(":oneconfig-common"))
        }
    }

    if (loader) {
        dependencies {
            if (common) {
                "compileOnly"(project(":oneconfig-common"))
            } else {
                shade(project(":oneconfig-common-loader"))
            }
        }
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    if (!common) {
        tasks {
            withType(Jar::class) {
                archiveBaseName.set(project.name)
            }
            val shadowJar by named<ShadowJar>("shadowJar") {
                archiveClassifier.set("dev")
                configurations = listOf(shade)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                if (loader) {
                    relocate("cc.polyfrost.oneconfig.loader.stage0", "cc.polyfrost.oneconfig.loader")
                }
            }
            named<RemapJarTask>("remapJar") {
                inputFile.set(shadowJar.archiveFile)
                archiveClassifier.set("")
            }
            named<Jar>("jar") {
                dependsOn(shadowJar)
                archiveClassifier.set("")
                enabled = false
            }
        }
    }
}


