package xyz.regulad.supir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import xyz.regulad.supir.ir.loadAllBrands
import xyz.regulad.supir.ui.nav.SupIRNavHost
import xyz.regulad.supir.ui.theme.SupIRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewmodel = SupIRViewModel(application)
        val allBrands  = loadAllBrands(assets)

        setContent {
            SupIRTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SupIRNavHost(
                        modifier = Modifier.padding(innerPadding),
                        supIRViewModel = viewmodel,
                        allBrandsFlow = allBrands
                    )
                }
            }
        }
    }
}
