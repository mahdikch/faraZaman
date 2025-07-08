package net.osmtracker.di

import android.content.Context
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.osmtracker.AppConstants
import net.osmtracker.data.api.FormDataApiService
import net.osmtracker.data.repository.FormDataRepository
import net.osmtracker.service.remote.AuthService
import net.osmtracker.service.remote.RoadService
import net.osmtracker.service.remote.ViolationApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient,@ApplicationContext context: Context): Retrofit {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val baseUrl = prefs.getString("BASE_URL", "https://app.tfs.co.ir/")!!
        return RetrofitProvider.getRetrofit(baseUrl, okHttpClient)

    }
    
    @Provides
    @Singleton
    fun provideFormDataApiService(retrofit: Retrofit): FormDataApiService {
        return retrofit.create(FormDataApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideFormDataRepository(
        apiService: FormDataApiService,
        @ApplicationContext context: Context,
        sharedPreferences: android.content.SharedPreferences
    ): FormDataRepository {
        return FormDataRepository(apiService, context, sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoadService(retrofit: Retrofit): RoadService {
        return retrofit.create(RoadService::class.java)
    }

    @Provides
    @Singleton
    fun provideViolationApiService(retrofit: Retrofit): ViolationApiService {
        return retrofit.create(ViolationApiService::class.java)
    }

    object RetrofitProvider {
        @Volatile
        private var retrofit: Retrofit? = null

        fun getRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
            if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!
        }

        fun reset() {
            retrofit = null
        }
    }
}