import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.ksp)
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("android-app-dependencies")
    id("test-app-dependencies")
    id("jacoco-app-dependencies")
}

repositories {
    mavenCentral()
    google()
}

fun generateGitBuild(): String {
    val stringBuilder: StringBuilder = StringBuilder()
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--always")
            standardOutput = stdout
        }
        val commitObject = stdout.toString().trim()
        stringBuilder.append(commitObject)
    } catch (ignored: Exception) {
        stringBuilder.append("NoGitSystemAvailable")
    }
    return stringBuilder.toString()
}

fun generateGitRemote(): String {
    val stringBuilder: StringBuilder = StringBuilder()
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "remote", "get-url", "origin")
            standardOutput = stdout
        }
        val commitObject: String = stdout.toString().trim()
        stringBuilder.append(commitObject)
    } catch (ignored: Exception) {
        stringBuilder.append("NoGitSystemAvailable")
    }
    return stringBuilder.toString()
}

fun generateDate(): String {
    val stringBuilder: StringBuilder = StringBuilder()
    // showing only date prevents app to rebuild everytime
    stringBuilder.append(SimpleDateFormat("yyyy.MM.dd").format(Date()))
    return stringBuilder.toString()
}

fun isMaster(): Boolean = !Versions.appVersion.contains("-")

fun gitAvailable(): Boolean {
    val stringBuilder: StringBuilder = StringBuilder()
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "--version")
            standardOutput = stdout
        }
        val commitObject = stdout.toString().trim()
        stringBuilder.append(commitObject)
    } catch (ignored: Exception) {
        return false // NoGitSystemAvailable
    }
    return stringBuilder.toString().isNotEmpty()

}

fun allCommitted(): Boolean {
    val stringBuilder: StringBuilder = StringBuilder()
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "status", "-s")
            standardOutput = stdout
        }
        // ignore all changes done in .idea/codeStyles
        val cleanedList: String = stdout.toString().replace(Regex("""(?m)^\s*(M|A|D|\?\?)\s*.*?\.idea\/codeStyles\/.*?\s*$"""), "")
            // ignore all files added to project dir but not staged/known to GIT
            .replace(Regex("""(?m)^\s*(\?\?)\s*.*?\s*$"""), "")
        stringBuilder.append(cleanedList.trim())
    } catch (ignored: Exception) {
        return false // NoGitSystemAvailable
    }
    return stringBuilder.toString().isEmpty()
}

val keyProps = Properties()
val keyPropsFile: File = rootProject.file("keystore/keystore.properties")
keyProps.load(FileInputStream(keyPropsFile))
fun getStoreFile(): String {
    var storeFile = keyProps["storeFile"].toString()
    if (storeFile.isEmpty()) {
        storeFile = System.getenv("storeFile") ?: ""
    }
    return storeFile
}

fun getStorePassword(): String {
    var storePassword = keyProps["storePassword"].toString()
    if (storePassword.isEmpty()) {
        storePassword = System.getenv("storePassword") ?: ""
    }
    return storePassword
}

fun getKeyAlias(): String {
    var keyAlias = keyProps["keyAlias"].toString()
    if (keyAlias.isEmpty()) {
        keyAlias = System.getenv("keyAlias") ?: ""
    }
    return keyAlias
}

fun getKeyPassword(): String {
    var keyPassword = keyProps["keyPassword"].toString()
    if (keyPassword.isEmpty()) {
        keyPassword = System.getenv("keyPassword") ?: ""
    }
    return keyPassword
}


android {

    namespace = "app.aaps"
    ndkVersion = Versions.ndkVersion

    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        buildConfigField("String", "VERSION", "\"$version\"")
        buildConfigField("String", "BUILDVERSION", "\"${generateGitBuild()}-${generateDate()}\"")
        buildConfigField("String", "REMOTE", "\"${generateGitRemote()}\"")
        buildConfigField("String", "HEAD", "\"${generateGitBuild()}\"")
        buildConfigField("String", "COMMITTED", "\"${allCommitted()}\"")

        // For Dagger injected instrumentation tests in app module
        testInstrumentationRunner = "app.aaps.runners.InjectedTestRunner"
    }

    flavorDimensions.add("standard")
    productFlavors {
        create("full") {
            isDefault = true
            applicationId = "info.nightscout.androidaps"
            dimension = "standard"
            resValue("string", "app_name", "AAPS")
            versionName = Versions.appVersion
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
        }
        create("pumpcontrol") {
            applicationId = "info.nightscout.aapspumpcontrol"
            dimension = "standard"
            resValue("string", "app_name", "Pumpcontrol")
            versionName = Versions.appVersion + "-pumpcontrol"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_pumpcontrol"
            manifestPlaceholders["appIconRound"] = "@null"
        }
        create("aapsclient") {
            applicationId = "info.nightscout.aapsclient"
            dimension = "standard"
            resValue("string", "app_name", "AAPSClient")
            versionName = Versions.appVersion + "-aapsclient"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_yellowowl"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_yellowowl"
        }
        create("aapsclient2") {
            applicationId = "info.nightscout.aapsclient2"
            dimension = "standard"
            resValue("string", "app_name", "AAPSClient2")
            versionName = Versions.appVersion + "-aapsclient"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_blueowl"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_blueowl"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(getStoreFile())
            storePassword = getStorePassword()
            keyAlias = getKeyAlias()
            keyPassword = getKeyPassword()
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }

    }

    useLibrary("org.apache.http.legacy")

    //Deleting it causes a binding error
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

allprojects {
    repositories {
    }
}

dependencies {
    // in order to use internet"s versions you"d need to enable Jetifier again
    // https://github.com/nightscout/graphview.git
    // https://github.com/nightscout/iconify.git
    implementation(project(":shared:impl"))
    implementation(project(":core:data"))
    implementation(project(":core:objects"))
    implementation(project(":core:graph"))
    implementation(project(":core:graphview"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:keys"))
    implementation(project(":core:libraries"))
    implementation(project(":core:nssdk"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":core:validators"))
    implementation(project(":ui"))
    implementation(project(":plugins:aps"))
    implementation(project(":plugins:automation"))
    implementation(project(":plugins:configuration"))
    implementation(project(":plugins:constraints"))
    implementation(project(":plugins:insulin"))
    implementation(project(":plugins:main"))
    implementation(project(":plugins:sensitivity"))
    implementation(project(":plugins:smoothing"))
    implementation(project(":plugins:source"))
    implementation(project(":plugins:sync"))
    implementation(project(":implementation"))
    implementation(project(":database:impl"))
    implementation(project(":database:persistence"))
    implementation(project(":pump:combov2"))
    implementation(project(":pump:dana"))
    implementation(project(":pump:danars"))
    implementation(project(":pump:danar"))
    implementation(project(":pump:diaconn"))
    implementation(project(":pump:eopatch"))
    implementation(project(":pump:medtrum"))
    implementation(project(":pump:equil"))
    implementation(project(":pump:insight"))
    implementation(project(":pump:medtronic"))
    implementation(project(":pump:pump-common"))
    implementation(project(":pump:omnipod-common"))
    implementation(project(":pump:omnipod-eros"))
    implementation(project(":pump:omnipod-dash"))
    implementation(project(":pump:rileylink"))
    implementation(project(":pump:virtual"))
    implementation(project(":workflow"))

    testImplementation(project(":shared:tests"))
    androidTestImplementation(project(":shared:tests"))
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.org.skyscreamer.jsonassert)


    kspAndroidTest(libs.com.google.dagger.android.processor)

    /* Dagger2 - We are going to use dagger.android which includes
     * support for Activity and fragment injection so we need to include
     * the following dependencies */
    ksp(libs.com.google.dagger.android.processor)
    ksp(libs.com.google.dagger.compiler)

    // MainApp
    api(libs.com.uber.rxdogtag2.rxdogtag)
}

println("-------------------")
println("isMaster: ${isMaster()}")
println("gitAvailable: ${gitAvailable()}")
println("allCommitted: ${allCommitted()}")
println("-------------------")
