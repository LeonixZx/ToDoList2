package com.example.todolist


data class Todo(
    val id: Int,
    val task: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList()
)