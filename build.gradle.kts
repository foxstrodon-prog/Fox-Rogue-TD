// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

tasks.register<Exec>("buildWindowsLauncher") {
  description = "Compiles the portable Windows .exe launcher"
  group = "distribution"
  workingDir = rootDir
  commandLine = listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", "tools/windows/build-launcher.ps1")
}

tasks.register<Exec>("uploadGitHubRelease") {
  description = "Compiles APK, Windows launcher, and publishes a new GitHub Release with binary assets"
  group = "distribution"
  dependsOn(":app:assembleDebug", "buildWindowsLauncher")
  workingDir = rootDir
  commandLine = listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", "tools/release.ps1", "-SkipBuild")
}

