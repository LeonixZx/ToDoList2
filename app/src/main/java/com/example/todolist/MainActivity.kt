package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val todoViewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppTheme {
                TodoApp(todoViewModel)
            }
        }
    }
}

@Composable
fun TodoAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF5B67CA),
            secondary = Color(0xFFFF6B6B),
            background = Color(0xFFF8F8F8),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1D1D1D),
            onSurface = Color(0xFF1D1D1D)
        ),
        content = content
    )
}

object AppColors {
    val LightPurple = Color(0xFFE4E7FF)
    val LightPink = Color(0xFFFFE4E4)
    val LightGreen = Color(0xFFE4FFF1)
    val LightYellow = Color(0xFFFFF8E1)
    val DarkText = Color(0xFF1D1D1D)
    val LightText = Color(0xFF7C7C7C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(todoViewModel: TodoViewModel) {
    var newTodoText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppColors.LightPurple,
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tasks", style = MaterialTheme.typography.titleMedium)
                        CategoryButtons(selectedCategory) { selectedCategory = it }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.LightPurple
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (newTodoText.isNotBlank()) {
                        todoViewModel.addTodo(newTodoText)
                        newTodoText = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(brush = gradient)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Greeting()
            TaskCount(todoViewModel.todos.size)
            Spacer(modifier = Modifier.height(8.dp))
            TodoList(
                todos = todoViewModel.getTodos(searchQuery, selectedCategory),
                onToggleTodo = { todoViewModel.toggleTodo(it) },
                onDeleteTodo = { todoViewModel.deleteTodo(it) }
            )
        }
    }
}

@Composable
fun CategoryButtons(selectedCategory: TaskCategory, onCategorySelected: (TaskCategory) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryButton(
            text = "To-Do",
            icon = Icons.Default.CheckCircleOutline,
            category = TaskCategory.TODO,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
        CategoryButton(
            text = "Done",
            icon = Icons.Default.CheckCircle,
            category = TaskCategory.DONE,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
        CategoryButton(
            text = "All",
            icon = Icons.Default.List,
            category = TaskCategory.ALL,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
    }
}

@Composable
fun CategoryButton(
    text: String,
    icon: ImageVector,
    category: TaskCategory,
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit
) {
    TextButton(
        onClick = { onCategorySelected(category) },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (category == selectedCategory)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = { Text("Search", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun Greeting() {
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Text(
        text = greeting,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun TaskCount(count: Int) {
    Text(
        text = "$count tasks this month",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun TodoList(todos: List<Todo>, onToggleTodo: (Int) -> Unit, onDeleteTodo: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle(todo.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.task,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(todo.createdAt)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            TaskStatusChip(completed = todo.isCompleted, onClick = { onToggle(todo.id) })
        }
    }
}

@Composable
fun TaskStatusChip(completed: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (completed) AppColors.LightGreen else AppColors.LightPink,
        contentColor = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    ) {
        Text(
            text = if (completed) "Done" else "Todo",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

enum class TaskCategory {
    TODO, DONE, ALL
}