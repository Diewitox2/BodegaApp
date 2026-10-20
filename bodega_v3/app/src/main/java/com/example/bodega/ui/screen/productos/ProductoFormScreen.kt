package com.example.bodega.ui.screen.productos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bodega.ui.components.AppTextField
import com.example.bodega.ui.components.SelectorField
import com.example.bodega.util.Formato
import com.example.bodega.viewmodel.ProductoFormViewModel

/** Nuevo producto (productoId = 0) o edicion de uno existente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(
    productoId: Int,
    viewModel: ProductoFormViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val proveedores by viewModel.proveedores.collectAsStateWithLifecycle()

    val esEdicion = productoId != 0

    var codigo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var categoriaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var proveedorId by rememberSaveable { mutableStateOf<Int?>(null) }
    var precioCompra by rememberSaveable { mutableStateOf("") }
    var precioVenta by rememberSaveable { mutableStateOf("") }
    var stockMinimo by rememberSaveable { mutableStateOf("0") }
    var activo by rememberSaveable { mutableStateOf(true) }
    var cargado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(productoId) {
        if (esEdicion) viewModel.cargar(productoId)
    }

    // Rellena los campos una sola vez cuando llega el producto a editar
    LaunchedEffect(uiState.producto) {
        val p = uiState.producto
        if (p != null && !cargado) {
            codigo = p.codigo
            nombre = p.nombre
            categoriaId = p.categoriaId
            proveedorId = p.proveedorId
            precioCompra = Formato.numero(p.precioCompra)
            precioVenta = Formato.numero(p.precioVenta)
            stockMinimo = p.stockMinimo.toString()
            activo = p.activo
            cargado = true
        }
    }

    // Nombre sugerido por el servicio REST (Open Food Facts)
    LaunchedEffect(uiState.nombreSugerido) {
        uiState.nombreSugerido?.let {
            nombre = it
            viewModel.consumirSugerencia()
        }
    }

    LaunchedEffect(uiState.aviso) {
        uiState.aviso?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumirAviso()
        }
    }

    LaunchedEffect(uiState.completado) {
        if (uiState.completado) {
            Toast.makeText(context, "Producto guardado", Toast.LENGTH_SHORT).show()
            viewModel.consumirCompletado()
            onFinished()
        }
    }

    val textoCategoria = categorias.firstOrNull { it.id == categoriaId }?.nombre
        ?: if (categoriaId != null) "(inactiva)" else "Seleccionar"
    val textoProveedor = proveedores.firstOrNull { it.id == proveedorId }?.razonSocial
        ?: if (proveedorId != null) "(inactivo)" else "Ninguno"

    val opcionesProveedor: List<Pair<Int?, String>> =
        listOf<Pair<Int?, String>>(Pair(null, "Ninguno")) +
            proveedores.map { Pair<Int?, String>(it.id, it.razonSocial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (esEdicion) "Editar producto" else "Nuevo producto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTextField(
                value = codigo,
                onValueChange = { codigo = it; viewModel.clearError() },
                label = "Código (barras o interno)"
            )

            OutlinedButton(
                onClick = { viewModel.buscarNombre(codigo) },
                enabled = codigo.isNotBlank() && !uiState.buscandoNombre,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.buscandoNombre) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Buscar nombre por código (internet)")
                }
            }
            Text(
                text = "Consulta pública: Open Food Facts (datos ODbL). Si no aparece, escribe el nombre a mano.",
                style = MaterialTheme.typography.bodySmall
            )

            AppTextField(
                value = nombre,
                onValueChange = { nombre = it; viewModel.clearError() },
                label = "Nombre"
            )

            SelectorField(
                etiqueta = "Categoría",
                textoSeleccionado = textoCategoria,
                opciones = categorias.map { Pair(it.id, it.nombre) },
                onSeleccion = { categoriaId = it; viewModel.clearError() }
            )

            SelectorField(
                etiqueta = "Proveedor",
                textoSeleccionado = textoProveedor,
                opciones = opcionesProveedor,
                onSeleccion = { proveedorId = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = precioCompra,
                    onValueChange = { precioCompra = it; viewModel.clearError() },
                    label = "P. compra",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
                AppTextField(
                    value = precioVenta,
                    onValueChange = { precioVenta = it; viewModel.clearError() },
                    label = "P. venta",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
            }

            AppTextField(
                value = stockMinimo,
                onValueChange = { stockMinimo = it; viewModel.clearError() },
                label = "Stock mínimo (alerta)",
                keyboardType = KeyboardType.Number
            )

            if (esEdicion) {
                val stock = uiState.producto?.stockActual
                Text(
                    text = "Stock actual: ${stock ?: "-"} (solo cambia con movimientos)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activo, onCheckedChange = { activo = it })
                    Text(
                        text = if (activo) "  Producto activo" else "  Producto inactivo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.guardar(
                        id = productoId,
                        codigo = codigo,
                        nombre = nombre,
                        categoriaId = categoriaId,
                        proveedorId = proveedorId,
                        precioCompra = precioCompra,
                        precioVenta = precioVenta,
                        stockMinimo = stockMinimo,
                        activo = activo
                    )
                },
                enabled = !uiState.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Guardar")
                }
            }
        }
    }
}
