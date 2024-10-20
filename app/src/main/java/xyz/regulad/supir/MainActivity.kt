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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
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
                var scaffoldTopBarTitle by remember { mutableStateOf("SupIR") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            colors = topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            title = { Text(scaffoldTopBarTitle) }
                        )
                    }
                ) { innerPadding ->
                    SupIRNavHost(
                        modifier = Modifier.padding(innerPadding),
                        supIRViewModel = viewmodel,
                        navController = navController,
                        setTopBarTitle = { title -> scaffoldTopBarTitle = title }
                    )
                }
            }
        }
    }
}
