package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.data.ChannelEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.CountryItem
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val isTabletOrTv = configuration.screenWidthDp >= 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Auto-dismiss import toast message
    LaunchedEffect(state.importMessage) {
        if (state.importMessage != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.dismissImportMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackgroundStart, DarkBackgroundEnd)))
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (!isTabletOrTv && state.currentScreen !is Screen.Player) {
                    PhoneBottomNavigation(
                        currentScreen = state.currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive Side Navigation for Android TV or Landscape Phone
                if (isTabletOrTv && state.currentScreen !is Screen.Player) {
                    TvSideNavigation(
                        currentScreen = state.currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }

                // Screen Area with custom Transitions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color.Transparent)
                ) {
                when (val screen = state.currentScreen) {
                    is Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        allChannels = state.allChannels,
                        isLoading = state.isLoading
                    )
                    is Screen.LiveMatches -> LiveMatchesScreen(
                        viewModel = viewModel,
                        matches = viewModel.getLiveMatches()
                    )
                    is Screen.Countries -> CountriesScreen(
                        viewModel = viewModel,
                        countries = viewModel.getCountries()
                    )
                    is Screen.Favorites -> FavoritesScreen(
                        viewModel = viewModel,
                        favorites = state.favorites
                    )
                    is Screen.Search -> SearchScreen(
                        viewModel = viewModel,
                        allChannels = state.allChannels
                    )
                    is Screen.Settings -> SettingsScreen(
                        viewModel = viewModel
                    )
                    is Screen.CountryDetail -> CountryDetailScreen(
                        viewModel = viewModel,
                        countryName = screen.countryName,
                        allChannels = state.allChannels
                    )
                    is Screen.Player -> Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
                        com.example.ui.components.VideoPlayer(
                            channel = screen.channel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }

                // Import feedback snackbar / banner
                state.importMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(0.9f)
                            .testTag("notification_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant, contentColor = Color.White),
                        border = BorderStroke(1.dp, LightTurquoise)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.dismissImportMessage() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Bottom Bar navigation for Portrait phones
@Composable
fun PhoneBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1E1B4B),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Screen.Home, "Home", Icons.Default.Home),
            Triple(Screen.LiveMatches, "Matches", Icons.Default.PlayArrow),
            Triple(Screen.Countries, "Countries", Icons.Default.List),
            Triple(Screen.Favorites, "Favorites", Icons.Default.Favorite),
            Triple(Screen.Search, "Search", Icons.Default.Search)
        )

        items.forEach { (screen, label, icon) ->
            val isSelected = currentScreen::class == screen::class
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = LightTurquoise,
                    indicatorColor = PrimaryPurple,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_bottom_${label.lowercase()}")
            )
        }
    }
}

// Side Rail navigation for TV & Landscape
@Composable
fun TvSideNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF1E1B4B),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ayt_ikkou_logo_1783168393057),
                    contentDescription = "AYT_IKKOU TV Logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, LightTurquoise, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        },
        modifier = Modifier.width(100.dp)
    ) {
        val items = listOf(
            Triple(Screen.Home, "Home", Icons.Default.Home),
            Triple(Screen.LiveMatches, "Matches", Icons.Default.PlayArrow),
            Triple(Screen.Countries, "Countries", Icons.Default.List),
            Triple(Screen.Favorites, "Favorites", Icons.Default.Favorite),
            Triple(Screen.Search, "Search", Icons.Default.Search)
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { (screen, label, icon) ->
                val isSelected = currentScreen::class == screen::class
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f)

                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onNavigate(screen) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = LightTurquoise,
                        indicatorColor = PrimaryPurple,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier
                        .scale(scale)
                        .padding(vertical = 4.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .testTag("nav_side_${label.lowercase()}")
                )
            }
        }
    }
}

// Premium Grid Card for IPTV Dashboard
@Composable
fun GridCard(
    title: String,
    subtitle: String,
    flag: String,
    isLiveItem: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isFocused -> 1.10f
            else -> 1.0f
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "grid_card_scale"
    )
    
    val borderStroke = if (isFocused) {
        BorderStroke(2.5.dp, LightTurquoise)
    } else {
        BorderStroke(1.dp, Color(0x33FFFFFF))
    }
    
    val shadowElevation = if (isFocused) 20.dp else 4.dp
    val glowColor = if (isFocused) LightTurquoise else Color.Transparent

    // Gradient from Purple to Turquoise
    // For non-focused: a gorgeous subtle deep purple to deep teal/turquoise gradient
    // For focused: a highly vibrant bright purple to bright turquoise gradient
    val backgroundBrush = if (isFocused) {
        Brush.linearGradient(
            colors = listOf(PrimaryPurple, LightTurquoise)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF3B0764), Color(0xFF134E4A))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag(if (isLiveItem) "live_matches_grid_item" else "country_grid_item_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            // Subtle premium radial background glow when focused or for Live Match
            if (isLiveItem && !isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x33FF0000), Color.Transparent),
                                radius = 300f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLiveItem) {
                        // Large Red Live icon/indicator (3D style)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔴",
                                fontSize = 24.sp
                            )
                        }
                        
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "LIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        // Large Country flag (3D style with gradient background and shadow)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(PrimaryPurple.copy(alpha = 0.8f), LightTurquoise.copy(alpha = 0.8f))),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = flag,
                                fontSize = 28.sp
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = if (isFocused) Color(0xFF1E1B4B) else LightTurquoise,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Home Screen Layout
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    allChannels: List<ChannelEntity>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightTurquoise)
        }
        return
    }

    val countries = remember(allChannels) { viewModel.getCountries() }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = when {
        screenWidth >= 960 -> 5
        screenWidth >= 720 -> 4
        screenWidth >= 480 -> 3
        else -> 2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full Span Header
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large 3D style brand logo
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .size(130.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), clip = false)
                        .background(
                            Brush.linearGradient(listOf(PrimaryPurple, LightTurquoise)),
                            RoundedCornerShape(24.dp)
                        )
                        .border(2.dp, LightTurquoise, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ayt_ikkou_logo_1783168393057),
                        contentDescription = "AYT_IKKOU TV Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    text = "Premium IPTV & Match Broadcaster",
                    color = LightTurquoise,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Channels Portal",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // First item: Live Matches Grid Card
        item {
            val liveMatches = remember(allChannels) { viewModel.getLiveMatches() }
            val liveCount = liveMatches.size
            GridCard(
                title = "🔴 Live Matches",
                subtitle = if (liveCount > 0) "$liveCount active matches" else "View Live Matches",
                flag = "",
                isLiveItem = true,
                onClick = { viewModel.navigateTo(Screen.LiveMatches) }
            )
        }

        // Remaining items: Arabic Countries only
        items(countries) { country ->
            GridCard(
                title = country.name,
                subtitle = "${country.channelCount} channels",
                flag = country.flag,
                isLiveItem = false,
                onClick = { viewModel.navigateTo(Screen.CountryDetail(country.name)) }
            )
        }
    }
}

// Vertical item list for Countries
@Composable
fun CountryVerticalItem(
    country: CountryItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1.0f)
    val borderStroke = if (isFocused) BorderStroke(1.5.dp, LightTurquoise) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .testTag("country_item_${country.name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Large Flag
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = country.flag,
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = country.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${country.channelCount} Channels available",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open Country",
                tint = LightTurquoise
            )
        }
    }
}

// Country channels detail view
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailScreen(
    viewModel: MainViewModel,
    countryName: String,
    allChannels: List<ChannelEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val channels = remember(allChannels, countryName, searchQuery) {
        allChannels.filter { 
            it.country.equals(countryName, ignoreCase = true) &&
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .testTag("country_detail_back")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = LightTurquoise
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = countryName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${channels.size} channels total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightTurquoise
                )
            }
        }

        // Search Bar inside country
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search channel in $countryName...", color = TextSecondary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = LightTurquoise) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = LightTurquoise,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("country_channel_search")
        )

        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No channels match your search", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels) { channel ->
                    ChannelVerticalRowItem(
                        channel = channel,
                        onClick = { viewModel.selectChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

// Medium sized Channel Row component for lists
@Composable
fun ChannelVerticalRowItem(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1.0f)
    val borderStroke = if (isFocused) BorderStroke(1.5.dp, LightTurquoise) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .testTag("channel_row_${channel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("channel_fav_btn_${channel.id}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) Color.Red else TextSecondary
                )
            }
        }
    }
}

// Live Matches full screen view
@Composable
fun LiveMatchesScreen(
    viewModel: MainViewModel,
    matches: List<ChannelEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "🔴 Live Matches",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active live match broadcasts found.\nImport custom sports playlists inside settings.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(matches) { channel ->
                    ChannelVerticalRowItem(
                        channel = channel,
                        onClick = { viewModel.selectChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

// Countries list with search bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    viewModel: MainViewModel,
    countries: List<CountryItem>
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCountries = remember(countries, searchQuery) {
        countries.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Countries",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter countries list...", color = TextSecondary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = LightTurquoise) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = LightTurquoise,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("countries_filter_search")
        )

        if (filteredCountries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No countries match your filter", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCountries) { country ->
                    CountryVerticalItem(
                        country = country,
                        onClick = { viewModel.navigateTo(Screen.CountryDetail(country.name)) }
                    )
                }
            }
        }
    }
}

// Favorites screen
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    favorites: List<ChannelEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Local Favorites",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites saved yet",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click the heart icon on any channel to save it offline here.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { channel ->
                    ChannelVerticalRowItem(
                        channel = channel,
                        onClick = { viewModel.selectChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

// Global Search Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    allChannels: List<ChannelEntity>
) {
    var query by remember { mutableStateOf("") }
    
    val channels = remember(allChannels, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allChannels.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Instant Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search channel, country, or sport...", color = TextSecondary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = LightTurquoise) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedIndicatorColor = LightTurquoise,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("global_search_input")
        )

        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type channel details above to search instantly", color = TextSecondary)
                }
            }
        } else if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No channels match \"$query\"", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels) { channel ->
                    ChannelVerticalRowItem(
                        channel = channel,
                        onClick = { viewModel.selectChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

// Settings dashboard with playlist import
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    var importUrl by remember { mutableStateOf("") }
    var rawM3uText by remember { mutableStateOf("") }
    var expandedImportText by remember { mutableStateOf(false) }
    var expandedImportUrl by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Dashboard Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Section: IPTV Link Importer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📥 Dynamic IPTV Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add/replace M3U8 links. They will load instantly in your TV or phone screens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option A: Link Import
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expandedImportUrl = !expandedImportUrl },
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = LightTurquoise)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Import from remote M3U URL", color = Color.White)
                            }
                            Icon(
                                imageVector = if (expandedImportUrl) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    if (expandedImportUrl) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            TextField(
                                value = importUrl,
                                onValueChange = { importUrl = it },
                                placeholder = { Text("https://example.com/channels.m3u", color = TextSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedIndicatorColor = LightTurquoise
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("import_url_field")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (importUrl.isNotBlank()) {
                                        viewModel.importFromUrl(importUrl.trim(), "m3u")
                                        importUrl = ""
                                        expandedImportUrl = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightTurquoise, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().testTag("submit_import_url")
                            ) {
                                Text("Load Remote Stream List", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: Paste raw M3U text
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expandedImportText = !expandedImportText },
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = LightTurquoise)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Paste raw M3U playlist text", color = Color.White)
                            }
                            Icon(
                                imageVector = if (expandedImportText) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    if (expandedImportText) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            TextField(
                                value = rawM3uText,
                                onValueChange = { rawM3uText = it },
                                placeholder = { Text("#EXTM3U\n#EXTINF:-1,My Channel\nhttps://example.com/live.m3u8", color = TextSecondary) },
                                minLines = 4,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedIndicatorColor = LightTurquoise
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("import_raw_text_field")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (rawM3uText.isNotBlank()) {
                                        viewModel.importM3U(rawM3uText)
                                        rawM3uText = ""
                                        expandedImportText = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightTurquoise, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().testTag("submit_import_text")
                            ) {
                                Text("Parse & Import Playlist", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: System Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚙️ Actions & Troubleshooting",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Selection Note
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme Default", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Default system theme colors", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = LightTurquoise
                            ),
                            enabled = false
                        )
                    }

                    Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Clear Cache Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reset Local App Database", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Clears custom listings and resets to assets file", color = TextSecondary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.clearCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White),
                            modifier = Modifier.testTag("clear_cache_btn")
                        ) {
                            Text("Clear", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: About metadata
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ℹ️ App Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "App Name: AYT_IKKOU TV\n" +
                               "App Version: 1.0.0 (Stable)\n" +
                               "Architecture: MVVM + Jetpack Compose\n" +
                               "Player Engine: ExoPlayer Native HLS\n" +
                               "Data Source: https://github.com/iptv-org/iptv",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Privacy Policy: All streams, files, playlists, and favorited channels are saved 100% locally in secure app database containers on your device. Absolutely no data leaves your phone or TV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightTurquoise
                    )
                }
            }
        }
    }
}
