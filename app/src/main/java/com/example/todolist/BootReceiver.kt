package com.example.todolist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val todoRepository = TodoRepository(context)
            val todos = todoRepository.loadTasks()

            todos.forEach { todo ->
                todo.reminder?.let { reminderDate ->
                    val currentTime = System.currentTimeMillis()
                    val delay = reminderDate.time - currentTime
                    if (delay > 0) {
                        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    "taskId" to todo.id,
                                    "taskTitle" to todo.task
                                )
                            )
                            .build()

                        WorkManager.getInstance(context).enqueue(workRequest)
                    }
                }
            }
        }
    }
}