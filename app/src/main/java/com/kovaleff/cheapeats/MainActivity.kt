package com.kovaleff.cheapeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kovaleff.cheapeats.ui.home.HomeScreen
import com.kovaleff.cheapeats.ui.map.MapScreen
import com.kovaleff.cheapeats.ui.theme.CheapEatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheapEatsTheme {
                CheapEatsApp()
            }
        }
    }
}

@Composable
fun CheapEatsApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenMap = { navController.navigate("map") },
                onOpenCategory = { categoryId -> navController.navigate("map?categoryId=$categoryId") },
            )
        }
        composable(
            route = "map?categoryId={categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId")?.toLongOrNull()
            MapScreen(
                onBack = { navController.popBackStack() },
                preselectedCategoryId = categoryId,
            )
        }
    }
}
