// minecraft-plugin-menu — the in-game per-player menu UI (/menu).
// Depends on core as `compileOnly`: core is a separate plugin on the server, so
// we never bundle it. This plugin holds no state of its own — it renders the
// settings other plugins register on core's SettingsRegistry and writes the
// player's choices through core's PlayerPreferenceService (the central DB).
// The shaded jar only relocates third-party libs (none yet).

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":minecraft-plugin-core"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // relocate("com.example.shadedlib", "com.mrfermz.mcplugins.menu.libs.shadedlib")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
