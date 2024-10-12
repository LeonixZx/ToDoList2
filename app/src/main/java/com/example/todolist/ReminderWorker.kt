package com.example.todolist

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.util.Log


class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val taskId = inputData.getInt("taskId", 0)
        val taskTitle = inputData.getString("taskTitle") ?: "Task Reminder"

        Log.d("ReminderWorker", "doWork called for task: $taskTitle")

        showNotification(taskId, taskTitle)

        return Result.success()
    }

    private fun showNotification(taskId: Int, taskTitle: String) {
        Log.d("ReminderWorker", "Showing notification for task: $taskTitle")
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Todo Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure you have this icon in your drawable resources
            .setContentTitle("Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification)
    }

    companion object {
        const val CHANNEL_ID = "TODO_REMINDERS"
    }
}