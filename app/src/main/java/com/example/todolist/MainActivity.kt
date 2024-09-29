package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check

class MainActivity : ComponentActivity() {
    private lateinit var adMobManager: AdMobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adMobManager = (application as TodoApplication).adMobManager
        setContent {
            AppTheme {
                TodoApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adMobManager.showAdIfAvailable(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(todoViewModel: TodoViewModel = viewModel()) {
    var newTodoText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Greeting("User")
            Spacer(modifier = Modifier.height(8.dp))
            TaskCount(todoViewModel.todos.size)
            Spacer(modifier = Modifier.height(16.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            CategoryButtons(selectedCategory) { selectedCategory = it }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Today's Tasks",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                todos = todoViewModel.getTodos(searchQuery, selectedCategory),
                onToggleTodo = { todoViewModel.toggleTodo(it) },
                onDeleteTodo = { todoViewModel.deleteTodo(it) }
            )
        }
    }
}

@Composable
fun Greeting(name: String) {
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    Text(
        text = "$greeting, $name!",
        style = MaterialTheme.typography.headlineMedium,
        color = AppColors.DarkText
    )
}

@Composable
fun TaskCount(count: Int) {
    Text(
        text = buildAnnotatedString {
            append("You have ")
            withStyle(style = SpanStyle(
                color = AppColors.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )) {
                append("$count tasks")
            }
            append(" this month 👍")
        },
        style = MaterialTheme.typography.bodyLarge,
        color = AppColors.LightText
    )
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        placeholder = { Text("Search a task...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.LightText.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun CategoryButtons(selectedCategory: TaskCategory, onCategorySelected: (TaskCategory) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CategoryButton(
            text = "To-Do",
            icon = Icons.Default.Add,
            color = AppColors.LightPink,
            selected = selectedCategory == TaskCategory.TODO,
            onClick = { onCategorySelected(TaskCategory.TODO) }
        )
        CategoryButton(
            text = "Done",
            icon = Icons.Default.Check,
            color = AppColors.LightGreen,
            selected = selectedCategory == TaskCategory.DONE,
            onClick = { onCategorySelected(TaskCategory.DONE) }
        )
        CategoryButton(
            text = "All",
            icon = Icons.Default.List,
            color = AppColors.LightPurple,
            selected = selectedCategory == TaskCategory.ALL,
            onClick = { onCategorySelected(TaskCategory.ALL) }
        )
    }
}

@Composable
fun CategoryButton(text: String, icon: ImageVector, color: Color, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) color else color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.DarkText)
            Text(text, color = AppColors.DarkText)
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
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.LightText.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onAddTodo,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add todo")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoList(
    todos: List<Todo>,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit
) {
    LazyColumn {
        items(todos, key = { it.id }) { todo ->
            TodoItem(
                todo = todo,
                onToggle = { onToggleTodo(todo.id) },
                onDelete = { onDeleteTodo(todo.id) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.task,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(todo.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.LightText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(
                onClick = { onToggle() },
                label = { Text(if (todo.isCompleted) "Completed" else "Pending") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (todo.isCompleted) AppColors.LightGreen else AppColors.LightPink
                )
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Secondary)
            }
        }
    }
}

enum class TaskCategory { TODO, DONE, ALL }