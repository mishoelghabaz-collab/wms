package com.example.di

import android.content.Context
import com.example.data.inventory.InventoryItemDao
import com.example.data.inventory.WarehouseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module providing local persistence and Room database instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWarehouseDatabase(
        @ApplicationContext context: Context
    ): WarehouseDatabase {
        return WarehouseDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideInventoryItemDao(
        database: WarehouseDatabase
    ): InventoryItemDao {
        return database.inventoryItemDao()
    }
}
