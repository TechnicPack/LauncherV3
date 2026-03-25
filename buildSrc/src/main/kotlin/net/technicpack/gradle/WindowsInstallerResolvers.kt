package net.technicpack.gradle

internal fun buildStableLauncherDownloadUrl(downloadUrl: String): String = downloadUrl.trim()

internal fun buildAdoptiumWindowsX64JreArchiveUrl(majorVersion: Int): String =
    "https://api.adoptium.net/v3/binary/latest/$majorVersion/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk"
