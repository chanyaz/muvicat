package xyz.arnau.muvicat.cache

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PreferencesHelper @Inject constructor(context: Context) {

    companion object {
        const val PREF_BUFFER_PACKAGE_NAME = "xyz.arnau.muvicat.preferences"

        const val PREF_KEY_LAST_CACHE = "last_cache"
    }

    private val bufferPref: SharedPreferences

    init {
        bufferPref = context.getSharedPreferences(PREF_BUFFER_PACKAGE_NAME, Context.MODE_PRIVATE)
    }

    open var lastCacheTime: Long
        get() = bufferPref.getLong(PREF_KEY_LAST_CACHE, 0)
        set(lastCache) = bufferPref.edit().putLong(PREF_KEY_LAST_CACHE, lastCache).apply()
}
