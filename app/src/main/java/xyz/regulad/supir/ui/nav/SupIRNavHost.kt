package xyz.regulad.supir.ui.nav

import android.content.Context
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Dvr
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import xyz.regulad.regulib.compose.firstState
import xyz.regulad.regulib.compose.produceState
import xyz.regulad.regulib.showToast
import xyz.regulad.supir.SupIRViewModel
import xyz.regulad.supir.ir.*
import xyz.regulad.supir.ir.IrEncoder.getCanRepeat

@Composable
fun FullscreenLoader() {
    // centrally placed spinny
    Box(
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

interface RouteWithTopBar {
    val topBarTitle: String
}

@Serializable
data object MainRoute : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Select brand"
}

@Serializable
data object FavoritesRoute : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Favorites"
}

@Serializable
data object UnsupportedRoute : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Unsupported device"
}

@Serializable
data class BrandRoute(val brandName: String) : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Select type of $brandName device"
}

@Serializable
data class CategoryRoute(val brandName: String, val categoryName: String) : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Select model of $categoryName"
}

@Serializable
data class ModelRoute(val brandName: String, val categoryName: String, val modelIdentifier: String) : RouteWithTopBar {
    override val topBarTitle: String
        get() = "Select command"
}

@Serializable
data class FunctionRoute(
    val brandName: String,
    val categoryName: String,
    val modelIdentifier: String,
    val functionIdentifier: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupIRNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    supIRViewModel: SupIRViewModel
) {
    val favoriteBrandModels by supIRViewModel.favoriteBrandModels.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (supIRViewModel.transmitter != null) MainRoute else UnsupportedRoute,
        modifier = modifier
    ) {
        composable<UnsupportedRoute> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Sorry, but your device does not support IR transmission.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This app requires an IR blaster to work.")
                }
            }
        }
        composable<MainRoute> {
            fun navigateToBrand(brand: SBrand) {
                if (brand.categories.size > 1) {
                    navController.navigate(
                        route = BrandRoute(
                            brandName = brand.name,
                        )
                    )
                } else {
                    navController.navigate(
                        route = CategoryRoute(
                            brandName = brand.name,
                            categoryName = brand.categories.first().name // guaranteed to have at least one
                        )
                    )
                }
            }

            val (items, flowFinished) = supIRViewModel.allBrandsFlow.produceState()
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
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
        composable<BrandRoute> { backStackEntry ->
            val brandRoute: BrandRoute = backStackEntry.toRoute()

            val brand = supIRViewModel.allBrandsFlow.firstState { it.name == brandRoute.brandName }

            fun navigateToCategory(category: SCategory) {
                navController.navigate(
                    route = CategoryRoute(
                        brandName = brand!!.name,
                        categoryName = category.name
                    )
                )
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
        composable<FavoritesRoute> {
            if (favoriteBrandModels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No favorite devices yet. Go add some and return!",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val favoriteBrandModelsTriples = favoriteBrandModels.map { brandModelCoordinate ->
                    val list = brandModelCoordinate.split("//")
                    Triple(list[0], list[1], list[2])
                }

                val lazyColumnState = rememberLazyListState()
                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(favoriteBrandModelsTriples) { brandModelCoordinate ->
                        val (brand, category, modelIdentifier) = brandModelCoordinate

                        Surface(onClick = {
                            navController.navigate(
                                route = ModelRoute(
                                    brandName = brand,
                                    categoryName = category,
                                    modelIdentifier = modelIdentifier
                                )
                            )
                        }) {
                            Column {
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Dvr,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    },
                                    headlineContent = { Text("$brand $category") },
                                    supportingContent = { Text(modelIdentifier) }
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

            val brand = supIRViewModel.allBrandsFlow.firstState { it.name == categoryRoute.brandName }
            val category = brand?.categories?.find { it.name == categoryRoute.categoryName }

            fun navigateToModel(model: SModel) {
                navController.navigate(
                    route = ModelRoute(
                        brandName = brand!!.name,
                        categoryName = category!!.name,
                        modelIdentifier = model.identifier
                    )
                )
            }

            if (category == null) {
                FullscreenLoader()
            } else {
                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(category.models) { model ->
                        val brandModelCoordinate = "${brand.name}//${category.name}//${model.identifier}"
                        val isFavorite = favoriteBrandModels.contains(brandModelCoordinate)

                        Surface(onClick = {
                            navigateToModel(model)
                        }) {
                            Column {
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Dvr,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    },
                                    headlineContent = { Text("${brand.name} ${category.name}") },
                                    supportingContent = { Text(model.identifier) },
                                    trailingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            onClick = {
                                                supIRViewModel.setBrandModelFavorite(
                                                    brandModelCoordinate,
                                                    !isFavorite
                                                )
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            if (isFavorite) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Favorite",
                                                    tint = Color.Yellow,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Star,
                                                    contentDescription = "Not favorite",
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }
                                        }
                                    }
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

            val brand = supIRViewModel.allBrandsFlow.firstState { it.name == modelRoute.brandName }
            val category = brand?.categories?.find { it.name == modelRoute.categoryName }
            val model = category?.models?.find { it.identifier == modelRoute.modelIdentifier }

            fun navigateToFunction(function: IRFunction) {
                navController.navigate(
                    FunctionRoute(
                        brand!!.name,
                        category!!.name,
                        model!!.identifier,
                        function.identifier
                    )
                )
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

            val brand = supIRViewModel.allBrandsFlow.firstState { it.name == functionRoute.brandName }
            val category = brand?.categories?.find { it.name == functionRoute.categoryName }
            val model = category?.models?.find { it.identifier == functionRoute.modelIdentifier }
            val function = model?.functions?.find { it.identifier == functionRoute.functionIdentifier }

            if (function == null) {
                FullscreenLoader()
                return@composable
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
                                            function.transmitInitialPattern(context, supIRViewModel.transmitter!!)
                                            context.showToast("Sent ${function.functionName} successfully.")
                                        } catch (e: Exception) {
                                            context.showToast("Failed to send ${function.functionName}: ${e.message}")
                                            return@detectTapGestures
                                        }

                                        if (function.getCanRepeat(context)) {
                                            val retransmissionJob = transmissionScope.launch {
                                                delay(40)
                                                while (isActive) {
                                                    try {
                                                        function.transmitRepeatPattern(
                                                            context,
                                                            supIRViewModel.transmitter
                                                        )
                                                        delay(108)
                                                    } catch (e: Exception) {
                                                        context.showToast("Failed to send ${function.functionName}: ${e.message}")
                                                        return@launch
                                                    }
                                                }
                                            }

                                            tryAwaitRelease()
                                            retransmissionJob.cancelAndJoin()
                                        }
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
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(120.dp),
                            contentDescription = function.functionName.ifEmpty { "Command" },
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}
