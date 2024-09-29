package com.example.todolist


data class Todo(
    val id: Int,
    val task: String,
    var isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)