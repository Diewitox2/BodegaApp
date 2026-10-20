package com.example.bodega.ui.screen.productos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bodega.data.local.ProductoEntity
import com.example.bodega.data.local.ProductoVista
import com.example.bodega.ui.components.Cargando
import com.example.bodega.ui.components.ConfirmDialog
import com.example.bodega.ui.components.EstadoVacio
import com.example.bodega.util.Formato
import com.example.bodega.viewmodel.ProductosViewModel

/** Lista personalizada de productos con busqueda, edicion y eliminacion. */
@Composable
fun ProductosScreen(
    viewModel: ProductosViewModel,
    onEditar: (Int) -> Unit
) {
    val context = LocalContext.current
    val texto by viewModel.texto.collectAsStateWithLifecycle()
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    var porEliminar by remember { mutableStateOf<ProductoEntity?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumirMensaje()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = texto,
            onValueChange = viewModel::onTextoChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
            label = { Text(text = "Buscar por nombre o código") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        val lista = productos
        when {
            lista == null -> Cargando()

            lista.isEmpty() -> EstadoVacio(
                if (texto.isBlank()) "No hay productos.\nPresiona + para registrar el primero."
                else "No se encontraron productos con ese texto."
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = lista, key = { it.producto.id }) { vista ->
                    ProductoCard(
                        vista = vista,
                        onEditar = { onEditar(vista.producto.id) },
                        onEliminar = { porEliminar = vista.producto }
                    )
                }
            }
        }
    }

    porEliminar?.let { producto ->
        ConfirmDialog(
            titulo = "Eliminar producto",
            mensaje = "¿Eliminar \"${producto.nombre}\"?\n\nEsta acción no se puede deshacer.",
            onConfirm = {
                viewModel.eliminar(producto)
                porEliminar = null
            },
            onDismiss = { porEliminar = null }
        )
    }
}

@Composable
private fun ProductoCard(
    vista: ProductoVista,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val p = vista.producto
    val stockBajo = p.activo && p.stockActual <= p.stockMinimo

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (stockBajo) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = p.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Código: ${p.codigo} · ${vista.categoriaNombre}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Compra ${Formato.moneda(p.precioCompra)} · Venta ${Formato.moneda(p.precioVenta)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Stock: ${p.stockActual} (mín. ${p.stockMinimo})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (stockBajo) {
                    Text(
                        text = "STOCK BAJO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!p.activo) {
                    Text(
                        text = "Inactivo",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            IconButton(onClick = onEditar) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onEliminar) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
