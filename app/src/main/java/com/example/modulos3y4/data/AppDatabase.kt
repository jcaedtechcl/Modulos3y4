package com.example.modulos3y4.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, IssPosition::class, IssPositionRecord::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun issPositionDao(): IssPositionDao
    abstract fun issHistoryDao(): IssHistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * La version 1 solo tenia la tabla `users`. Se migra agregando `iss_position`
         * en vez de recurrir a una migracion destructiva, para no borrar los usuarios
         * ya registrados en las instalaciones existentes.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `iss_position` (
                        `id` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * La version 3 agrega el historial que el usuario arma a mano. El DDL
         * debe coincidir con el que genera Room para IssPositionRecord, porque
         * Room valida el esquema al abrir la base despues de migrar.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `iss_position_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * La version 4 agrega la marca de sincronizacion. Los registros que ya
         * existen quedan en 0 (pendientes), que es lo correcto: se guardaron
         * antes de que hubiera Firestore, asi que nunca se subieron.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `iss_position_history` " +
                        "ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "modulos3y4.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
