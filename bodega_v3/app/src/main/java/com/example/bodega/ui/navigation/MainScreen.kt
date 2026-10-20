package com.example.bodega.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bodega.ui.screen.catalogos.CatalogosScreen
import com.example.bodega.ui.screen.movimientos.MovimientosScreen
import com.example.bodega.ui.screen.productos.ProductosScreen
import com.example.bodega.viewmodel.AppViewModelFactory
import com.example.bodega.viewmodel.CatalogosViewModel
import com.example.bodega.viewmodel.MovimientosViewModel
import com.example.bodega.viewmodel.ProductosViewModel

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** Pantalla con la barra inferior: Productos, Movimientos y Catalogos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    factory: AppViewModelFactory,
    nombreUsuario: String,
    rol: String,
    onNuevoProducto: () -> Unit,
    onEditarProducto: (Int) -> Unit,
    onNuevoMovimiento: () -> Unit,
    onLogout: () -> Unit
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val items = listOf(
        BottomItem(Routes.PRODUCTOS, "Productos", Icons.Default.Inventory2),
        BottomItem(Routes.MOVIMIENTOS, "Movimientos", Icons.Default.SwapHoriz),
        BottomItem(Routes.CATALOGOS, "Catálogos", Icons.Default.Category)
    )

    val titulo = when (currentRoute) {
        Routes.MOVIMIENTOS -> "Movimientos"
        Routes.CATALOGOS -> "Catálogos"
        else -> "Productos"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "$titulo · $nombreUsuario ($rol)", maxLines = 1)
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            when (currentRoute) {
                Routes.PRODUCTOS, null -> FloatingActionButton(onClick = onNuevoProducto) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo producto")
                }

                Routes.MOVIMIENTOS -> FloatingActionButton(onClick = onNuevoMovimiento) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo movimiento")
                }

                else -> Unit
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            innerNav.navigate(item.route) {
                                popUpTo(innerNav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = null) },
                        label = { Text(text = item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNav,
            startDestination = Routes.PRODUCTOS,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.PRODUCTOS) {
                val vm: ProductosViewModel = viewModel(factory = factory)
                ProductosScreen(viewModel = vm, onEditar = onEditarProducto)
            }
            composable(Routes.MOVIMIENTOS) {
                val vm: MovimientosViewModel = viewModel(factory = factory)
                MovimientosScreen(viewModel = vm)
            }
            composable(Routes.CATALOGOS) {
                val vm: CatalogosViewModel = viewModel(factory = factory)
                CatalogosScreen(viewModel = vm)
            }
        }
    }
}
