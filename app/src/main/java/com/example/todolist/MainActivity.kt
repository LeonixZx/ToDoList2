package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.ui.theme.MacOSTheme
import com.example.todolist.ui.theme.MacOSColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MacOSTheme {
                TodoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(todoViewModel: TodoViewModel = viewModel()) {
    var newTodoText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TodoInput(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                onAddTodo = {
                    if (newTodoText.isNotBlank()) {
                        todoViewModel.addTodo(newTodoText)
                        newTodoText = ""
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TodoList(
                todos = todoViewModel.todos,
                onToggleTodo = { todoViewModel.toggleTodo(it) },
                onDeleteTodo = { todoViewModel.deleteTodo(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoInput(value: String, onValueChange: (String) -> Unit, onAddTodo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Add a new task") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onAddTodo,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add todo")
        }
    }
}

@Composable
fun TodoList(
    todos: List<Todo>,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(todos) { todo ->
            TodoItem(todo, onToggleTodo, onDeleteTodo)
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggleTodo: (Int) -> Unit, onDeleteTodo: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (todo.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggleTodo(todo.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = todo.task,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onDeleteTodo(todo.id) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete todo",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}