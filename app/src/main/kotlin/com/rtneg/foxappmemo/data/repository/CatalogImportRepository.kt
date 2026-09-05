package com.rtneg.foxappmemo.data.repository

import com.rtneg.foxappmemo.data.db.AppDao
import com.rtneg.foxappmemo.data.db.AppDatabase
import com.rtneg.foxappmemo.data.db.TagDao
import com.rtneg.foxappmemo.data.entity.AppEntity
import com.rtneg.foxappmemo.data.entity.TagEntity
import com.rtneg.foxappmemo.data.importer.SkitImportParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Summary of one import run, for the snackbar/result UI. */
data class ImportResult(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val systemApps: Int,
    val errors: List<String> = emptyList(),
) {
    fun message(): String = buildString {
        append("Imported $imported")
        if (updated > 0) append(", refreshed $updated existing")
        if (systemApps > 0) append(" ($systemApps system)")
        if (skipped > 0) append(", skipped $skipped")
        append(".")
    }
}

/**
 * Imports app-catalog exports (Skit JSON format) into the memo database.
 *
 * Semantics:
 * - An app **not yet in the DB** is created with status TRYING and no rating;
 *   catalog metadata (version, size, SDKs, system flag) is populated.
 * - An app **already in the DB** keeps its memo / rating / status / tags and
 *   only gets its catalog metadata refreshed. Memos are never overwritten.
 * - System apps are imported (so the catalog is complete) but flagged, and
 *   can be filtered out of the main list.
 */
@Singleton
class CatalogImportRepository @Inject constructor(
    private val db: AppDatabase,
    private val appDao: AppDao,
    private val tagDao: TagDao,
) {
    suspend fun importFromSkitJson(text: String): ImportResult = withContext(Dispatchers.IO) {
        val parse = SkitImportParser.parse(text)
        if (parse.apps.isEmpty()) {
            return@withContext ImportResult(0, 0, parse.skipped, 0, parse.errors)
        }

        var imported = 0
        var updated = 0
        var systemApps = 0
        val now = System.currentTimeMillis()

        db.withTransaction {
            for (skitApp in parse.apps) {
                val existing = appDao.getApp(skitApp.packageName)
                val merged = if (existing == null) {
                    imported++
                    if (skitApp.isSystem) systemApps++
                    AppEntity(
                        packageName = skitApp.packageName,
                        appName = skitApp.title ?: skitApp.packageName,
                        installDate = skitApp.installTime,
                        status = AppEntityStatusDefault,
                        version = skitApp.version,
                        versionCode = skitApp.versionCode,
                        apkSize = skitApp.apkSize,
                        minSdk = skitApp.minSdk,
                        targetSdk = skitApp.targetSdk,
                        isSystem = skitApp.isSystem,
                        catalogUpdatedAt = now,
                    )
                } else {
                    updated++
                    if (skitApp.isSystem) systemApps++
                    // Preserve user data; refresh catalog metadata only.
                    existing.copy(
                        appName = existing.appName.ifBlank { skitApp.title ?: skitApp.packageName },
                        version = skitApp.version ?: existing.version,
                        versionCode = skitApp.versionCode ?: existing.versionCode,
                        apkSize = skitApp.apkSize ?: existing.apkSize,
                        minSdk = skitApp.minSdk ?: existing.minSdk,
                        targetSdk = skitApp.targetSdk ?: existing.targetSdk,
                        isSystem = skitApp.isSystem || existing.isSystem,
                        installDate = existing.installDate ?: skitApp.installTime,
                        catalogUpdatedAt = now,
                    )
                }
                appDao.insertApp(merged)
            }
        }

        ImportResult(imported, updated, parse.skipped, systemApps, parse.errors)
    }

    private companion object {
        const val AppEntityStatusDefault = "trying"
    }

    /** True when the import text looks like a catalog export worth attempting. */
    fun quickCheck(text: String): Boolean =
        text.contains("\"packageName\"") && text.contains("\"apps\"") ||
            text.contains("\"packageName\"")
}
