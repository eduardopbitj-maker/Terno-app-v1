package com.roupas.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PecaEntity::class,
        AssociacaoTernoGravataEntity::class,
        CombinacaoEntity::class,
        RegistroUsoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pecaDao(): PecaDao
    abstract fun associacaoDao(): AssociacaoDao
    abstract fun combinacaoDao(): CombinacaoDao
    abstract fun registroUsoDao(): RegistroUsoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roupas_app.db"
                ).build().also { instancia = it }
            }
    }
}
