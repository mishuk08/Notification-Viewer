package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NotificationDatabase
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import com.example.ui.FluidInboxViewModel
import com.example.ui.FluidInboxViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database & viewmodel repository direct injection
        val database = NotificationDatabase.getDatabase(applicationContext)
        val repository = NotificationRepository(database.notificationDao())
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FluidInboxViewModel = viewModel(
                    factory = FluidInboxViewModelFactory(repository)
                )
                
                // Track lifecycle permissions check on activity resume
                val context = LocalContext.current
                DisposableEffect(Unit) {
                    viewModel.checkPermission(context)
                    onDispose {}
                }

                var showHistory by remember { mutableStateOf(false) }
                val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
                val isEnglish = currentLang == "en"
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF070708) // OLED Deep iOS Slate Black background
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Blurred iOS-like background lights
                        BackgroundAuraGlow()
 
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding())
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Top header layout with history button listener and language switcher
                            iOSHeaderSection(
                                viewModel = viewModel,
                                isEnglish = isEnglish,
                                onHistoryClick = { showHistory = true }
                            )
 
                            // Main Scrollable Area with simulated items and real notification stream
                            Box(modifier = Modifier.weight(1f)) {
                                MainAppContent(viewModel = viewModel, isEnglish = isEnglish)
                            }
                        }
 
                        // iOS Dynamic Island Notification overlay at the absolute top center
                        LiquidDynamicIslandOverlay(
                            viewModel = viewModel,
                            isEnglish = isEnglish,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )

                        // Cool overlay modal showing older history entries up to 30 days
                        HistoryOverlay(
                            visible = showHistory,
                            viewModel = viewModel,
                            isEnglish = isEnglish,
                            onClose = { showHistory = false }
                        )
                    }
                }
            }
        }
    }
}

// Background atmospheric iOS ambient ambient-glows
@Composable
fun BackgroundAuraGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlowTransition")

    // Smooth looping coordinates for liquid flow effect
    val floatX1 by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob1X"
    )
    val floatY1 by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob1Y"
    )

    val floatX2 by infiniteTransition.animateFloat(
        initialValue = 80f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2X"
    )
    val floatY2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2Y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .offset(x = floatX1.dp, y = floatY1.dp)
                .graphicsLayer { alpha = 0.85f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x200A84FF), // Ambient iOS blue
                            Color(0x050A84FF),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(450.dp)
                .offset(x = floatX2.dp, y = floatY2.dp)
                .graphicsLayer { alpha = 0.75f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1530D158), // Green liquid drop
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// Standard header showing permissions and overall inbox count
@Composable
fun iOSHeaderSection(
    viewModel: FluidInboxViewModel,
    isEnglish: Boolean,
    onHistoryClick: () -> Unit
) {
    val isGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isEnglish) "Task Viewer" else "টাস্ক ভিউয়ার",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = if (isEnglish) "Task Viewer Dashboard" else "টাস্ক ভিউয়ার ড্যাশবোর্ড",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93), // iOS Dark Gray text color
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive History Access button (user request)
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = if (isEnglish) "View Older History (Up to 30 days)" else "৩০ দিনের পুরনো হিস্টরি দেখুন",
                        tint = Color(0xFF0A84FF) // iOS active Blue accent
                    )
                }

                // Interactive Language switch button instead of clear all (user request)
                IconButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = if (isEnglish) "Switch to Bengali" else "ইংরেজিতে পরিবর্তন করুন",
                        tint = Color(0xFF00FFCC) // Neon teal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Beautiful Dynamic warning banner if Listener permission is disabled
        if (!isGranted) {
            Card(
                onClick = {
                    try {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        } catch (ex: Exception) { }
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x30FF9F0A), // Transparent warning Orange
                ),
                border = BorderStroke(1.dp, Color(0x70FF9F0A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9F0A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert Permission",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) "Enable Live Notifications" else "লাইভ নোটিফিকেশন চালু করুন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD60A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) "Grant access so Task Viewer can capture message and call notifications from your device apps." else "আপনার ফোনের সব অ্যাপের মেসেজ ও কল ধরতে এই নোটিফিকেশন বাটন ক্লিক করে সেট করুন।",
                            fontSize = 11.sp,
                            color = Color(0xFFE5E5EA),
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Open Settings",
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: FluidInboxViewModel,
    isEnglish: Boolean
) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Aesthetic Segmented sliding horizontal pills
        LiquidSegmentedController(
            selectedFilter = currentFilter,
            onFilterSelected = { viewModel.setFilter(it) },
            isEnglish = isEnglish,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // iOS Search Bar
        StatusBarSearchBar(
            query = searchQuery,
            onQueryChanged = { viewModel.setSearchQuery(it) },
            isEnglish = isEnglish,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // History list Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isEnglish) "Inbox Feed (${items.size})" else "ইনবক্স ফিড (${items.size})",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Bold
            )

            if (items.any { !it.isRead }) {
                Text(
                    text = if (isEnglish) "Mark all as read" else "সব পঠিত চিহ্নিত করুন",
                    fontSize = 12.sp,
                    color = Color(0xFF0A84FF),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.markAllAsRead() }
                )
            }
        }

        // List Scroll wrapped in adaptive weighting box
        Box(modifier = Modifier.weight(1f)) {
            if (items.isEmpty()) {
                EmptyInboxState(isEnglish = isEnglish, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = items,
                        key = { it.id }
                    ) { item ->
                        UnifiedInboxRowItem(
                            item = item,
                            isEnglish = isEnglish,
                            onMarkRead = { viewModel.markAsRead(item.id) },
                            onDelete = { viewModel.deleteNotification(item.id) }
                        )
                    }
                }
            }
        }

        // Stylish dynamic footer displaying developed by credit
        DeveloperFooter()
    }
}

@Composable
fun DeveloperFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "FooterLiquidPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Smooth horizontal neon divider line
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x30FFFFFF),
                                Color(0x900A84FF),
                                Color(0x30FFFFFF)
                            )
                        )
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, Color(0xFF1E1E22), RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/mishuk008"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // safety fallback
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Shiny pulsating neon indicator to represent active service listening
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer { alpha = pulseAlpha }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00FFCC),
                                    Color(0xFF0A84FF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Developed by Mishuk Babu Shoukhin",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE5E5EA),
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// iOS Interactive Segmented filter with sweet spring animation sliding background pill
@Composable
fun LiquidSegmentedController(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val filters = listOf("ALL", "MESSAGES", "CALLS")
    val selectedIndex = filters.indexOf(selectedFilter)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .padding(4.dp)
    ) {
        // Sliding indicator with organic bouncing springs
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cellWidth = maxWidth / 3
            val offsetX by animateDpAsState(
                targetValue = cellWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "SegmentSlide"
            )

            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .offset(x = offsetX)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E))
                        )
                    )
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            filters.forEach { filter ->
                val displayLabel = if (isEnglish) {
                    when (filter) {
                        "ALL" -> "All"
                        "MESSAGES" -> "Messages"
                        "CALLS" -> "Calls"
                        else -> filter
                    }
                } else {
                    when (filter) {
                        "ALL" -> "সব প্ল্যাটফর্ম"
                        "MESSAGES" -> "মেসেজ"
                        "CALLS" -> "কলিং"
                        else -> filter
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onFilterSelected(filter) }
                        .testTag("segment_${filter.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayLabel,
                        color = if (selectedFilter == filter) Color.White else Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// iOS style glass search bar
@Composable
fun StatusBarSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        prefix = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = if (isEnglish) "Search icon" else "অনুসন্ধান আইকন",
                tint = Color(0xFF8E8E93),
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        },
        suffix = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isEnglish) "Clear" else "মুছুন",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChanged("") }
                )
            }
        },
        placeholder = {
            Text(
                if (isEnglish) "Search sender or message content..." else "মেসেজদাতা বা লেখার বিষয় খুঁজুন...",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1C1C1E),
            unfocusedContainerColor = Color(0xFF1C1C1E),
            focusedBorderColor = Color(0xFF2C2C2E),
            unfocusedBorderColor = Color(0xFF1C1C1E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("search_bar")
    )
}

// Empty State View
@Composable
fun EmptyInboxState(
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AllInbox,
                contentDescription = if (isEnglish) "Empty" else "খালি",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isEnglish) "No new calls or messages!" else "কোনো নতুন কল বা বার্তা নেই!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isEnglish) {
                "Trigger a demo simulation from the controls, or enable live notification access to display actual calls and messages here."
            } else {
                "সিমুলেটর দিয়ে ডেমো ট্র্রিগার করুন অথবা নোটিফিকেশন সার্ভিসটি সচল করুন যাতে যেকোনো অ্যাপের কল-মেসেজ এখানে ক্যাচ করে দেখায়।"
            },
            fontSize = 12.sp,
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// High visual fidelity: Detailed unified row item for notifications list
@Composable
fun UnifiedInboxRowItem(
    item: NotificationEntity,
    isEnglish: Boolean,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val platformColors = getPlatformAestheticColors(item.platform)
    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (item.isRead) Color(0xFF141416) else Color(0xFF1B1D24))
            .border(
                width = 1.2.dp,
                brush = if (item.isRead) {
                    androidx.compose.ui.graphics.SolidColor(Color(0xFF1E1E22))
                } else {
                    Brush.sweepGradient(
                        colors = listOf(
                            platformColors.primaryAccent.copy(alpha = 0.9f),
                            Color(0x35FFFFFF),
                            platformColors.primaryAccent.copy(alpha = 0.2f),
                            platformColors.primaryAccent.copy(alpha = 0.9f)
                        )
                    )
                },
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp)
            .testTag("inbox_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Elegant liquid avatar generator with beautiful sender seed dynamic values
            SenderLiquidAvatar(
                name = item.title,
                seed = item.senderAvatarSeed,
                platformColor = platformColors.primaryAccent,
                platform = item.platform
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title (Sender details)
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Relative Time
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // App Brand Badge & category (message vs call status)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(platformColors.badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = getPlatformIcon(item.platform),
                        contentDescription = null,
                        tint = platformColors.primaryAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val categoryLabel = if (isEnglish) {
                        if (item.type == "CALL") "Incoming Call" else "Message"
                    } else {
                        if (item.type == "CALL") "ইনকামিং কল" else "বার্তা"
                    }
                    Text(
                        text = "${item.appName} • $categoryLabel",
                        fontSize = 9.sp,
                        color = platformColors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Notification Content Message
                Text(
                    text = item.message,
                    fontSize = 13.sp,
                    color = Color(0xFFE5E5EA),
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Row action tasks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.isRead) {
                            Button(
                                onClick = onMarkRead,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2C2C2E)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Mark Read" else "পঠিত করুন",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (item.type == "CALL") {
                            Button(
                                onClick = { /* Perform direct phone intent or call back simulated action */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x2030D158)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Call Back" else "ব্যাক কল",
                                        fontSize = 10.sp,
                                        color = Color(0xFF30D158),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Item",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Gorgeous Custom liquid avatars derived from title initials utilizing geometric spring circles
@Composable
fun SenderLiquidAvatar(
    name: String,
    seed: Int,
    platformColor: Color,
    platform: String
) {
    val initials = remember(name) {
        if (name.isBlank()) "S" else {
            val parts = name.trim().split(" ")
            if (parts.size >= 2) {
                "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            } else {
                "${name.firstOrNull() ?: "S"}".uppercase()
            }
        }
    }

    // High fidelity color palettes using circular gradients to feel bouncy and luxury
    val gradient = remember(seed) {
        val color1 = when (seed % 5) {
            0 -> Color(0xFF0A84FF)
            1 -> Color(0xFFFF9F0A)
            2 -> Color(0xFFBF5AF2)
            3 -> Color(0xFFFF2D55)
            else -> Color(0xFF30D158)
        }
        val color2 = when ((seed + 2) % 5) {
            0 -> Color(0xFF64D2FF)
            1 -> Color(0xFFFFD60A)
            2 -> Color(0xFFD30D15)
            3 -> Color(0xFFFF375F)
            else -> Color(0xFF00C7BE)
        }
        Brush.linearGradient(listOf(color1, color2))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AvatarBreathing")
    val morphRatio by infiniteTransition.animateFloat(
        initialValue = 23f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800 + (seed % 800), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MorphRatio"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(morphRatio.dp))
            .background(gradient)
            .border(1.5.dp, Color(0x30FFFFFF), RoundedCornerShape(morphRatio.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Little floating brand app icon at the bottom-left corner
        Box(
            modifier = Modifier
                .size(17.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color.Black, CircleShape)
                .padding(1.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getPlatformIcon(platform),
                contentDescription = platform,
                tint = platformColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ==========================================
// FLUID LIQUID DYNAMIC ISLAND SYSTEM
// ==========================================
@Composable
fun LiquidDynamicIslandOverlay(
    viewModel: FluidInboxViewModel,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val activeNotification by viewModel.activeIslandNotification.collectAsStateWithLifecycle()
    val isExpanded by viewModel.isIslandExpanded.collectAsStateWithLifecycle()

    if (activeNotification != null) {
        val item = activeNotification!!

        // Auto collapse notification after 7 seconds if message, or keeps open on incoming CALL
        LaunchedEffect(item.id) {
            if (item.type != "CALL") {
                delay(7000)
                viewModel.clearActiveIsland()
            }
        }

        // Animated island size with true liquid feel ratios
        val expandedWidth = 345.dp
        val collapsedWidth = 145.dp
        val expandedHeight = if (item.type == "CALL") 190.dp else 135.dp
        val collapsedHeight = 36.dp

        val targetWidth = if (isExpanded) expandedWidth else collapsedWidth
        val targetHeight = if (isExpanded) expandedHeight else collapsedHeight

        val widthAnim by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(
                dampingRatio = 0.58f, // custom liquid spring physics
                stiffness = 140f
            ),
            label = "IslandWidth"
        )
        val heightAnim by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = spring(
                dampingRatio = 0.58f,
                stiffness = 140f
            ),
            label = "IslandHeight"
        )

        val scaleAnim by animateFloatAsState(
            targetValue = if (isExpanded) 1.0f else 0.96f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "IslandScale"
        )

        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
                .width(widthAnim)
                .height(heightAnim)
                .clip(RoundedCornerShape(32.dp)) // squircle
                .background(Color.Black)
                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(32.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isExpanded) {
                        viewModel.setIslandExpanded(true)
                    }
                }
                .testTag("dynamic_island")
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(120, delayMillis = 40)) + 
                            scaleIn(initialScale = 0.85f, animationSpec = tween(150)))
                        .togetherWith(fadeOut(animationSpec = tween(110)))
                },
                label = "IslandStateContent"
            ) { state ->
                if (state) {
                    // Expanded Mode
                    ExpandedIslandContent(
                        item = item,
                        isEnglish = isEnglish,
                        onCollapse = { viewModel.setIslandExpanded(false) },
                        onDismiss = { viewModel.clearActiveIsland() }
                    )
                } else {
                    // Compact Mode (Resting dynamic green/blue dot indicators)
                    CompactIslandContent(item = item, isEnglish = isEnglish)
                }
            }
        }
    } else {
        // Safe default micro pill when nothing is active (Static Minimal Notch)
        Box(
            modifier = modifier
                .width(110.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
        )
    }
}

// Compact Island showing mini app icon and incoming calling or text status dots
@Composable
fun CompactIslandContent(
    item: NotificationEntity,
    isEnglish: Boolean
) {
    val platformColor = getPlatformAestheticColors(item.platform).primaryAccent

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = getPlatformIcon(item.platform),
                contentDescription = null,
                tint = platformColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val stateText = if (isEnglish) {
                if (item.type == "CALL") "Calling..." else "Message"
            } else {
                if (item.type == "CALL") "কলিং..." else "বার্তা"
            }
            Text(
                text = stateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Simulated dynamic iOS green calling indicator wave or incoming message dot
        if (item.type == "CALL") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeating voice visualizers with simple infinite float rotation / scaling
                val infiniteTransition = rememberInfiniteTransition(label = "CompactWave")
                val floatHeight1 by infiniteTransition.animateFloat(
                    initialValue = 0.4f, targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
                    label = "DotH1"
                )
                val floatHeight2 by infiniteTransition.animateFloat(
                    initialValue = 1.0f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(animation = tween(450), repeatMode = RepeatMode.Reverse),
                    label = "DotH2"
                )
                val floatHeight3 by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(animation = tween(380), repeatMode = RepeatMode.Reverse),
                    label = "DotH3"
                )

                Box(modifier = Modifier.size(2.dp, 8.dp * floatHeight1).background(Color(0xFF30D158), CircleShape))
                Box(modifier = Modifier.size(2.dp, 8.dp * floatHeight2).background(Color(0xFF30D158), CircleShape))
                Box(modifier = Modifier.size(2.dp, 8.dp * floatHeight3).background(Color(0xFF30D158), CircleShape))
            }
        } else {
            // Little neon orange notify dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9F0A))
            )
        }
    }
}

// Gorgeous iOS expanded details view
@Composable
fun ExpandedIslandContent(
    item: NotificationEntity,
    isEnglish: Boolean,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit
) {
    val platformColor = getPlatformAestheticColors(item.platform).primaryAccent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App identifier row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(platformColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPlatformIcon(item.platform),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val statusTextHeight = if (isEnglish) {
                    if (item.type == "CALL") "Incoming Audio Call" else "New Message"
                } else {
                    if (item.type == "CALL") "ইনকামিং অডিও কল" else "নতুন বার্তা"
                }
                Text(
                    text = "${item.appName} • $statusTextHeight",
                    fontSize = 12.sp,
                    color = Color(0xFFE5E5EA),
                    fontWeight = FontWeight.Bold
                )
            }

            // iOS Close Button
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Details segment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar generator inside Island
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(platformColor, Color(0xFF1C1C1E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.title.firstOrNull()?.toString()?.uppercase() ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = Color(0xFFE5E5EA),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        // Action Buttons Row (Swipe answer slider aesthetic or close action)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.type == "CALL") {
                // Fluid Call Answer Slider / Buttons
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF453A) // iOS Red Decline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Decline" else "কেটে দিন",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF30D158) // iOS Green Answer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Answer" else "রিসিভ করুন",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Messages quick confirm button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x30FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Mark Read" else "পড়া শেষ",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = platformColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Quick Reply" else "অনুরূপ রিপ্লাই",
                        fontSize = 11.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Map platforms to correct vector symbols
fun getPlatformIcon(platform: String): ImageVector {
    return when (platform) {
        "whatsapp" -> Icons.Rounded.Forum
        "messenger" -> Icons.Rounded.ChatBubbleOutline
        "telegram" -> Icons.Rounded.Send
        "instagram" -> Icons.Rounded.CameraAlt
        "phone" -> Icons.Rounded.PhoneInTalk
        "sms" -> Icons.Rounded.Sms
        "gmail" -> Icons.Rounded.Email
        else -> Icons.Rounded.NotificationsActive
    }
}

data class BrandAesthetics(val primaryAccent: Color, val badgeBg: Color)

// Dynamic brand aesthetic map
fun getPlatformAestheticColors(platform: String): BrandAesthetics {
    return when (platform) {
        "whatsapp" -> BrandAesthetics(Color(0xFF25D366), Color(0x1525D366))
        "messenger" -> BrandAesthetics(Color(0xFF0084FF), Color(0x150084FF))
        "telegram" -> BrandAesthetics(Color(0xFF229ED9), Color(0x15229ED9))
        "instagram" -> BrandAesthetics(Color(0xFFE1306C), Color(0x15E1306C))
        "phone" -> BrandAesthetics(Color(0xFF30D158), Color(0x1530D158))
        "sms" -> BrandAesthetics(Color(0xFF0A84FF), Color(0x150A84FF))
        "gmail" -> BrandAesthetics(Color(0xFFFF453A), Color(0x15FF453A))
        else -> BrandAesthetics(Color(0xFFFF9F0A), Color(0x15FF9F0A))
    }
}

// Interactive History Slider Overlay - Displays items older than 24 Hours up to 30 Days (user request)
@Composable
fun HistoryOverlay(
    visible: Boolean,
    viewModel: FluidInboxViewModel,
    isEnglish: Boolean,
    onClose: () -> Unit
) {
    val historyItems by viewModel.historyState.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070708)) // Dark premium OLED canvas
        ) {
            // Re-render the background lights for fluid look
            BackgroundAuraGlow()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) "History Feed" else "হিস্টরি ফিড",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) "Notifications from the last 30 days" else "সর্বোচ্চ ৩০ দিন আগের নোটিফিকেশন সমূহ",
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Done/Back Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEnglish) "Close History" else "বন্ধ করুন",
                            tint = Color.White
                        )
                    }
                }

                // Styled Separator Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 22.dp)
                )

                // List Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (historyItems.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF141416)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isEnglish) "No history found" else "কোনো হিস্টরি পাওয়া যায়নি",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEnglish) {
                                    "Notifications and messages older than 24 hours are automatically moved here and kept safely for up to 30 days."
                                } else {
                                    "১ দিনের চেয়ে পুরোনো সব ইনকামিং মেসেজ ও কল নোটিফিকেশন স্বয়ংক্রিয়ভাবে এখানে ৩০ দিন পর্যন্ত নিরাপদে সংরক্ষিত থাকবে।"
                                },
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = historyItems,
                                key = { it.id }
                            ) { item ->
                                UnifiedInboxRowItem(
                                    item = item,
                                    isEnglish = isEnglish,
                                    onMarkRead = { viewModel.markAsRead(item.id) },
                                    onDelete = { viewModel.deleteNotification(item.id) }
                                )
                            }
                        }
                    }
                }

                // Credit Footer at bottom
                DeveloperFooter()
            }
        }
    }
}
