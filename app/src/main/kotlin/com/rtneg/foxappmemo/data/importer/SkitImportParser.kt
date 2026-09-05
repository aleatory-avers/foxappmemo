package com.rtneg.foxappmemo.data.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser for app-catalog exports in the Skit JSON format.
 *
 * Accepted shape (fields beyond the core ones are ignored):
 *
 * ```json
 * {
 *   "version": 1,
 *   "title": "...",
 *   "date": 1788588208036,
 *   "apps": [
 *     {
 *       "title": "Gambonanza",
 *       "packageName": "com.strayfawnstudio.gambonanza",
 *       "isSystem": false,
 *       "version": "1.3.8",
 *       "versionCode": 69,
 *       "apkSize": 269879808,
 *       "cacheSize": 0,
 *       "dataSize": 0,
 *       "lastUpdateTime": 1788586477248,
 *       "minSDK": 23,
 *       "targetSDK": 36,
 *       "installTime": 1788586477248,
 *       "enabled": true
 *     }
 *   ]
 * }
 * ```
 *
 * Both the top-level object and a bare array of app objects are accepted
 * (Skit versions vary in whether they wrap the export).
 */
object SkitImportParser {

    /** True when the app carries the system flag (or is otherwise bloatware). */
    @Serializable
    data class SkitApp(
        @SerialName("title") val title: String? = null,
        @SerialName("packageName") val packageName: String,
        @SerialName("isSystem") val isSystem: Boolean = false,
        @SerialName("version") val version: String? = null,
        @SerialName("versionCode") val versionCode: Long? = null,
        @SerialName("apkSize") val apkSize: Long? = null,
        @SerialName("cacheSize") val cacheSize: Long? = null,
        @SerialName("dataSize") val dataSize: Long? = null,
        @SerialName("lastUpdateTime") val lastUpdateTime: Long? = null,
        @SerialName("minSDK") val minSdk: Int? = null,
        @SerialName("targetSDK") val targetSdk: Int? = null,
        @SerialName("installTime") val installTime: Long? = null,
        @SerialName("enabled") val enabled: Boolean = true,
    )

    @Serializable
    private data class SkitExport(
        @SerialName("version") val version: Int? = null,
        @SerialName("title") val title: String? = null,
        @SerialName("date") val date: Long? = null,
        @SerialName("apps") val apps: List<SkitApp> = emptyList(),
    )

    /** Result of parsing + validating an import file. */
    data class ParseResult(
        val apps: List<SkitApp>,
        val skipped: Int,
        val errors: List<String>,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(text: String): ParseResult {
        val errors = mutableListOf<String>()
        val apps = mutableListOf<SkitApp>()
        var skipped = 0

        val entries: List<SkitApp> = try {
            // Try the wrapped export object first.
            val wrapped = try {
                json.decodeFromString<SkitExport>(text)
            } catch (_: Exception) {
                null
            }
            if (wrapped != null && wrapped.apps.isNotEmpty()) {
                wrapped.apps
            } else {
                // Fall back to a bare array of app objects.
                json.decodeFromString<List<SkitApp>>(text)
            }
        } catch (e: Exception) {
            return ParseResult(emptyList(), 0, listOf("Not a valid Skit-format file: ${e.message}"))
        }

        val seen = mutableSetOf<String>()
        for (app in entries) {
            val pkg = app.packageName.trim()
            if (pkg.isEmpty()) {
                skipped++
                continue
            }
            if (!seen.add(pkg)) {
                // Duplicate package in the same file — keep the first occurrence.
                skipped++
                continue
            }
            apps.add(app.copy(packageName = pkg))
        }

        return ParseResult(apps, skipped, errors)
    }
}
