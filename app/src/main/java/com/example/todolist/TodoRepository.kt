package com.example.todolist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class TodoRepository(private val context: Context) {
    private val gson = Gson()

    fun saveTasks(tasks: List<Todo>) {
        val json = gson.toJson(tasks)
        context.openFileOutput("tasks.json", Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun loadTasks(): List<Todo> {
        val file = File(context.filesDir, "tasks.json")
        return if (file.exists()) {
            val json = file.readText()
            gson.fromJson(json, object : TypeToken<List<Todo>>() {}.type)
        } else {
            emptyList()
        }
    }

    fun exportTasks(file: File) {
        val tasks = loadTasks()
        val json = gson.toJson(tasks)
        file.writeText(json)
    }

    fun importTasks(file: File) {
        val json = file.readText()
        val tasks: List<Todo> = gson.fromJson(json, object : TypeToken<List<Todo>>() {}.type)
        saveTasks(tasks)
    }
}