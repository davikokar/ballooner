---
name: add-room-entity
description: Add a new Room entity with its DAO, repository, and a database migration to the Ballooner Android app. Use when the user wants to persist a new kind of data.
---

# Add Room Entity

Use this skill to store a new kind of data. It keeps the data layer consistent:
entity -> DAO -> repository, plus a migration and DB version bump.

## Inputs to gather

1. **Entity name** in PascalCase (e.g. `Comic`, `Panel`, `Balloon`).
2. **Fields** with Kotlin types and which one is the primary key.
3. Whether it **relates** to an existing entity (foreign key / index),
   e.g. a `Panel` belongs to a `Comic`, a `Balloon` belongs to a `Panel`.

## Workflow

1. Add the entity in `data/<entity>/<Entity>Entity.kt`.
2. Add the DAO in `data/<entity>/<Entity>Dao.kt` (expose reads as `Flow`).
3. Register the entity + DAO in the `AppDatabase` class:
   - add the entity to the `@Database(entities = [...])` list,
   - add an abstract `fun <entity>Dao(): <Entity>Dao`,
   - **increment the `version`**.
4. Write a `Migration(<old>, <new>)` with the `CREATE TABLE` / `ALTER TABLE`
   SQL and register it where the database is built.
5. Add a repository **interface** in `data/<entity>/<Entity>Repository.kt` and a
   Room-backed implementation in `data/<entity>/Room<Entity>Repository.kt` that
   wraps the DAO and maps entities to domain models. ViewModels depend on the
   interface, not the DAO — this also lets tests supply a hand-written fake.
6. Bind the implementation to the interface with a `@Binds` Hilt module in `di/`.
7. Verify: `./gradlew assembleDebug` and add a Room in-memory DB test for the DAO.

## Rules

- Never change an existing entity's schema without adding a migration and
  bumping the DB `version`. Do **not** rely on `fallbackToDestructiveMigration`
  outside of local throwaway prototyping.
- DAO read queries return `Flow<...>`; writes are `suspend`.
- Keep Room annotations out of the `domain/` layer — map entities to domain
  models in the repository if the shapes differ.
- Name migrations `MIGRATION_<from>_<to>`.
- The repository is an **interface** with a Room-backed implementation, bound via
  `@Binds`. This keeps ViewModels testable with a plain fake (no mocking).

## Templates

### `<Entity>Entity.kt`

```kotlin
package com.ballooner.data.<entity>

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "<entity>")
data class <Entity>Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)
```

### `<Entity>Dao.kt`

```kotlin
package com.ballooner.data.<entity>

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface <Entity>Dao {
    @Query("SELECT * FROM <entity> ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<<Entity>Entity>>

    @Insert
    suspend fun insert(item: <Entity>Entity): Long
}
```

### Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `<entity>` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
```

### `<Entity>Repository.kt` (interface) + `Room<Entity>Repository.kt` (impl)

```kotlin
package com.ballooner.data.<entity>

import com.ballooner.domain.model.<Entity>
import kotlinx.coroutines.flow.Flow

interface <Entity>Repository {
    fun observe<Entity>s(): Flow<List<<Entity>>>
    suspend fun create<Entity>(name: String): Long
    suspend fun delete<Entity>(id: Long)
}
```

```kotlin
package com.ballooner.data.<entity>

import com.ballooner.domain.model.<Entity>
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class Room<Entity>Repository @Inject constructor(
    private val dao: <Entity>Dao,
) : <Entity>Repository {

    override fun observe<Entity>s(): Flow<List<<Entity>>> =
        dao.observeAll().map { entities -> entities.map(<Entity>Entity::toDomain) }

    override suspend fun create<Entity>(name: String): Long =
        dao.insert(<Entity>Entity(name = name.trim(), createdAt = System.currentTimeMillis()))

    override suspend fun delete<Entity>(id: Long) = dao.deleteById(id)
}

private fun <Entity>Entity.toDomain() = <Entity>(id = id, name = name, createdAt = createdAt)
```

### Hilt binding (`di/RepositoryModule.kt`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bind<Entity>Repository(impl: Room<Entity>Repository): <Entity>Repository
}
```
