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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex

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


class CustomTopShape(private val cornerRadius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(
            path = Path().apply {
                reset()
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - cornerRadius)
                quadraticBezierTo(size.width, size.height, size.width - cornerRadius, size.height)
                lineTo(cornerRadius, size.height)
                quadraticBezierTo(0f, size.height, 0f, size.height - cornerRadius)
                close()
            }
        )
    }
}

@Composable
fun TopSection(
    todoViewModel: TodoViewModel,
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val today = remember { SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()) }
    val itemCount = todoViewModel.todos.size

    val colors = listOf(
        Color(0xFF9C27B0),
        Color(0xFF3F51B5),
        Color(0xFF2196F3),
        Color(0xFF00BCD4)
    )

    val brush = Brush.linearGradient(colors = colors)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(brush = brush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tasks", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Row {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Filled.Upload, contentDescription = "Export", tint = Color.White)
                    }
                    IconButton(onClick = onImport) {
                        Icon(Icons.Filled.Download, contentDescription = "Import", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Today: $today",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CategoryButtons(selectedCategory, onCategorySelected)
        }
    }
}



@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .then(Modifier.height(48.dp)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(text)
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopSection(
                todoViewModel = todoViewModel,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onExport = onExport,
                onImport = onImport
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                onDeleteTodo = { todoViewModel.deleteTodo(it) },
                onEditTodo = { id, newText -> todoViewModel.editTodo(id, newText) }
            )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CategoryButton(
            text = "To-Do",
            icon = Icons.Default.CheckCircleOutline,
            selected = selectedCategory == TaskCategory.TODO,
            onClick = { onCategorySelected(TaskCategory.TODO) },
            modifier = Modifier.weight(1f)
        )
        CategoryButton(
            text = "Done",
            icon = Icons.Default.CheckCircle,
            selected = selectedCategory == TaskCategory.DONE,
            onClick = { onCategorySelected(TaskCategory.DONE) },
            modifier = Modifier.weight(1f)
        )
        CategoryButton(
            text = "All",
            icon = Icons.Default.List,
            selected = selectedCategory == TaskCategory.ALL,
            onClick = { onCategorySelected(TaskCategory.ALL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CategoryButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
            contentColor = if (selected) Color(0xFF3F51B5) else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}






@Composable
fun TaskStatusToggle(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val switchWidth = 52.dp
    val switchHeight = 26.dp
    val toggleSize = 22.dp

    val interactionSource = remember { MutableInteractionSource() }

    val animatedOffset by animateDpAsState(
        targetValue = if (isCompleted) (switchWidth - toggleSize - 2.dp) else 2.dp,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .width(switchWidth)
            .height(switchHeight)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(switchHeight / 2))
            .clip(RoundedCornerShape(switchHeight / 2))
            .background(if (isCompleted) Color(0xFF4CAF50) else Color(0xFFFF5252))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onToggle()
            }
    ) {
        // Text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = if (isCompleted) "Done" else "Todo",
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(if (isCompleted) Alignment.CenterStart else Alignment.CenterEnd)
                    .zIndex(2f)
            )
        }

        // Toggle
        Box(
            modifier = Modifier
                .size(toggleSize)
                .offset(x = animatedOffset)
                .align(Alignment.CenterStart)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .zIndex(3f)
        )
    }
}




@Composable
fun AnimatedGradientBackground(content: @Composable () -> Unit) {
    val colors = listOf(
        Color(0xFF9C27B0),
        Color(0xFF3F51B5),
        Color(0xFF2196F3),
        Color(0xFF00BCD4),
        Color(0xFF4CAF50)
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

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(offset * 1000f, 0f),
        end = Offset((offset + 1) * 1000f, 1000f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = brush)
    ) {
        content()
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
fun TodoList(todos: List<Todo>, onToggleTodo: (Int) -> Unit, onDeleteTodo: (Int) -> Unit, onEditTodo: (Int, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(todos) { todo ->
            TodoItem(todo, onToggleTodo, onDeleteTodo, onEditTodo)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoItem(
    todo: Todo,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (Int, String) -> Unit
) {
    var showDeletePrompt by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(todo.task) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = { expanded = !expanded },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isEditing = !isEditing },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit task",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    BasicTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (todo.isCompleted) Color.Gray else Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = todo.task,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (todo.isCompleted) Color.Gray else Color.Black,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (todo.isCompleted) 0.7f else 1f)
                    )
                }
                if (expanded || todo.task.length <= 100) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(todo.createdAt)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = if (todo.isCompleted) Color.Gray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TaskStatusToggle(
                isCompleted = todo.isCompleted,
                onToggle = { onToggle(todo.id) }
            )
        }

        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    isEditing = false
                    editedText = todo.task  // Reset to original text
                }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onEdit(todo.id, editedText)
                        isEditing = false
                    },
                    enabled = editedText.isNotBlank() && editedText != todo.task
                ) {
                    Text("Save")
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



