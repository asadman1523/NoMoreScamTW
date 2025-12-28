package com.jackwu.nomorescamtw.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FraudDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sites: List<FraudSite>)

    @Query("SELECT * FROM fraud_sites WHERE url = :url LIMIT 1")
    suspend fun getSite(url: String): FraudSite?

    @Query("DELETE FROM fraud_sites")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM fraud_sites")
    suspend fun getCount(): Int
}
