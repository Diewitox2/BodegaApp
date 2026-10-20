package com.example.bodega.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bodega.session.SessionManager
import com.example.bodega.ui.screen.auth.LoginScreen
import com.example.bodega.ui.screen.auth.RegisterScreen
import com.example.bodega.ui.screen.movimientos.MovimientoFormScreen
import com.example.bodega.ui.screen.productos.ProductoFormScreen
import com.example.bodega.viewmodel.AppViewModelFactory
import com.example.bodega.viewmodel.AuthViewModel
import com.example.bodega.viewmodel.MovimientoFormViewModel
import com.example.bodega.viewmodel.ProductoFormViewModel

@Composable
fun AppNavigation(
    factory: AppViewModelFactory,
    session: SessionManager
) {
    val navController = rememberNavController()
    val inicio = if (session.haySesion) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = inicio
    ) {
        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val vm: AuthViewModel = viewModel(factory = factory)
            RegisterScreen(
                viewModel = vm,
                onRegistered = { navController.popBackStack() },
                onGoToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                factory = factory,
                nombreUsuario = session.nombre,
                rol = session.rol,
                onNuevoProducto = { navController.navigate(Routes.productoForm(0)) },
                onEditarProducto = { id -> navController.navigate(Routes.productoForm(id)) },
                onNuevoMovimiento = { navController.navigate(Routes.MOVIMIENTO_FORM) },
                onLogout = {
                    session.cerrar()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PRODUCTO_FORM,
            arguments = listOf(navArgument(Routes.ARG_PRODUCTO_ID) { type = NavType.IntType })
        ) { entry ->
            val productoId = entry.arguments?.getInt(Routes.ARG_PRODUCTO_ID) ?: 0
            val vm: ProductoFormViewModel = viewModel(factory = factory)
            ProductoFormScreen(
                productoId = productoId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }

        composable(Routes.MOVIMIENTO_FORM) {
            val vm: MovimientoFormViewModel = viewModel(factory = factory)
            MovimientoFormScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }
    }
}
