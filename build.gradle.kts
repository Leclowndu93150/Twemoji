plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.5"

prism {
    metadata {
        modId = "twemoji"
        name = "Twemoji"
        description = "Pixelated Twemoji emojis in chat — with a picker, shortcodes, animated emojis, and datapack support."
        license = "All Rights Reserved"
    }

//    skidfuscate()
//    obfuscate()

    curseMaven()

    publishing {
        changelogFile = "CHANGELOG.md"

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1543001"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "aCiQlU58"
        }
    }

    version("1.20.1") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.92.9+1.20.1")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:5162837")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244418")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
                optional("chat_heads")
            }
        }
        forge {
            loaderVersion = "47.4.10"
            dependencies {
                modRuntimeOnly("curse.maven:chat-heads-407206:8244416")
            }
        }
    }

    version("1.21.1") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.116.12+1.21.1")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:7808443")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244450")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
                optional("chat_heads")
            }
        }
        neoforge {
            loaderVersion = "21.1.228"
            dependencies {
                modRuntimeOnly("curse.maven:chat-heads-407206:8244448")
            }
        }
    }

    version("1.21.11") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.141.4+1.21.11")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:7808841")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174612")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
                optional("chat_heads")
            }
        }
        neoforge {
            loaderVersion = "21.11.42"
            dependencies {
                modRuntimeOnly("curse.maven:chat-heads-407206:8174609")
            }
        }
    }

    version("26.1.2") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.148.0+26.1.2")

            dependencies {
                implementation("curse.maven:modmenu-308702:8065321")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174615")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
                optional("chat_heads")
            }
        }
        neoforge {
            loaderVersion = "26.1.2.41-beta"
            dependencies {
                modRuntimeOnly("curse.maven:chat-heads-407206:8174614")
            }
        }
    }

}
