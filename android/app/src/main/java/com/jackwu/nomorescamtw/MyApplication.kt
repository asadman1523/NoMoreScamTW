package com.jackwu.nomorescamtw

import android.app.Application
import androidx.room.Room
import com.jackwu.nomorescamtw.data.AppDatabase
import com.jackwu.nomorescamtw.network.FirebaseApiService
import com.jackwu.nomorescamtw.network.FraudApiService
import com.jackwu.nomorescamtw.repository.FraudRepository
import retrofit2.Retrofit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MyApplication : Application() {

    lateinit var repository: FraudRepository

    override fun onCreate() {
        super.onCreate()
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "fraud-database"
        ).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://opdadm.moi.gov.tw/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val apiService = retrofit.create(FraudApiService::class.java)

        val apiService = retrofit.create(FraudApiService::class.java)

        // Firebase Retrofit
        val firebaseRetrofit = Retrofit.Builder()
            .baseUrl(Config.FIREBASE_DB_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val firebaseService = firebaseRetrofit.create(FirebaseApiService::class.java)

        repository = FraudRepository(database.fraudDao(), apiService, firebaseService)
    }
}
