package xyz.regulad.supir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import xyz.regulad.regulib.compose.firstState
import xyz.regulad.regulib.compose.toRoute
import xyz.regulad.supir.ui.nav.*
import xyz.regulad.supir.ui.theme.SupIRTheme

data class BottomNavigationItem<T> private constructor(
    val icon: ImageVector,
    val label: String,
    val route: T
) {
    companion object {
        val bottomNavigationItems = listOf(
            BottomNavigationItem(Icons.Default.Home, "Home", MainRoute),
            BottomNavigationItem(Icons.Default.Favorite, "Favorites", FavoritesRoute),
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewmodel: SupIRViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SupIRTheme {
                val navController = rememberNavController()

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.toRoute<Any>()

                val scaffoldTopbarTitle = when (currentRoute) {
                    is RouteWithTopBar -> {
                        currentRoute.topBarTitle
                    }

                    is FunctionRoute -> {
                        // function routes are special because we need to fetch something from the viewmodel
                        val functionRoute = currentRoute

                        val allBrandsFlow = viewmodel.allBrandsFlow
                        val brand = allBrandsFlow.firstState { it.name == functionRoute.brandName }
                        val category = brand?.categories?.find { it.name == functionRoute.categoryName }
                        val model = category?.models?.find { it.identifier == functionRoute.modelIdentifier }
                        val function = model?.functions?.find { it.identifier == functionRoute.functionIdentifier }

                        function?.let { "Press/hold to send ${it.functionName}" } ?: "Loading..."
                    }

                    else -> {
                        "SupIR"
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            colors = topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            title = { Text(scaffoldTopbarTitle) }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            BottomNavigationItem.bottomNavigationItems.forEach { item ->
                                NavigationBarItem(
                                    selected = item.route::class.isInstance(currentRoute),
                                    label = {
                                        Text(item.label)
                                    },
                                    icon = {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    SupIRNavHost(
                        modifier = Modifier.padding(innerPadding),
                        supIRViewModel = viewmodel,
                        navController = navController,
                    )
                }
            }
        }
    }
}
