package com.example.todolist

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TodoRepository(application)
    private val _todos = mutableStateListOf<Todo>()
    val todos: List<Todo> = _todos
    private val gson = Gson()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedTodos = repository.loadTasks()
            withContext(Dispatchers.Main) {
                _todos.clear()
                _todos.addAll(loadedTodos)
            }
        }
    }

    fun addTodo(task: String) {
        viewModelScope.launch {
            val newTodo = Todo(id = (_todos.maxOfOrNull { it.id } ?: 0) + 1, task = task)
            _todos.add(newTodo)
            saveTasks()
        }
    }

    fun toggleTodo(id: Int) {
        viewModelScope.launch {
            val index = _todos.indexOfFirst { it.id == id }
            if (index != -1) {
                _todos[index] = _todos[index].copy(isCompleted = !_todos[index].isCompleted)
                saveTasks()
            }
        }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch {
            _todos.removeAll { it.id == id }
            saveTasks()
        }
    }

    private fun saveTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTasks(_todos)
        }
    }

    fun exportTasks(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(gson.toJson(_todos).toByteArray())
            }
        }
    }

    fun importTasks(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val importedTodos = gson.fromJson<List<Todo>>(jsonString, object : TypeToken<List<Todo>>() {}.type)
                withContext(Dispatchers.Main) {
                    _todos.clear()
                    _todos.addAll(importedTodos)
                }
                saveTasks()
            }
        }
    }

    fun getTodos(query: String, category: TaskCategory): List<Todo> {
        return _todos.filter { todo ->
            (todo.task.contains(query, ignoreCase = true)) &&
                    when (category) {
                        TaskCategory.TODO -> !todo.isCompleted
                        TaskCategory.DONE -> todo.isCompleted
                        TaskCategory.ALL -> true
                    }
        }
    }
}