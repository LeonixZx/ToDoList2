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

import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

import android.util.Log
import android.os.Handler
import android.os.Looper

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.MobileAds
import android.provider.Settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlin.math.pow
import kotlin.random.Random

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.filled.DirectionsRun

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import java.io.File

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
        uri?.let { todoViewModel.exportTasks(contentResolver, it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { todoViewModel.importTasks(contentResolver, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    private var lastAdLoadAttempt = 0L
    private val AD_LOAD_COOLDOWN = 60000L // 1 minute cooldown

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
            .background(Color(0x40FFFFFF)) // Very transparent white background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xCC1A237E),  // Dark blue at bottom (80% opaque)
                        0.3f to Color(0xCC3949AB),  // Slightly brighter blue at 30% (80% opaque)
                        0.7f to Color(0xCC3F51B5),  // Even brighter blue at 70% (80% opaque)
                        1.0f to Color(0xCC5C6BC0)   // Brightest blue at top (80% opaque)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f - animatedProgress)
                .align(Alignment.TopCenter)
                .background(Color(0x80FFFFFF))  // Semi-transparent overlay
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
                    .data(data = R.drawable.todo_animation) // Make sure this matches your GIF filename
                    .apply(block = {
                        size(coil.size.Size.ORIGINAL)
                    }).build(),
                imageLoader = imageLoader
            ),
            contentDescription = "Animated GIF",
            modifier = Modifier
                .size(400.dp) //
                .align(Alignment.CenterEnd)
                .padding(end = 1.dp) // 16
                .alpha(0.3f), // Adjust alpha to make it more or less visible 0.3f
            contentScale = ContentScale.Crop //FIT, or Crop
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.ALL) }
    var newTodoText by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedAttachments = uris
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSection(
            todoViewModel = todoViewModel,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            onExport = onExport,
            onImport = onImport
        )
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddTodoInput(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                onAddClick = {
                    if (newTodoText.isNotBlank()) {
                        todoViewModel.addTodo(newTodoText, selectedAttachments)
                        newTodoText = ""
                        selectedAttachments = emptyList()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Attach file")
            }
        }

        TodoList(
            todos = todoViewModel.todos,
            onToggleTodo = { todoViewModel.toggleTodo(it) },
            onDeleteTodo = { todoViewModel.deleteTodo(it) },
            onEditTodo = { id, newText, newAttachments ->
                todoViewModel.editTodo(id, newText, newAttachments)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
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
fun TodoList(
    todos: List<Todo>,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit,
    onEditTodo: (Int, String, List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(todos) { todo ->
            TodoItem(
                todo = todo,
                onToggle = { onToggleTodo(todo.id) },
                onDelete = { onDeleteTodo(todo.id) },
                onEdit = { newText, newAttachments ->
                    onEditTodo(todo.id, newText, newAttachments)
                }
            )
        }
    }
}



@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String, List<Uri>) -> Unit
) {
    var showDeletePrompt by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember(todo.id) { mutableStateOf(todo.task) }
    var editedAttachments by remember(todo.id) { mutableStateOf<List<Uri>>(emptyList()) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        editedAttachments = uris
    }

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
                        if (isEditing) {
                            editedText = todo.task
                            editedAttachments = emptyList()
                        }
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

                // Display attachments
                if (todo.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todo.attachments.forEach { attachment ->
                            Image(
                                painter = rememberAsyncImagePainter(File(attachment)),
                                contentDescription = "Attached image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        isEditing = false
                        editedText = todo.task
                        editedAttachments = emptyList()
                    }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onEdit(editedText, editedAttachments)
                            isEditing = false
                        },
                        enabled = editedText.isNotBlank() && (editedText != todo.task || editedAttachments.isNotEmpty())
                    ) {
                        Text("Save")
                    }
                    IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach file")
                    }
                }

                // Display edited attachments
                if (editedAttachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        editedAttachments.forEach { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Edited attachment",
                                modifier = Modifier
                                    .size(100.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
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

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-2107817689571311/2329181913" // Your mediated banner ad unit ID  ca-app-pub-3940256099942544/6300978111 (Test Ids)
                loadAd(AdManager.createAdRequest())
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdMob", "Mediated banner ad loaded successfully")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdMob", "Mediated banner ad failed to load. Error: ${error.message}")
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
                    }
                    override fun onAdOpened() {
                        Log.d("AdMob", "Mediated banner ad opened")
                    }
                    override fun onAdClicked() {
                        Log.d("AdMob", "Mediated banner ad clicked")
                    }
                    override fun onAdClosed() {
                        Log.d("AdMob", "Mediated banner ad closed")
                    }
                    override fun onAdImpression() {
                        Log.d("AdMob", "Mediated banner ad impression recorded")
                    }
                }
            }
        }
    )
}

@Composable
fun AdMobInterstitial(onAdClosed: () -> Unit) {
    val context = LocalContext.current
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    LaunchedEffect(Unit) {
        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712", // Your mediated interstitial ad unit ID
            AdManager.createAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdMob", "Mediated interstitial ad loaded successfully")
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("AdMob", "Mediated interstitial ad was dismissed")
                            interstitialAd = null
                            onAdClosed()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdMob", "Mediated interstitial ad failed to show. Error: ${adError.message}")
                            interstitialAd = null
                            onAdClosed()
                        }
                        override fun onAdShowedFullScreenContent() {
                            Log.d("AdMob", "Mediated interstitial ad showed fullscreen content")
                        }
                        override fun onAdClicked() {
                            Log.d("AdMob", "Mediated interstitial ad was clicked")
                        }
                        override fun onAdImpression() {
                            Log.d("AdMob", "Mediated interstitial ad impression recorded")
                        }
                    }
                    showInterstitialAd(context as Activity, ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Mediated interstitial ad failed to load. Error: ${error.message}")
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
                    onAdClosed()
                }
            }
        )
    }
}

private fun showInterstitialAd(activity: Activity, ad: InterstitialAd?) {
    if (ad != null) {
        ad.show(activity)
    } else {
        Log.w("AdMob", "Interstitial ad was not ready yet.")
    }
}

enum class TaskCategory {
    TODO, DONE, ALL
}