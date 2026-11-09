package com.bekacodes.aegispay.di

import android.content.Context
import com.bekacodes.aegispay.data.AegisPayDao
import com.bekacodes.aegispay.data.AegisPayDatabase
import com.bekacodes.aegispay.domain.AiSecurityAgent
import com.bekacodes.aegispay.security.KeystoreVaultCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityVaultModule {

    @Provides
    @Singleton
    fun provideAegisPayDatabase(
        @ApplicationContext context: Context,
    ): AegisPayDatabase = AegisPayDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideAegisPayDao(
        database: AegisPayDatabase,
    ): AegisPayDao = database.dao()

    @Provides
    @Singleton
    fun provideKeystoreVaultCipher(): KeystoreVaultCipher = KeystoreVaultCipher()

    @Provides
    @Singleton
    fun provideAiSecurityAgent(): AiSecurityAgent = AiSecurityAgent()
}
