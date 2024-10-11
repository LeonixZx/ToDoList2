package com.example.todolist

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        val taskTitle = inputData.getString("taskTitle") ?: "Task Reminder"

        if (taskId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, "TODO_REMINDERS")
                .setContentTitle("Task Reminder")
                .setContentText(taskTitle)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(taskId, notification)
        }

        return Result.success()
    }
}