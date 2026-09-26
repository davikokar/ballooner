package com.ballooner.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.ballooner.ui.comic.ComicFixture
import com.ballooner.ui.comic.ComicPage
import com.ballooner.ui.comic.PanelImageSource
import com.ballooner.ui.comic.comicFixtures
import com.ballooner.ui.theme.BalloonerTheme

/**
 * Debug-only gallery of rendered layout fixtures. It exists so panel geometry can be checked by
 * eye on a device before the editor is built, and is not part of the shipped app.
 */
class ComicRenderPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BalloonerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FixtureGallery()
                }
            }
        }
    }
}

@Composable
private fun FixtureGallery() {
    val bitmap = remember { checkerboard() }
    val images = remember(bitmap) { PanelImageSource { bitmap } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(comicFixtures()) { fixture -> FixtureCard(fixture, images) }
    }
}

@Composable
private fun FixtureCard(fixture: ComicFixture, images: PanelImageSource) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(fixture.name, style = MaterialTheme.typography.titleSmall)
        ComicPage(
            comic = fixture.comic,
            images = images,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(fixture.comic.pageShape.aspectRatio)
                .background(Color(0xFFDDDDDD)),
        )
    }
}
