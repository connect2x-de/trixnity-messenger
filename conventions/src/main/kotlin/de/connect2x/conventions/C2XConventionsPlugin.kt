package de.connect2x.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

open class C2XConventionsPlugin @Inject constructor(
    val providerFactory: ProviderFactory
) : Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.info("Using connect2x conventions plugin")
    }
}
