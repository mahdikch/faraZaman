package net.osmtracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.osmtracker.data.db.dao.UserDao
import net.osmtracker.data.db.room.AppDatabase
import net.osmtracker.data.repository.AuthRepositoryImpl
import net.osmtracker.domain.repository.AuthRepository
import net.osmtracker.domain.usecase.LoginUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
//    @Provides
//    @Singleton
//    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
//        return Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
//            .build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideUserDao(database: AppDatabase): UserDao {
//        return database.userDao()
//    }

    @Provides
    @Singleton
    fun provideAuthRepository(authService: net.osmtracker.service.remote.AuthService): AuthRepository {
        return AuthRepositoryImpl(authService)
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(repository: AuthRepository): LoginUseCase {
        return LoginUseCase(repository)
    }
}