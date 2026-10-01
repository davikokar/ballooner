package com.ballooner.data.comic

import android.content.Context
import androidx.room.Room
import com.ballooner.data.AppDatabase
import com.ballooner.data.MIGRATION_10_11
import com.ballooner.data.MIGRATION_11_12
import com.ballooner.data.MIGRATION_6_7
import com.ballooner.data.MIGRATION_7_8
import com.ballooner.data.MIGRATION_8_9
import com.ballooner.data.MIGRATION_9_10
import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Cut
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.model.BalloonType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Exercises the comic tables against real SQLite, including the migration that adds them, which
 * is the part that fails loudly on a user's device rather than in a pure unit test.
 */
@RunWith(RobolectricTestRunner::class)
class ComicDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ComicRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun openDatabase(): AppDatabase {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
            )
            .build()
        repository = RoomComicRepository(database.comicDao())
        return database
    }

    /** Builds the schema as version 6 left it, so the migration has something real to upgrade. */
    private fun createVersion6Database() {
        val db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)
        VERSION_6_SCHEMA.forEach(db::execSQL)
        db.execSQL(
            "INSERT INTO `project` (`name`, `description`, `createdAt`, `updatedAt`, `imageUri`) " +
                "VALUES ('Old comic', '', 1, 1, 'file://old.png')",
        )
        db.version = 6
        db.close()
    }

    @Test
    fun `migrating an old database brings it to the comic tables and clears out the rest`() = runTest {
        createVersion6Database()

        openDatabase()
        val id = repository.createComic(Comic(name = "After migration"))

        assertEquals("After migration", repository.observeComic(id).first()?.name)
        // The flattened-image editor is gone, so its tables go with it.
        listOf("project", "panel", "balloon").forEach { table ->
            database.openHelper.readableDatabase
                .query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'")
                .use { assertEquals("$table should have been dropped", 0, it.count) }
        }
    }

    @Test
    fun `a whole document survives a round trip through the database`() = runTest {
        openDatabase()
        val comic = Comic(
            name = "Round trip",
            sizing = PageSizing.Ratio(WIDE_RATIO),
            style = ComicStyle(gutter = 0.02f, borderThickness = 0.008f, cornerRadius = 0.01f),
            layout = Layout(
                grid = Grid(
                    rows = 2,
                    columns = 3,
                    rowWeights = listOf(2f, 1f),
                    columnWeights = listOf(1f, 1.5f, 2f),
                    spans = listOf(Span(0, 0, rowCount = 1, columnCount = 2)),
                ),
                cuts = listOf(
                    Cut(NormalizedPoint(0f, 0.3f), NormalizedPoint(1f, 0.7f), CutScope.WholePage),
                    Cut(
                        a = NormalizedPoint(0.5f, 0f),
                        b = NormalizedPoint(0.5f, 1f),
                        scope = CutScope.AtPoint(NormalizedPoint(0.2f, 0.2f)),
                    ),
                ),
            ),
            panels = listOf(
                Panel(PanelImage("file://a", NormalizedPoint(0.3f, 0.6f), zoom = 1.75f, angleDegrees = 22f)),
                Panel(),
            ),
            balloons = listOf(
                Balloon(
                    id = 1,
                    type = BalloonType.SPEAK,
                    scope = BalloonScope.Panel(0),
                    text = "Hello",
                    borderThickness = 0.013f,
                    matchPanelBorder = false,
                ),
                Balloon(id = 2, type = BalloonType.CAPTION, scope = BalloonScope.Comic, text = "Later"),
            ),
        )

        val id = repository.createComic(comic)

        assertEquals(comic, repository.observeComic(id).first())
    }

    @Test
    fun `saving replaces the parts of a comic rather than adding to them`() = runTest {
        openDatabase()
        val id = repository.createComic(
            Comic(
                layout = Layout(Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, 1, 2)))),
                panels = List(3) { Panel(PanelImage("image$it")) },
            ),
        )

        repository.saveComic(id, Comic(name = "Replaced"))

        val restored = repository.observeComic(id).first()!!
        assertEquals("Replaced", restored.name)
        assertEquals(emptyList<Span>(), restored.layout.grid.spans)
        assertEquals(1, restored.panels.size)
        assertNull(restored.panels.single().image)
    }

    @Test
    fun `grid weights survive the converter and sqlite`() = runTest {
        openDatabase()
        val weights = listOf(1f, 2.5f, 0.25f)

        val id = repository.createComic(
            Comic(layout = Layout(Grid(rows = 1, columns = 3, columnWeights = weights))),
        )

        assertEquals(weights, repository.observeComic(id).first()?.layout?.grid?.columnWeights)
    }

    @Test
    fun `deleting a comic takes its panels balloons and cuts with it`() = runTest {
        openDatabase()
        val id = repository.createComic(
            Comic(
                layout = Layout(
                    grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, 1, 2))),
                    cuts = listOf(Cut(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), CutScope.WholePage)),
                ),
                panels = List(4) { Panel(PanelImage("image$it")) },
                balloons = listOf(Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Comic)),
            ),
        )
        assertNotNull(repository.observeComic(id).first())

        repository.deleteComic(id)

        assertNull(repository.observeComic(id).first())
        listOf("comic_span", "comic_cut", "comic_panel", "comic_balloon").forEach { table ->
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
                it.moveToFirst()
                assertEquals("$table should be empty", 0, it.getInt(0))
            }
        }
    }

    @Test
    fun `the comic list reports every saved comic`() = runTest {
        openDatabase()

        repository.createComic(Comic(name = "First"))
        repository.createComic(Comic(name = "Second"))

        assertEquals(setOf("First", "Second"), repository.observeComics().first().map { it.name }.toSet())
    }

    @Test
    fun `saving a comic that does not exist does nothing`() = runTest {
        openDatabase()

        repository.saveComic(id = 404, comic = Comic(name = "Ghost"))

        assertNull(repository.observeComic(404).first())
    }
}

private const val DB_NAME = "ballooner-test.db"

private val VERSION_6_SCHEMA = listOf(
    "CREATE TABLE IF NOT EXISTS `project` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `imageUri` TEXT)",
    "CREATE TABLE IF NOT EXISTS `balloon` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`projectId` INTEGER NOT NULL, `type` TEXT NOT NULL, `text` TEXT NOT NULL, " +
        "`centerX` REAL NOT NULL, `centerY` REAL NOT NULL, `width` REAL NOT NULL, " +
        "`height` REAL NOT NULL, `tailAngleDegrees` REAL NOT NULL, `tailLength` REAL NOT NULL, " +
        "`cornerRoundness` REAL NOT NULL DEFAULT 1.0, `tailWidth` REAL NOT NULL DEFAULT 0.5, " +
        "`fontSize` REAL NOT NULL DEFAULT 14.0, `font` TEXT NOT NULL DEFAULT 'DEFAULT', " +
        "FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_balloon_projectId` ON `balloon` (`projectId`)",
    "CREATE TABLE IF NOT EXISTS `panel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`projectId` INTEGER NOT NULL, `left` REAL NOT NULL, `top` REAL NOT NULL, " +
        "`width` REAL NOT NULL, `height` REAL NOT NULL, " +
        "FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_panel_projectId` ON `panel` (`projectId`)",
    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
    "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
        "VALUES(42, '8eee54f58fce40354108155836f3fd27')",
)
