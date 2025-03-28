package de.connect2x.conventions

import org.gradle.api.publish.maven.MavenPomContributorSpec
import org.gradle.api.publish.maven.MavenPomDeveloperSpec

fun MavenPomDeveloperSpec.c2xDeveloper(id: String, name: String) {
    developer {
        this.id.set(id)
        this.name.set(name)
        this.url.set("https://gitlab.com/$id")
        this.organization.set("connect2x")
        this.organizationUrl.set("https://gitlab.com/connect2x")
    }
}

fun MavenPomContributorSpec.c2xContributor(id: String, name: String) {
    contributor {
        this.name.set(name)
        this.url.set("https://gitlab.com/$id")
        this.organization.set("connect2x")
        this.organizationUrl.set("https://gitlab.com/connect2x")
    }
}
