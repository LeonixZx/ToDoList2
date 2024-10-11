package com.example.todolist

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.animateContentSize

class MainActivity : ComponentActivity() {
    private companion object {
        private const val AD_LOAD_COOLDOWN = 60000L // 1 minute cooldown
    }
    private val todoViewModel: TodoViewModel by viewModels()
    private var interstitialAd: InterstitialAd? = null
    private lateinit var appOpenAdManager: AppOpenAdManager

    private fun getAdMobDeviceId() {
        val adRequest = AdRequest.Builder().build()
        val adId = adRequest.hashCode().toString() // Use hashCode as a fallback
        Log.d("DeviceID", "AdMob Device ID: $adId")
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            // Delete the existing file if it exists
            contentResolver.openOutputStream(it, "wt")?.close()
            todoViewModel.exportTasks(contentResolver, it)
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { todoViewModel.importTasks(contentResolver, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        // Initialize AppOpenAdManager
        appOpenAdManager = (application as TodoApplication).appOpenAdManager

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("TestDeviceID", "Use this ID for testing: $deviceId")

        // Initialize AdMob
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                Log.d("AdMob", "Adapter name: $adapterClass, Description: ${status.description}, Latency: ${status.latency}")
            }
        }

        // Initialize AdManager
        AdManager.initialize(this)

        // Call getAdMobDeviceId() here
        getAdMobDeviceId()

        // Load the interstitial ad
        loadInterstitialAd()

        setContent {
            TodoAppTheme {
                var showAd by remember { mutableStateOf(true) }

                if (showAd) {
                    ShowInterstitialAd(
                        onAdClosed = {
                            showAd = false
                        }
                    )
                }

                TodoApp(
                    todoViewModel = todoViewModel,
                    onExport = { exportLauncher.launch("todos.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
        observeReminders()
    }

    private var lastAdLoadAttempt = 0L

    private fun loadInterstitialAd() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdLoadAttempt < AD_LOAD_COOLDOWN) {
            Log.d("AdMob", "Skipping ad load due to cooldown")
            return
        }
        lastAdLoadAttempt = currentTime

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-2107817689571311/7365052025", // Test ID (ca-app-pub-3940256099942544/1033173712)
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdMob", "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("AdMob", "Interstitial ad was dismissed")
                            interstitialAd = null
                            loadInterstitialAd() // Load the next ad
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdMob", "Interstitial ad failed to show. Error: ${adError.message}")
                            interstitialAd = null
                        }
                        override fun onAdShowedFullScreenContent() {
                            Log.d("AdMob", "Interstitial ad showed fullscreen content")
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Interstitial ad failed to load. Error: ${error.message}")
                    Log.e("AdMob", "Error code: ${error.code}")
                    Log.e("AdMob", "Error domain: ${error.domain}")
                    when (error.code) {
                        AdRequest.ERROR_CODE_INTERNAL_ERROR -> Log.e("AdMob", "Internal error occurred")
                        AdRequest.ERROR_CODE_INVALID_REQUEST -> Log.e("AdMob", "Invalid ad request")
                        AdRequest.ERROR_CODE_NETWORK_ERROR -> Log.e("AdMob", "Network error occurred")
                        AdRequest.ERROR_CODE_NO_FILL -> Log.e("AdMob", "No ad fill")
                        AdRequest.ERROR_CODE_APP_ID_MISSING -> Log.e("AdMob", "App ID is missing")
                        else -> Log.e("AdMob", "Unknown error occurred")
                    }
                    interstitialAd = null
                    retryWithExponentialBackoff { loadInterstitialAd() }
                }
            }
        )
    }

    private fun retryWithExponentialBackoff(
        attemptNumber: Int = 0,
        maxAttempts: Int = 3,
        initialDelay: Long = 5000, // Increased from 1000 to 5000 milliseconds
        maxDelay: Long = 300000,   // Increased from 60000 to 300000 milliseconds (5 minutes)
        factor: Double = 2.0,
        action: () -> Unit
    ) {
        if (attemptNumber >= maxAttempts) {
            Log.e("AdMob", "Max retry attempts reached")
            return
        }

        val delay = (initialDelay * factor.pow(attemptNumber.toDouble())).toLong().coerceAtMost(maxDelay)
        val jitter = Random.nextLong(delay / 2)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isNetworkAvailable()) {
                action()
            } else {
                Log.e("AdMob", "No network connection, retrying later")
                retryWithExponentialBackoff(attemptNumber + 1, maxAttempts, initialDelay, maxDelay, factor, action)
            }
        }, delay + jitter)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("TODO_REMINDERS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeReminders() {
        todoViewModel.todos.forEach { todo ->
            todo.reminder?.let { reminderDate ->
                val currentTime = System.currentTimeMillis()
                val delay = reminderDate.time - currentTime
                if (delay > 0) {
                    val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            "taskId" to todo.id,
                            "taskTitle" to todo.task
                        ))
                        .build()
                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                }
            }
        }
    }

    @Composable
    private fun ShowInterstitialAd(onAdClosed: () -> Unit) {
        val canShowAd = remember { mutableStateOf(true) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            if (canShowAd.value && interstitialAd != null) {
                interstitialAd?.show(this@MainActivity)
                canShowAd.value = false
                coroutineScope.launch {
                    delay(AD_LOAD_COOLDOWN)
                    canShowAd.value = true
                }
            } else {
                Log.w("AdMob", "Interstitial ad not loaded or cooldown in effect")
                onAdClosed()
            }
        }
    }
}

@Composable
fun TodoApp(todoViewModel: TodoViewModel, onExport: () -> Unit, onImport: () -> Unit) {
    var newTodoText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    var imageUriToDownload by remember { mutableStateOf<String?>(null) }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val totalTasks = todoViewModel.todos.size
    val completedTasks = todoViewModel.todos.count { it.isCompleted }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedImageUri = uri
        }
    )

    val context = LocalContext.current
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let { destinationUri ->
            imageUriToDownload?.let { imageUri ->
                todoViewModel.downloadImage(imageUri, context.contentResolver, destinationUri)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            todoViewModel.addTodo(
                                task = newTodoText,
                                imageUri = selectedImageUri
                            )
                            newTodoText = ""
                            selectedImageUri = null
                        }
                    },
                    onImageClick = { imagePicker.launch("image/*") },
                    selectedImageUri = selectedImageUri,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.weight(1f)) {
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
                        onEditTodo = { id, newText, newImageUri ->
                            todoViewModel.editTodo(id, newText, newImageUri)
                        },
                        onShowFullScreenImage = { imageUri -> showFullScreenImage = imageUri },
                        onDownloadImage = { imageUri ->
                            imageUriToDownload = imageUri
                            downloadLauncher.launch("todo_image_${System.currentTimeMillis()}.jpg")
                        },
                        onSetReminder = { id, date -> todoViewModel.setReminder(id, date) },
                        onRemoveReminder = { id -> todoViewModel.removeReminder(id) }
                    )
                }
            }
        }

        ProgressGauge(
            progress = progress,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(16.dp)
                .align(Alignment.BottomStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        )

        showFullScreenImage?.let { imageUri ->
            FullScreenImageViewer(
                imageUri = imageUri,
                onDismiss = { showFullScreenImage = null }
            )
        }
    }

    if (showSortDialog) {
        SortDialog(
            currentSortOrder = sortOrder,
            onSortOrderSelected = {
                sortOrder = it
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
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

    // Set up GIF loader
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(brush = brush)
    ) {
        // Add GIF on the right side
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(data = R.drawable.todo_animation)
                    .apply(block = {
                        size(coil.size.Size.ORIGINAL)
                    }).build(),
                imageLoader = imageLoader
            ),
            contentDescription = "Animated GIF",
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 1.dp)
                .alpha(0.3f),
            contentScale = ContentScale.Crop
        )

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
                Text(
                    text = "Simple ToDoList",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    color = Color.White
                )
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
fun AddTodoInput(
    value: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onImageClick: () -> Unit,
    selectedImageUri: Uri?,
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
                IconButton(
                    onClick = onImageClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (selectedImageUri != null) Icons.Filled.Image else Icons.Outlined.Image,
                        contentDescription = "Add Image",
                        tint = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
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
fun TodoList(
    todos: List<Todo>,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit,
    onEditTodo: (Int, String, Uri?) -> Unit,
    onShowFullScreenImage: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onSetReminder: (Int, Date) -> Unit,
    onRemoveReminder: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(todos) { todo ->
            TodoItem(
                todo = todo,
                onToggle = onToggleTodo,
                onDelete = onDeleteTodo,
                onEdit = onEditTodo,
                onShowFullScreenImage = onShowFullScreenImage,
                onDownloadImage = onDownloadImage,
                onSetReminder = onSetReminder,
                onRemoveReminder = onRemoveReminder
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoItem(
    todo: Todo,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (Int, String, Uri?) -> Unit,
    onShowFullScreenImage: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onSetReminder: (Int, Date) -> Unit,
    onRemoveReminder: (Int) -> Unit
) {
    var showDeletePrompt by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember(todo.id) { mutableStateOf(todo.task) }
    var editedImageUri by remember { mutableStateOf<Uri?>(todo.imageUri?.let { Uri.parse(it) }) }
    var hasImageChanged by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            editedImageUri = uri
            hasImageChanged = true
        }
    )

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isEditing = !isEditing
                        if (isEditing) editedText = todo.task
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit task",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isEditing) {
                    BasicTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (todo.isCompleted) Color.Gray else Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (todo.isCompleted) 0.7f else 1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TaskStatusToggle(
                    isCompleted = todo.isCompleted,
                    onToggle = { onToggle(todo.id) }
                )

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showReminderDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (todo.reminder != null) Icons.Filled.Alarm else Icons.Outlined.AlarmAdd,
                        contentDescription = if (todo.reminder != null) "Edit Reminder" else "Set Reminder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            imageVector = if (hasImageChanged) Icons.Default.Check else Icons.Default.Image,
                            contentDescription = "Change Image"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (hasImageChanged) "Image Changed" else "Change Image")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        isEditing = false
                        editedText = todo.task
                        editedImageUri = todo.imageUri?.let { Uri.parse(it) }
                        hasImageChanged = false
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onEdit(todo.id, editedText, editedImageUri)
                            isEditing = false
                            hasImageChanged = false
                        },
                        enabled = editedText.isNotBlank() && (editedText != todo.task || editedImageUri != todo.imageUri?.let { Uri.parse(it) })
                    ) {
                        Text("Save")
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                todo.imageUri?.let { imageUriString ->
                    AsyncImage(
                        model = imageUriString,
                        contentDescription = "Todo Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShowFullScreenImage(imageUriString) },
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onDownloadImage(imageUriString) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download Image")
                        }
                    }
                }

                todo.reminder?.let { reminderDate ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reminder: ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(reminderDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onRemoveReminder(todo.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove Reminder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReminderDialog) {
        ReminderDialog(
            currentReminder = todo.reminder,
            onDismiss = { showReminderDialog = false },
            onSetReminder = { date ->
                onSetReminder(todo.id, date)
                showReminderDialog = false
            }
        )
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
fun ReminderDialog(
    currentReminder: Date?,
    onDismiss: () -> Unit,
    onSetReminder: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf(currentReminder ?: Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder") },
        text = {
            Column {
                DatePicker(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(
                    selectedTime = selectedDate,
                    onTimeSelected = { selectedDate = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSetReminder(selectedDate) }) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(selectedDate: Date, onDateSelected: (Date) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Date: ${selectedDate.toFormattedString()}")
        IconButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(selectedTime: Date, onTimeSelected: (Date) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance().apply { time = selectedTime }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Time: ${selectedTime.toFormattedTimeString()}")
        IconButton(onClick = { showTimePicker = true }) {
            Icon(Icons.Default.Schedule, contentDescription = "Select Time")
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    onTimeSelected(calendar.time)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProgressGauge(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .size(60.dp, 120.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(30.dp),
                clip = true
            )
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0x40FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xCC1A237E),
                        0.3f to Color(0xCC3949AB),
                        0.7f to Color(0xCC3F51B5),
                        1.0f to Color(0xCC5C6BC0)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f - animatedProgress)
                .align(Alignment.TopCenter)
                .background(Color(0x80FFFFFF))
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Task Progress Icon",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FullScreenImageViewer(
    imageUri: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { _ ->
                        scale = if (scale > 1f) 1f else 2f
                    },
                    onTap = { onDismiss() }
                )
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Full-screen image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        scale *= zoom
                        rotation += rotate
                        offset += pan
                    }
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun SortDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Tasks") },
        text = {
            Column {
                SortOption(
                    text = "Date (Newest First)",
                    selected = currentSortOrder == SortOrder.DATE_DESC,
                    onClick = { onSortOrderSelected(SortOrder.DATE_DESC) }
                )
                SortOption(
                    text = "Date (Oldest First)",
                    selected = currentSortOrder == SortOrder.DATE_ASC,
                    onClick = { onSortOrderSelected(SortOrder.DATE_ASC) }
                )
                SortOption(
                    text = "Alphabetically (A-Z)",
                    selected = currentSortOrder == SortOrder.ALPHABET_ASC,
                    onClick = { onSortOrderSelected(SortOrder.ALPHABET_ASC) }
                )
                SortOption(
                    text = "Alphabetically (Z-A)",
                    selected = currentSortOrder == SortOrder.ALPHABET_DESC,
                    onClick = { onSortOrderSelected(SortOrder.ALPHABET_DESC) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SortOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun TaskStatusToggle(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val switchWidth = 62.dp
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

fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(this)
}

fun Date.toFormattedTimeString(): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(this)
}

enum class TaskCategory {
    TODO, DONE, ALL
}

enum class SortOrder {
    DATE_DESC, DATE_ASC, ALPHABET_ASC, ALPHABET_DESC
}