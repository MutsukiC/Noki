import javax.inject.Inject
import java.util.Properties
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.apktransform)
}

val versionCodeFromGit by extra(getAppVersionCode())
val versionNameFromGit by extra(getAppVersionName())

abstract class GitHelper @Inject constructor(
    private val execOps: ExecOperations
) {
    fun getGitCommitCount(): Int {
        val out = ByteArrayOutputStream()
        execOps.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = out
        }
        return out.toString().trim().toInt()
    }

    fun getGitDescribe(): String {
        val out = ByteArrayOutputStream()
        execOps.exec {
            commandLine("git", "describe", "--tags", "--always")
            standardOutput = out
        }
        return out.toString().trim()
    }
}

fun getAppVersionCode(): Int {
    val gitHelper = objects.newInstance(GitHelper::class.java)
    val commitCount = gitHelper.getGitCommitCount()
    return commitCount
}

fun getAppVersionName(): String {
    val gitHelper = objects.newInstance(GitHelper::class.java)
    return gitHelper.getGitDescribe()
}

val keystorePropertiesFile: File = rootProject.file("key.properties")
val keystoreProperties = if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) {
    Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
} else null

apktransform {
    copy {
        when (it.buildType) {
            "release" -> file("${it.name}/noki.${versionNameFromGit}.apk")
            "debug" -> file("${it.name}/noki.${versionNameFromGit}-debug.apk")
            else -> null
        }
    }
}

android {
    namespace = "moe.lar.noki"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.kmi.noki"
        minSdk = 30
        targetSdk = 36
        versionCode = versionCodeFromGit
        versionName = versionNameFromGit

    }
    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                "proguard-rules.pro"
            )
            val releaseSig = signingConfigs.findByName("release")
            signingConfig = if (releaseSig != null) releaseSig else {
                println("Use debug signing config")
                signingConfigs["debug"]
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
}