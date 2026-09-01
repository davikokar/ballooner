package com.ballooner.ui.project

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSharingTest {

    @Test
    fun `share filename replaces unsafe characters and uses jpg extension`() {
        val projectName = " My / Comic? "

        val result = shareFileName(projectName)

        assertEquals("My_Comic.jpg", result)
    }

    @Test
    fun `share filename falls back to comic when title is blank`() {
        val projectName = "   "

        val result = shareFileName(projectName)

        assertEquals("comic.jpg", result)
    }
}