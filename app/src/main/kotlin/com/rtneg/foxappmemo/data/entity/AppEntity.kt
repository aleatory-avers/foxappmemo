package com.rtneg.foxappmemo.data.entity

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rtneg.foxappmemo.R
import kotlinx.serialization.Serializable

enum class AppRating(val value: Int, @StringRes val labelResId: Int) {
    DISLIKE(1, R.string.rating_not_for_me),
    NORMAL(2, R.string.rating_normal),
    LIKE(3, R.string.rating_like);

    companion object {
        fun fromValue(value: Int?): AppRating? = entries.firstOrNull { it.value == value }
    }
}

enum class AppStatus(val label: String, @StringRes val labelResId: Int) {
    TRYING("trying", R.string.status_trying),
    ONGOING("ongoing", R.string.status_ongoing),
    MAIN("main", R.string.status_main),
    AVOID("avoid", R.string.status_avoid),
    BLACKLIST("blacklist", R.string.status_blacklist),
    RECONSIDER("reconsider", R.string.status_reconsider);

    companion object {
        fun fromLabel(label: String): AppStatus =
            entries.firstOrNull { it.label == label } ?: TRYING
    }
}

@Serializable
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val installDate: Long? = null,
    val uninstallDate: Long? = null,
    val lastUsedDate: Long? = null,
    /** Rating as [AppRating.value] (1=dislike, 2=normal, 3=like), null if not rated */
    val rating: Int? = null,
    /** Stored as the [AppStatus.label] string */
    val status: String = AppStatus.TRYING.label,
    val memo: String? = null,
    // ── Catalog metadata (populated by Skit import / rescan) ──────────────────
    /** Version name as reported by the package manager / import source. */
    val version: String? = null,
    /** Version code as reported by the import source. */
    val versionCode: Long? = null,
    /** APK size in bytes. */
    val apkSize: Long? = null,
    /** Minimum supported Android SDK. */
    val minSdk: Int? = null,
    /** Target Android SDK. */
    val targetSdk: Int? = null,
    /** True when the import source flagged the app as a system app. */
    val isSystem: Boolean = false,
    /** Last time this catalog entry was refreshed from a package listing. */
    val catalogUpdatedAt: Long? = null,
)
