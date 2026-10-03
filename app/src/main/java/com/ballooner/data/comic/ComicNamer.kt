package com.ballooner.data.comic

import android.content.Context
import com.ballooner.R
import com.ballooner.data.settings.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The names the app gives comics it makes on the user's behalf.
 *
 * These are read from the string resources, so they belong behind an interface: a ViewModel is
 * not allowed an Android `Context`, and the names still have to be in the user's language.
 */
interface ComicNamer {
    /** What the next comic is called when the user has not named one yet. */
    fun defaultName(existingComics: Int): String

    /** What a copy of [original] is called. */
    fun copyName(original: String): String
}

class AppComicNamer @Inject constructor(
    @ApplicationContext private val context: Context,
) : ComicNamer {

    override fun defaultName(existingComics: Int): String =
        localized().getString(R.string.comic_default_name, existingComics + 1)

    override fun copyName(original: String): String =
        localized().let { it.getString(R.string.comic_copy_name, original.ifBlank { it.getString(R.string.untitled) }) }

    // Read afresh each time: the application context was wrapped when the process started, so it
    // still speaks the old language after Settings changes it.
    private fun localized() = LocaleHelper.wrap(context)
}

/** What the test and debug-host constructors use, where no `Context` is to be had. */
internal object TestComicNamer : ComicNamer {
    override fun defaultName(existingComics: Int) = "My Comic ${existingComics + 1}"

    override fun copyName(original: String) = "$original copy"
}
