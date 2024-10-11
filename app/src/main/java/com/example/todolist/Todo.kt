package com.example.todolist

import java.util.Date

data class Todo(
    val id: Int,
    val task: String,
    var isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var imageUri: String? = null,
    var reminder: Date? = null
)