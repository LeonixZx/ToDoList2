package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.ui.theme.AppTheme
import com.example.todolist.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.ui.tooling.preview.Preview
import android.app.Application
import androidx.compose.foundation.BorderStroke

import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    private lateinit var adMobManager: AdMobManager
    private val todoViewModel: TodoViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { todoViewModel.exportTasks(contentResolver, it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { todoViewModel.importTasks(contentResolver, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adMobManager = (application as TodoApplication).adMobManager
        setContent {
            AppTheme {
                TodoApp(todoViewModel, ::exportTasks, ::importTasks)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adMobManager.showAdIfAvailable(this)
    }

    private fun exportTasks() {
        exportLauncher.launch("tasks.json")
    }

    private fun importTasks() {
        importLauncher.launch(arrayOf("application/json"))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(
    todoViewModel: TodoViewModel = viewModel(),
    onExportTasks: () -> Unit,
    onImportTasks: () -> Unit
) {
    var newTodoText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List", style = MaterialTheme.typography.titleSmall) },
                actions = {
                    IconButton(onClick = onExportTasks) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Export Tasks", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onImportTasks) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Import Tasks", modifier = Modifier.size(20.dp))
                    }
                },
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Greeting()
                TaskCount(todoViewModel.todos.size)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
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
            }
            Spacer(modifier = Modifier.height(4.dp))
            CategoryButtons(selectedCategory) { selectedCategory = it }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Today's Tasks",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            TodoList(
                todos = todoViewModel.getTodos(searchQuery, selectedCategory),
                onToggleTodo = { todoViewModel.toggleTodo(it) },
                onDeleteTodo = { todoViewModel.deleteTodo(it) }
            )
        }
    }
}

@Composable
fun Greeting() {
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    Text(
        text = "$greeting!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TaskCount(count: Int) {
    Text(
        text = "$count tasks this month",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(40.dp),
        placeholder = { Text("Search", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun CategoryButtons(selectedCategory: TaskCategory, onCategorySelected: (TaskCategory) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CategoryButton("To-Do", TaskCategory.TODO, selectedCategory, onCategorySelected)
        CategoryButton("Done", TaskCategory.DONE, selectedCategory, onCategorySelected)
        CategoryButton("All", TaskCategory.ALL, selectedCategory, onCategorySelected)
    }
}

@Composable
fun CategoryButton(
    text: String,
    category: TaskCategory,
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit
) {
    val isSelected = category == selectedCategory
    OutlinedButton(
        onClick = { onCategorySelected(category) },
        modifier = Modifier.height(32.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoInput(value: String, onValueChange: (String) -> Unit, onAddTodo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.height(45.dp).weight(1f),
            placeholder = { Text("Add task", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
        IconButton(onClick = onAddTodo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add todo", modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoList(todos: List<Todo>, onToggleTodo: (Int) -> Unit, onDeleteTodo: (Int) -> Unit) {
    LazyColumn {
        items(todos) { todo ->
            TodoItem(todo, onToggleTodo, onDeleteTodo)
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggle: (Int) -> Unit, onDelete: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle(todo.id) },
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = todo.task,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            Text(
                text = if (todo.isCompleted) "Done" else "Todo",
                style = MaterialTheme.typography.labelSmall,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            IconButton(onClick = { onDelete(todo.id) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        TodoApp(
            todoViewModel = TodoViewModel(Application()),
            onExportTasks = {},
            onImportTasks = {}
        )
    }
}

enum class TaskCategory { TODO, DONE, ALL }