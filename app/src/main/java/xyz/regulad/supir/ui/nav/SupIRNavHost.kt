package xyz.regulad.supir.ui.nav

import android.content.Context
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import xyz.regulad.regulib.compose.firstState
import xyz.regulad.regulib.compose.produceState
import xyz.regulad.regulib.showToast
import xyz.regulad.supir.SupIRViewModel
import xyz.regulad.supir.ir.*
import xyz.regulad.supir.ir.IrEncoder.transmissionLengthDuration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun FullscreenLoader() {
    // centrally placed spinny
    Box (
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(64.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Serializable
data object MainRoute

@Serializable
data object UnsupportedRoute

@Serializable
data class BrandRoute(val brandName: String)

@Serializable
data class CategoryRoute(val brandName: String, val categoryName: String)

@Serializable
data class ModelRoute(val brandName: String, val categoryName: String, val modelIdentifier: String)

@Serializable
data class FunctionRoute(val brandName: String, val categoryName: String, val modelIdentifier: String, val functionIdentifier: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupIRNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    supIRViewModel: SupIRViewModel,
    setTopBarTitle: (String) -> Unit
) {
    val allBrandsFlow by produceState<Flow<SBrand>?>(null) {
        value = supIRViewModel.allBrands // this is a lazy value that will be loaded when first accessed
    }

    NavHost(navController = navController, startDestination = if (supIRViewModel.transmitter != null) MainRoute else UnsupportedRoute, modifier = modifier) {
        composable<UnsupportedRoute> {
            Text("Your device does not support IR transmission. Sorry.")
        }
        composable<MainRoute> {
            fun navigateToBrand(brand: SBrand) {
                if (brand.categories.size > 1) {
                    navController.navigate(
                        route = BrandRoute(
                            brandName = brand.name,
                        )
                    )
                } else if (brand.categories.size == 1 && brand.categories.first().models.size > 1) {
                    navController.navigate(
                        route = CategoryRoute(
                            brandName = brand.name,
                            categoryName = brand.categories.first().name
                        )
                    )
                } else if (brand.categories.size == 1 && brand.categories.first().models.size == 1) {
                    navController.navigate(
                        route = ModelRoute(
                            brandName = brand.name,
                            categoryName = brand.categories.first().name,
                            modelIdentifier = brand.categories.first().models.first().identifier
                        )
                    )
                }
            }

            LaunchedEffect(Unit) {
                setTopBarTitle("Select brand")
            }

            if (allBrandsFlow == null) {
                FullscreenLoader()
            } else {
                val (items, flowFinished) = allBrandsFlow!!.produceState()
                val lazyColumnState = rememberLazyListState()

                if (items.isEmpty()) {
                    FullscreenLoader()
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    var query by remember { mutableStateOf("") }
                    val filteredItems = items.filter { it.name.contains(query, ignoreCase = true) }

                    val context = LocalContext.current

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        SearchBar(
                            modifier = Modifier.fillMaxWidth(),
                            inputField = {
                                SearchBarDefaults.InputField(
                                    expanded = expanded,
                                    placeholder = { Text("Search brands") },
                                    onExpandedChange = {
                                        expanded = it
                                        if (!it) {
                                            query = ""
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    query = query,
                                    onQueryChange = { query = it },
                                    onSearch = {
                                        expanded = false

                                        if (filteredItems.size == 1) {
                                            navigateToBrand(filteredItems.first())
                                        } else if (filteredItems.isEmpty()) {
                                            context.showToast("No brand found matching \"$query\"")
                                        } else {
                                            context.showToast("Multiple brands found matching \"$query\", select one below.")
                                        }
                                    }
                                )
                            },
                            expanded = expanded,
                            onExpandedChange = {
                                expanded = it
                                if (!it) {
                                    query = ""
                                }
                            },
                        ) {
                            val previewLazyColumnState = rememberLazyListState()

                            LazyColumn(
                                state = previewLazyColumnState
                            ) {
                                items(filteredItems) { brand ->
                                    Surface(onClick = {
                                        navigateToBrand(brand)
                                    }) {
                                        Column {
                                            ListItem(
                                                headlineContent = { Text(brand.name) }
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }

                                if (!flowFinished) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .height(64.dp)
                                                    .align(Alignment.CenterHorizontally),
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                } else {
                                    if (filteredItems.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text("No brands found matching \"$query\"")
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(state = lazyColumnState) {
                            items(items) { brand ->
                                Surface(onClick = {
                                    navigateToBrand(brand)
                                }) {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text(brand.name) }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }

                            if (!flowFinished) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .width(64.dp)
                                                .height(64.dp)
                                                .align(Alignment.CenterHorizontally),
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        composable<BrandRoute> { backStackEntry ->
            val brandRoute: BrandRoute = backStackEntry.toRoute()

            val brand = allBrandsFlow?.firstState { it.name == brandRoute.brandName }

            fun navigateToCategory(category: SCategory) {
                if (category.models.size > 1) {
                    navController.navigate(
                        route = CategoryRoute(
                            brandName = brand!!.name,
                            categoryName = category.name
                        )
                    )
                } else {
                    navController.navigate(
                        route = ModelRoute(
                            brandName = brand!!.name,
                            categoryName = category.name,
                            modelIdentifier = category.models.first().identifier
                        )
                    )
                }
            }

            LaunchedEffect(brand) {
                setTopBarTitle("Select category of ${brand?.name ?: "brand"} device")
            }

            if (brand == null) {
                FullscreenLoader()
            } else {
                val lazyColumnState = rememberLazyListState()
                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(brand.categories) { category ->
                        Surface(onClick = {
                            navigateToCategory(category)
                        }) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(category.name) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        composable<CategoryRoute> { backStackEntry ->
            val categoryRoute: CategoryRoute = backStackEntry.toRoute()

            val brand = allBrandsFlow?.firstState { it.name == categoryRoute.brandName }
            val category = brand?.categories?.find { it.name == categoryRoute.categoryName }

            fun navigateToModel(model: SModel) {
                navController.navigate(route = ModelRoute(
                    brandName = brand!!.name,
                    categoryName = category!!.name,
                    modelIdentifier = model.identifier
                ))
            }

            LaunchedEffect(brand, category) {
                setTopBarTitle("Select model of ${category?.name ?: "device"}.")
            }

            if (category == null) {
                FullscreenLoader()
            } else {
                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(category.models) { model ->
                        Surface (onClick = {
                            navigateToModel(model)
                        }) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(model.identifier) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        composable<ModelRoute> { backStackEntry ->
            val modelRoute: ModelRoute = backStackEntry.toRoute()

            val brand = allBrandsFlow?.firstState { it.name == modelRoute.brandName }
            val category = brand?.categories?.find { it.name == modelRoute.categoryName }
            val model = category?.models?.find { it.identifier == modelRoute.modelIdentifier }

            fun navigateToFunction(function: IRDBFunction) {
                navController.navigate(
                    FunctionRoute(
                        brand!!.name,
                        category!!.name,
                        model!!.identifier,
                        function.identifier
                    )
                )
            }

            LaunchedEffect(brand, category, model) {
                setTopBarTitle("Select command")
            }

            if (model == null) {
                FullscreenLoader()
            } else {
                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(model.functions.toList()) { function ->
                        Surface(
                            onClick = {
                                navigateToFunction(function)
                            }
                        ) {
                            Column {
                                function.asListItem()
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        composable<FunctionRoute> { backStackEntry ->
            val functionRoute: FunctionRoute = backStackEntry.toRoute()

            val brand = allBrandsFlow?.firstState { it.name == functionRoute.brandName }
            val category = brand?.categories?.find { it.name == functionRoute.categoryName }
            val model = category?.models?.find { it.identifier == functionRoute.modelIdentifier }
            val function = model?.functions?.find { it.identifier == functionRoute.functionIdentifier }

            if (function == null) {
                FullscreenLoader()
                return@composable
            }

            LaunchedEffect(function, brand, category) {
                setTopBarTitle("Press/hold to send ${function.functionName}")
            }

            val context = LocalContext.current
            val transmissionScope = rememberCoroutineScope { Dispatchers.Main }
            val vibrator = LocalContext.current.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .wrapContentSize(Alignment.Center)
                        .pointerInput(Unit) {
                            coroutineScope {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            if (vibrator.hasVibrator()) {
                                                vibrator.vibrate(50)
                                            }
                                        } catch (e: Exception) {
                                            // ignore
                                        }

                                        try {
                                            function.transmit(context, supIRViewModel.transmitter!!)
                                            context.showToast("Sent ${function.functionName} successfully.")
                                        } catch (e: Exception) {
                                            context.showToast("Failed to send ${function.functionName}: ${e.message}")
                                            return@detectTapGestures
                                        }

                                        val retransmissionJob = transmissionScope.launch {
                                            var transmissionLength = function.transmissionLengthDuration(context)

                                            val isNecFamilyFunction = function.protocol.uppercase().startsWith("NEC")
                                            if (isNecFamilyFunction) {
                                                // this is an NEC family function, they have a special repeat function
                                                while (isActive) {
                                                    val transmissionDelay =
                                                        (108).toDuration(DurationUnit.MILLISECONDS) - transmissionLength!! // guaranteed with NEC
                                                    delay(transmissionDelay)

                                                    // transmit the repeat pattern
                                                    supIRViewModel.transmitter.transmitMicrosecondIntArray(
                                                        38000,
                                                        necRepeatPattern
                                                    )
                                                    transmissionLength =
                                                        necRepeatPattern.sum().toDuration(DurationUnit.MICROSECONDS)
                                                }
                                            }
                                        }

                                        tryAwaitRelease()

                                        retransmissionJob.cancel()
                                    }
                                )
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .background(MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(
                            imageVector = function.icon,
                            modifier = Modifier.align(Alignment.Center).size(120.dp),
                            contentDescription = function.functionName.ifEmpty { "Command" },
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}
