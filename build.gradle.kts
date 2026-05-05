plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.0"

prism {
    metadata {
        modId = "twemoji"
        name = "Twemoji"
        description = "A Minecraft mod."
        license = "All Rights Reserved"
    }

    version("26.1.2") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.148.0+26.1.2")
        }
        neoforge {
            loaderVersion = "26.1.2.41-beta"
            loaderVersionRange = "[4,)"
        }
    }

}
