package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HabitHeroTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userProfile by viewModel.userProfile.collectAsState()
            val selectedTab by viewModel.selectedTab.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()
            val context = LocalContext.current

            val themeMode = userProfile?.themeMode ?: "SYSTEM"
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            HabitHeroTheme(darkTheme = useDarkTheme) {
                Scaffold(
                    bottomBar = {
                        HabitHeroBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.setSelectedTab(it) }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> CalendarScreen(viewModel = viewModel)
                            2 -> StatisticsScreen(viewModel = viewModel)
                            3 -> AiCoachScreen(viewModel = viewModel)
                            4 -> ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun HabitHeroBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationTabItem("Home", Icons.Default.Home, "nav_home"),
        NavigationTabItem("Calendar", Icons.Default.CalendarMonth, "nav_calendar"),
        NavigationTabItem("Stats", Icons.Default.BarChart, "nav_stats"),
        NavigationTabItem("AI Coach", Icons.Default.AutoAwesome, "nav_ai_coach"),
        NavigationTabItem("Profile", Icons.Default.Person, "nav_profile")
    )

    NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    selectedTextColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
