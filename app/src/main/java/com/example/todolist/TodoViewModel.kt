package com.example.todolist

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

enum class SortOption {
    TIME, NAME
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val _todos = mutableStateListOf<Todo>()
    val todos: List<Todo> = _todos

    private val prefs = application.getSharedPreferences("TodoPrefs", Application.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        val todosJson = prefs.getString("todos", null)
        if (todosJson != null) {
            val type = object : TypeToken<List<Todo>>() {}.type
            val loadedTodos = gson.fromJson<List<Todo>>(todosJson, type)
            _todos.addAll(loadedTodos)
        }
    }

    private fun saveTodos() {
        val todosJson = gson.toJson(_todos)
        prefs.edit().putString("todos", todosJson).apply()
    }

    fun getTodos(query: String, sortOption: SortOption): List<Todo> {
        return _todos.filter { it.task.contains(query, ignoreCase = true) }
            .sortedWith(when (sortOption) {
                SortOption.TIME -> compareByDescending { it.createdAt }
                SortOption.NAME -> compareBy { it.task.lowercase(Locale.getDefault()) }
            })
    }

    fun addTodo(task: String) {
        _todos.add(Todo(id = (_todos.maxOfOrNull { it.id } ?: 0) + 1, task = task))
        saveTodos()
    }

    fun toggleTodo(id: Int) {
        val index = _todos.indexOfFirst { it.id == id }
        if (index != -1) {
            _todos[index] = _todos[index].copy(isCompleted = !_todos[index].isCompleted)
            saveTodos()
        }
    }

    fun deleteTodo(id: Int) {
        _todos.removeAll { it.id == id }
        saveTodos()
    }

    fun moveTodo(fromIndex: Int, toIndex: Int) {
        if (fromIndex in _todos.indices && toIndex in _todos.indices && fromIndex != toIndex) {
            val todo = _todos.removeAt(fromIndex)
            _todos.add(toIndex, todo)
            saveTodos()
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
