package xyz.regulad.supir.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import xyz.regulad.supir.SupIRViewModel
import xyz.regulad.supir.ir.Brand
import xyz.regulad.supir.ir.Category
import xyz.regulad.supir.ir.Model
import xyz.regulad.supir.ui.components.FlowLazyColumn
import xyz.regulad.supir.util.FlowCache.cachedCollectUntil

@Serializable
data object MainRoute

@Serializable
data object UnsupportedRoute

// i give up on trying to intermingle the serializable classes with the parcelable classes. it just doesn't work

@Serializable
data class BrandRoute(val brandName: String)

@Serializable
data class CategoryRoute(val brandName: String, val categoryName: String)

@Serializable
data class ModelRoute(val brandName: String, val categoryName: String, val modelIdentifier: String)

@Composable
fun SupIRNavHost(
    modifier: Modifier = Modifier,
    supIRViewModel: SupIRViewModel,
    allBrandsFlow: Flow<Brand>
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = if (supIRViewModel.transmitter != null) MainRoute else UnsupportedRoute, modifier = modifier) {
        composable<UnsupportedRoute> {
            Text("Your device does not support IR transmission. Sorry.")
        }
        composable<MainRoute> {
            fun navigateToBrand(brand: Brand) {
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

            Column (modifier = Modifier.fillMaxSize()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select the brand of the device you want to control")
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }

                FlowLazyColumn(
                    flow = allBrandsFlow,
                    modifier = Modifier.fillMaxSize()
                ) { brand ->
                    Surface (onClick = {
                        navigateToBrand(brand)
                    }) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(brand.name)

                            Spacer(modifier = Modifier.height(8.dp))

                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        composable<BrandRoute> { backStackEntry ->
            val brandRoute: BrandRoute = backStackEntry.toRoute()

            val brand by produceState<Brand?>(null) {
                value = allBrandsFlow.cachedCollectUntil { it.name == brandRoute.brandName }!!
            }

            fun navigateToCategory(category: Category) {
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

            if (brand == null) {
                Text("Loading ${brandRoute.brandName}...")
                return@composable
            }

            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text("From ${brand!!.name}, select the category of the device you want to control")
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(brand!!.categories) { category ->
                        Surface (onClick = {
                            navigateToCategory(category)
                        }) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(category.name)

                                Spacer(modifier = Modifier.height(8.dp))

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        composable<CategoryRoute> { backStackEntry ->
            val categoryRoute: CategoryRoute = backStackEntry.toRoute()

            val brand by produceState<Brand?>(null) {
                value = allBrandsFlow.cachedCollectUntil { it.name == categoryRoute.brandName }!!
            }
            val category = brand?.categories?.find { it.name == categoryRoute.categoryName }

            fun navigateToModel(model: Model) {
                navController.navigate(route = ModelRoute(
                    brandName = brand!!.name,
                    categoryName = category!!.name,
                    modelIdentifier = model.identifier
                ))
            }

            if (category == null) {
                Text("Loading ${categoryRoute.categoryName}s...")
                return@composable
            }

            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select the model of the ${category.name} from ${brand!!.name} you want to control.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("You may need to find the right model ID through trial-and-error.")
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(category.models) { model ->
                        Surface (onClick = {
                            navigateToModel(model)
                        }) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(model.identifier)

                                Spacer(modifier = Modifier.height(8.dp))

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        composable<ModelRoute> { backStackEntry ->
            val modelRoute: ModelRoute = backStackEntry.toRoute()

            val brand by produceState<Brand?>(null) {
                value = allBrandsFlow.cachedCollectUntil { it.name == modelRoute.brandName }!!
            }
            val category = brand?.categories?.find { it.name == modelRoute.categoryName }
            val model = category?.models?.find { it.identifier == modelRoute.modelIdentifier }

            val context = LocalContext.current

            if (model == null) {
                Text("Loading ${modelRoute.modelIdentifier}...")
                return@composable
            }

            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select the command you want to send to your ${brand!!.name} ${category.name}.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Model selected: ${model.identifier}")
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                val lazyColumnState = rememberLazyListState()

                LazyColumn(
                    state = lazyColumnState
                ) {
                    items(model.functions.toList()) { function ->
                        Surface (onClick = {
//                            context.showToast("Sending ${function.functionName}...")
                            try {
                                function.transmit(supIRViewModel.transmitter!!)
                            } catch (e: Exception) {
                                context.showToast("Failed to send ${function.functionName}: ${e.message}")
                                return@Surface
                            }
//                            context.showToast("Sent ${function.functionName} successfully.")
                        }) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(function.functionName)

                                Spacer(modifier = Modifier.height(8.dp))

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
