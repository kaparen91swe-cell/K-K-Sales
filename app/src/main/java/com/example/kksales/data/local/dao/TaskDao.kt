package com.example.kksales.data.local.dao

import androidx.room.*
import com.example.kksales.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE assignedToUserId = :userId ORDER BY timestamp DESC")
    fun getTasksForUser(userId: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean, completedAt: Long?)
}
