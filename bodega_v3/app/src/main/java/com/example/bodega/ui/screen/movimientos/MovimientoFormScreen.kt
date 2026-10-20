package com.example.bodega.ui.screen.movimientos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bodega.constant.TipoMovimiento
import com.example.bodega.data.local.LineaMovimiento
import com.example.bodega.ui.components.AppTextField
import com.example.bodega.ui.components.SelectorField
import com.example.bodega.util.Formato
import com.example.bodega.viewmodel.MovimientoFormViewModel

private data class LineaUi(
    val productoId: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Double
)

/** Registro de una entrada, salida o ajuste con varios productos. Se guarda en SQLite. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoFormScreen(
    viewModel: MovimientoFormViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val proveedores by viewModel.proveedores.collectAsStateWithLifecycle()

    var tipo by rememberSaveable { mutableStateOf(TipoMovimiento.ENTRADA) }
    var proveedorId by rememberSaveable { mutableStateOf<Int?>(null) }
    var observacion by rememberSaveable { mutableStateOf("") }
    var productoId by rememberSaveable { mutableStateOf<Int?>(null) }
    var cantidad by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    val lineas = remember { mutableStateListOf<LineaUi>() }

    LaunchedEffect(uiState.completado) {
        if (uiState.completado) {
            Toast.makeText(context, "Movimiento registrado", Toast.LENGTH_SHORT).show()
            viewModel.consumirCompletado()
            onFinished()
        }
    }

    val productoElegido = productos.firstOrNull { it.id == productoId }
    val textoProducto = productoElegido?.nombre ?: "Seleccionar"
    val textoProveedor = proveedores.firstOrNull { it.id == proveedorId }?.razonSocial ?: "Ninguno"

    val opcionesProveedor: List<Pair<Int?, String>> =
        listOf<Pair<Int?, String>>(Pair(null, "Ninguno")) +
            proveedores.map { Pair<Int?, String>(it.id, it.razonSocial) }

    val total = lineas.sumOf { it.cantidad * it.precio }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Nuevo movimiento") },
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
            SelectorField(
                etiqueta = "Tipo",
                textoSeleccionado = TipoMovimiento.etiqueta(tipo),
                opciones = TipoMovimiento.todos.map { Pair(it, TipoMovimiento.etiqueta(it)) },
                onSeleccion = { tipo = it; viewModel.clearError() }
            )

            if (tipo == TipoMovimiento.ENTRADA) {
                SelectorField(
                    etiqueta = "Proveedor",
                    textoSeleccionado = textoProveedor,
                    opciones = opcionesProveedor,
                    onSeleccion = { proveedorId = it }
                )
            }

            AppTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = "Observación (opcional)"
            )

            Text(
                text = "Agregar producto",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            SelectorField(
                etiqueta = "Producto",
                textoSeleccionado = textoProducto,
                opciones = productos.map { Pair(it.id, "${it.nombre} (stock ${it.stockActual})") },
                onSeleccion = { id ->
                    productoId = id
                    errorLocal = null
                    val elegido = productos.firstOrNull { it.id == id }
                    if (elegido != null) {
                        // Sugiere el precio segun el tipo de movimiento
                        precio = Formato.numero(
                            if (tipo == TipoMovimiento.SALIDA) elegido.precioVenta
                            else elegido.precioCompra
                        )
                    }
                }
            )

            if (productoElegido != null) {
                Text(
                    text = "Stock actual: ${productoElegido.stockActual}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it; errorLocal = null },
                    label = "Cantidad",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                AppTextField(
                    value = precio,
                    onValueChange = { precio = it; errorLocal = null },
                    label = "Precio unit.",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
            }

            OutlinedButton(
                onClick = {
                    val p = productos.firstOrNull { it.id == productoId }
                    val cant = cantidad.trim().toIntOrNull()
                    val pre = Formato.decimal(precio)
                    when {
                        p == null -> errorLocal = "Selecciona un producto"
                        cant == null || cant <= 0 -> errorLocal = "Ingresa una cantidad válida"
                        pre == null || pre < 0 -> errorLocal = "Ingresa un precio válido"
                        else -> {
                            val indice = lineas.indexOfFirst { it.productoId == p.id }
                            if (indice >= 0) {
                                val actual = lineas[indice]
                                lineas[indice] = actual.copy(
                                    cantidad = actual.cantidad + cant,
                                    precio = pre
                                )
                            } else {
                                lineas.add(LineaUi(p.id, p.nombre, cant, pre))
                            }
                            productoId = null
                            cantidad = ""
                            precio = ""
                            errorLocal = null
                            viewModel.clearError()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Text(text = " Agregar a la lista")
            }

            errorLocal?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (lineas.isNotEmpty()) {
                Text(
                    text = "Productos del movimiento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            lineas.forEach { linea ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = linea.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${linea.cantidad} x ${Formato.moneda(linea.precio)} = " +
                                    Formato.moneda(linea.cantidad * linea.precio),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { lineas.remove(linea) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Quitar"
                            )
                        }
                    }
                }
            }

            if (lineas.isNotEmpty()) {
                Text(
                    text = "Total: ${Formato.moneda(total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                        tipo = tipo,
                        proveedorId = proveedorId,
                        observacion = observacion,
                        lineas = lineas.map { LineaMovimiento(it.productoId, it.cantidad, it.precio) }
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
                    Text(text = "Guardar movimiento")
                }
            }
        }
    }
}
