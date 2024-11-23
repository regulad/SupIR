package xyz.regulad.supir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import xyz.regulad.regulib.compose.firstState
import xyz.regulad.regulib.compose.toRoute
import xyz.regulad.supir.ui.nav.FunctionRoute
import xyz.regulad.supir.ui.nav.RouteWithTopBar
import xyz.regulad.supir.ui.nav.SupIRNavHost
import xyz.regulad.supir.ui.theme.SupIRTheme

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

                val scaffoldTopbarTitle = if (currentRoute is RouteWithTopBar) {
                    currentRoute.topBarTitle
                } else if (currentRoute is FunctionRoute) {
                    // function routes are special because we need to fetch something from the viewmodel
                    val functionRoute = currentRoute

                    val allBrandsFlow = viewmodel.allBrands
                    val brand = allBrandsFlow.firstState { it.name == functionRoute.brandName }
                    val category = brand?.categories?.find { it.name == functionRoute.categoryName }
                    val model = category?.models?.find { it.identifier == functionRoute.modelIdentifier }
                    val function = model?.functions?.find { it.identifier == functionRoute.functionIdentifier }

                    function?.let { "Press/hold to send ${it.functionName}" } ?: "Loading..."
                } else {
                    "SupIR"
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
