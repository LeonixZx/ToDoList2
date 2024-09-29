package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Upload

class MainActivity : ComponentActivity() {
    private val todoViewModel: TodoViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { todoViewModel.exportTasks(contentResolver, it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { todoViewModel.importTasks(contentResolver, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppTheme {
                TodoApp(
                    todoViewModel,
                    onExport = { exportLauncher.launch("todos.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
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

enum class SortOrder {
    DATE_DESC, DATE_ASC, ALPHABET_ASC, ALPHABET_DESC
}

@Composable
fun rememberAuroraBackground(): Brush {
    val colors = listOf(
        Color(0xFFFF9AA2),
        Color(0xFFFFB7B2),
        Color(0xFFFFDAC1),
        Color(0xFFE2F0CB),
        Color(0xFFB5EAD7),
        Color(0xFFC7CEEA)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    return Brush.verticalGradient(
        colors = colors,
        startY = offset * 1000,
        endY = (offset + 1) * 1000
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(todoViewModel: TodoViewModel, onExport: () -> Unit, onImport: () -> Unit) {
    var newTodoText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortDialog by remember { mutableStateOf(false) }

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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Tasks", style = MaterialTheme.typography.titleMedium)
                        CategoryButtons(selectedCategory) { selectedCategory = it }
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Filled.Upload, contentDescription = "Export", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onImport) {
                        Icon(Icons.Filled.Download, contentDescription = "Import", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.LightPurple
                ),
                modifier = Modifier.height(48.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showSortDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                }
                AddTodoInput(
                    value = newTodoText,
                    onValueChange = { newTodoText = it },
                    onAddClick = {
                        if (newTodoText.isNotBlank()) {
                            todoViewModel.addTodo(newTodoText)
                            newTodoText = ""
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Greeting()
                    TaskCount(todoViewModel.todos.size)
                }
                Spacer(modifier = Modifier.height(4.dp))
                TodoList(
                    todos = todoViewModel.getTodos(searchQuery, selectedCategory).let { todos ->
                        when (sortOrder) {
                            SortOrder.DATE_DESC -> todos.sortedByDescending { it.createdAt }
                            SortOrder.DATE_ASC -> todos.sortedBy { it.createdAt }
                            SortOrder.ALPHABET_ASC -> todos.sortedBy { it.task }
                            SortOrder.ALPHABET_DESC -> todos.sortedByDescending { it.task }
                        }
                    },
                    onToggleTodo = { todoViewModel.toggleTodo(it) },
                    onDeleteTodo = { todoViewModel.deleteTodo(it) }
                )
            }
        }
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Tasks") },
            text = {
                Column {
                    TextButton(onClick = {
                        sortOrder = SortOrder.DATE_DESC
                        showSortDialog = false
                    }) {
                        Text("Date (Newest First)")
                    }
                    TextButton(onClick = {
                        sortOrder = SortOrder.DATE_ASC
                        showSortDialog = false
                    }) {
                        Text("Date (Oldest First)")
                    }
                    TextButton(onClick = {
                        sortOrder = SortOrder.ALPHABET_ASC
                        showSortDialog = false
                    }) {
                        Text("Alphabetically (A-Z)")
                    }
                    TextButton(onClick = {
                        sortOrder = SortOrder.ALPHABET_DESC
                        showSortDialog = false
                    }) {
                        Text("Alphabetically (Z-A)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddTodoInput(
    value: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    "Enter new task",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text(
                "Add",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
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
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(brush = gradient)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
        }
    }
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
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun TaskCount(count: Int) {
    Text(
        text = "$count tasks",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun TodoList(todos: List<Todo>, onToggleTodo: (Int) -> Unit, onDeleteTodo: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(todos) { todo ->
            TodoItem(todo, onToggleTodo, onDeleteTodo)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoItem(todo: Todo, onToggle: (Int) -> Unit, onDelete: (Int) -> Unit) {
    var showDeletePrompt by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize() // Add smooth animation for expansion
            .combinedClickable(
                onClick = { expanded = !expanded }, // Toggle expansion on click
                onLongClick = { showDeletePrompt = true },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (todo.isCompleted) Color.LightGray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top // Change to Top alignment
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.task,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (todo.isCompleted) Color.Gray else Color.Black,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (todo.isCompleted) 0.7f else 1f)
                )
                AnimatedVisibility(visible = expanded || todo.task.length <= 100) {
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(todo.createdAt)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = if (todo.isCompleted) Color.Gray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TaskStatusChip(completed = todo.isCompleted, onClick = { onToggle(todo.id) })
                if (todo.task.length > 100) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
        }
    }

    if (showDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(todo.id)
                    showDeletePrompt = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskStatusChip(completed: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (completed) AppColors.LightGreen.copy(alpha = 0.6f) else AppColors.LightPink,
        contentColor = if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .height(24.dp)
    ) {
        Text(
            text = if (completed) "Done" else "Todo",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

enum class TaskCategory {
    TODO, DONE, ALL
}



