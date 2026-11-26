package com.example.caloriescounter.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MealDatabaseTest {
    @Test
    fun migrations_coverEveryPublishedSchemaVersion() {
        val migrationVersions = MealDatabase.migrationsForTesting()
            .map { it.startVersion to it.endVersion }

        assertEquals(
            listOf(1 to 2, 2 to 3, 3 to 4, 4 to 5),
            migrationVersions
        )
    }
}
