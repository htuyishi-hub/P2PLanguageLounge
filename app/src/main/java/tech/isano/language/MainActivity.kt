package tech.isano.language

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tech.isano.language.core.engine.network.InMemoryP2PManager
import tech.isano.language.feature.canvas.ui.CanvasScreen
import tech.isano.language.lounge.LoungeViewModel
import tech.isano.language.ui.MainScreen
import tech.isano.language.ui.NavTab
import tech.isano.language.ui.academy.TeacherLiveClassScreen
import tech.isano.language.ui.onboarding.OnboardingScreen
import tech.isano.language.ui.theme.P2PLanguageLoungeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            P2PLanguageLoungeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LanguageLoungeNavigation()
                }
            }
        }
    }
}

@Composable
fun LanguageLoungeNavigation() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application

    // Shared P2P engine instance for session communication across modules
    val p2pManager = remember { InMemoryP2PManager() }
    val loungeViewModel = remember { LoungeViewModel(application, p2pManager) }

    var initialTab by remember { mutableStateOf(NavTab.ACADEMY) }

    NavHost(
        navController = navController,
        startDestination = "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = { targetTab ->
                    initialTab = targetTab
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                initialTab = initialTab,
                loungeViewModel = loungeViewModel,
                onNavigateToCanvas = {
                    navController.navigate("canvas")
                },
                onNavigateToTeacherClass = {
                    navController.navigate("teacher_live_class")
                }
            )
        }
        composable("canvas") {
            CanvasScreen(
                onEndCall = {
                    loungeViewModel.leaveLobby()
                    navController.popBackStack()
                }
            )
        }
        composable("teacher_live_class") {
            TeacherLiveClassScreen(
                onEndClass = {
                    navController.popBackStack()
                }
            )
        }
    }
}
