package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.TaskDao
import com.example.kksales.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasksForUser(userId: Int): Flow<List<Task>> = taskDao.getTasksForUser(userId)

    suspend fun addTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun completeTask(taskId: Int) {
        taskDao.updateTaskStatus(taskId, true, System.currentTimeMillis())
    }

    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()
}
