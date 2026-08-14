package com.ankitsaini.securevault.data.dao

import androidx.room.*
import com.ankitsaini.securevault.data.model.AppPolicy
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPolicyDao {
    
    @Query("SELECT * FROM app_policies ORDER BY created_at DESC")
    fun getAllPolicies(): Flow<List<AppPolicy>>
    
    @Query("SELECT * FROM app_policies WHERE policy_id = :policyId LIMIT 1")
    suspend fun getPolicyById(policyId: String): AppPolicy?
    
    @Query("SELECT * FROM app_policies WHERE policy_id = :policyId LIMIT 1")
    fun observePolicyById(policyId: String): Flow<AppPolicy?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: AppPolicy)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPolicies(policies: List<AppPolicy>)
    
    @Update
    suspend fun updatePolicy(policy: AppPolicy)
    
    @Delete
    suspend fun deletePolicy(policy: AppPolicy)
    
    @Query("DELETE FROM app_policies WHERE policy_id = :policyId")
    suspend fun deletePolicyById(policyId: String)
    
    @Query("DELETE FROM app_policies")
    suspend fun deleteAllPolicies()
}
