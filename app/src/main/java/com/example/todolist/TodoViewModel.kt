package com.example.todolist

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Base64
import java.io.FileInputStream
import java.io.FileOutputStream

import java.util.Date

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
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

    fun setReminder(id: Int, taskTitle: String, reminderDate: Date) {
        viewModelScope.launch {
            val index = _todos.indexOfFirst { it.id == id }
            if (index != -1) {
                _todos[index] = _todos[index].copy(reminder = reminderDate)
                saveTasks()
                scheduleReminder(id, taskTitle, reminderDate.time - System.currentTimeMillis())
            } else {
                Log.e("TodoViewModel", "Attempted to set reminder for non-existent todo with ID: $id")
                // Optionally, you could throw an exception here if you want to enforce stricter error handling
                // throw IllegalArgumentException("No todo item found with ID: $id")
            }
        }
    }


    private fun scheduleReminder(id: Int, taskTitle: String, delay: Long) {
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "taskId" to id,
                    "taskTitle" to taskTitle
                )
            )
            .build()

        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    fun removeReminder(id: Int) {
        viewModelScope.launch {
            val index = _todos.indexOfFirst { it.id == id }
            if (index != -1) {
                _todos[index] = _todos[index].copy(reminder = null)
                saveTasks()
            }
        }
    }

    fun addTodo(task: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            val newImageUri = imageUri?.let { saveImageToInternalStorage(it) }
            val newTodo = Todo(
                id = (_todos.maxOfOrNull { it.id } ?: 0) + 1,
                task = task,
                imageUri = newImageUri?.toString()
            )
            _todos.add(newTodo)
            saveTasks()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "images/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            file.parentFile?.mkdirs()
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("TodoViewModel", "Error saving image: ${e.message}")
            null
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
            val todoToDelete = _todos.find { it.id == id }
            todoToDelete?.let {
                it.imageUri?.let { uri ->
                    if (_todos.count { it.imageUri == uri } == 1) {
                        try {
                            val file = File(Uri.parse(uri).path!!)
                            if (file.exists()) {
                                file.delete()
                            } else {
                                Log.w("TodoViewModel", "Image file not found: $uri")
                            }
                        } catch (e: Exception) {
                            Log.e("TodoViewModel", "Error deleting image file: ${e.message}")
                        }
                    }
                }
            }
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
            val tasksWithEncodedImages = _todos.map { todo ->
                todo.copy(
                    imageUri = todo.imageUri?.let { encodeImageToBase64(it) }
                )
            }
            contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(gson.toJson(tasksWithEncodedImages).toByteArray())
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
                    _todos.addAll(importedTodos.map { todo ->
                        todo.copy(
                            imageUri = todo.imageUri?.let { decodeBase64ToImage(it) }
                        )
                    })
                }
                saveTasks()
            }
        }
    }

    fun editTodo(id: Int, newText: String, newImageUri: Uri?) {
        val index = _todos.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldImageUri = _todos[index].imageUri
            val updatedImageUri = newImageUri?.let { saveImageToInternalStorage(it) }?.toString()
            _todos[index] = _todos[index].copy(
                task = newText,
                imageUri = updatedImageUri
            )
            if (oldImageUri != updatedImageUri && oldImageUri != null) {
                if (_todos.count { it.imageUri == oldImageUri } == 0) {
                    try {
                        val file = File(Uri.parse(oldImageUri).path!!)
                        if (file.exists()) {
                            file.delete()
                        } else {
                            Log.w("TodoViewModel", "Old image file not found: $oldImageUri")
                        }
                    } catch (e: Exception) {
                        Log.e("TodoViewModel", "Error deleting old image file: ${e.message}")
                    }
                }
            }
            saveTasks()
        }
    }

    fun getTodos(query: String, category: TaskCategory): List<Todo> {
        return todos.filter { todo ->
            (todo.task.contains(query, ignoreCase = true)) &&
                    when (category) {
                        TaskCategory.TODO -> !todo.isCompleted
                        TaskCategory.DONE -> todo.isCompleted
                        TaskCategory.ALL -> true
                    }
        }
    }


    fun downloadImage(imageUri: String, contentResolver: ContentResolver, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(Uri.parse(imageUri).path!!)
                if (sourceFile.exists()) {
                    contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d("TodoViewModel", "Image downloaded successfully")
                } else {
                    Log.e("TodoViewModel", "Source image file not found")
                }
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error downloading image: ${e.message}")
            }
        }
    }


    private fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val imageFile = File(Uri.parse(imagePath).path!!)
            if (!imageFile.exists()) {
                Log.e("TodoViewModel", "Image file not found: $imagePath")
                return null
            }
            FileInputStream(imageFile).use { inputStream ->
                val bytes = ByteArray(imageFile.length().toInt())
                inputStream.read(bytes)
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            Log.e("TodoViewModel", "Error encoding image to Base64: ${e.message}")
            null
        }
    }

    private fun decodeBase64ToImage(base64String: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val file = File(context.filesDir, "images/${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(decodedBytes) }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e("TodoViewModel", "Error decoding Base64 to image: ${e.message}")
            null
        }
    }
}