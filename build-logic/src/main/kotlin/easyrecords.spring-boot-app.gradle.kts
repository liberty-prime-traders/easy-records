plugins {
    id("easyrecords.spring-module")
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib")
}

jib {
    from {
        image = "eclipse-temurin:25-jre"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "ezraorina834/easy-records:${project.findProperty("imageTag") ?: "latest"}"
    }
}

// The consuming build sets the version after this plugin applies, and jib's tags are a plain Set, not a Property.
afterEvaluate {
    jib.to.tags = setOf(version.toString())
}
