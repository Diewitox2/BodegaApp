package com.example.bodega.ui.screen.catalogos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.bodega.data.local.CategoriaEntity
import com.example.bodega.data.local.ProveedorEntity
import com.example.bodega.ui.components.AppTextField
import com.example.bodega.ui.components.Cargando
import com.example.bodega.ui.components.ConfirmDialog
import com.example.bodega.ui.components.EstadoVacio
import com.example.bodega.viewmodel.CatalogosViewModel

/** Mantenimiento (crear, editar, eliminar) de categorias y proveedores. */
@Composable
fun CatalogosScreen(viewModel: CatalogosViewModel) {
    val context = LocalContext.current
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val proveedores by viewModel.proveedores.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()

    var pestana by rememberSaveable { mutableStateOf(0) }

    // null = dialogo cerrado. Un objeto con id = 0 significa "nuevo".
    var editandoCategoria by remember { mutableStateOf<CategoriaEntity?>(null) }
    var eliminandoCategoria by remember { mutableStateOf<CategoriaEntity?>(null) }
    var editandoProveedor by remember { mutableStateOf<ProveedorEntity?>(null) }
    var eliminandoProveedor by remember { mutableStateOf<ProveedorEntity?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumirMensaje()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pestana == 0) {
                Button(onClick = { pestana = 0 }, modifier = Modifier.weight(1f)) {
                    Text(text = "Categorías")
                }
                OutlinedButton(onClick = { pestana = 1 }, modifier = Modifier.weight(1f)) {
                    Text(text = "Proveedores")
                }
            } else {
                OutlinedButton(onClick = { pestana = 0 }, modifier = Modifier.weight(1f)) {
                    Text(text = "Categorías")
                }
                Button(onClick = { pestana = 1 }, modifier = Modifier.weight(1f)) {
                    Text(text = "Proveedores")
                }
            }
        }

        OutlinedButton(
            onClick = {
                if (pestana == 0) editandoCategoria = CategoriaEntity(nombre = "")
                else editandoProveedor = ProveedorEntity(razonSocial = "", ruc = "")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Text(text = if (pestana == 0) " Nueva categoría" else " Nuevo proveedor")
        }

        if (pestana == 0) {
            val lista = categorias
            when {
                lista == null -> Cargando()
                lista.isEmpty() -> EstadoVacio("No hay categorías.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = lista, key = { it.id }) { categoria ->
                        FilaCatalogo(
                            titulo = categoria.nombre,
                            detalle = if (categoria.activo) "Activa" else "Inactiva",
                            onEditar = { editandoCategoria = categoria },
                            onEliminar = { eliminandoCategoria = categoria }
                        )
                    }
                }
            }
        } else {
            val lista = proveedores
            when {
                lista == null -> Cargando()
                lista.isEmpty() -> EstadoVacio("No hay proveedores.\nPresiona \"Nuevo proveedor\".")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = lista, key = { it.id }) { proveedor ->
                        FilaCatalogo(
                            titulo = proveedor.razonSocial,
                            detalle = "RUC ${proveedor.ruc}" +
                                (if (proveedor.telefono.isNotBlank()) " · ${proveedor.telefono}" else "") +
                                (if (proveedor.activo) "" else " · Inactivo"),
                            onEditar = { editandoProveedor = proveedor },
                            onEliminar = { eliminandoProveedor = proveedor }
                        )
                    }
                }
            }
        }
    }

    editandoCategoria?.let { categoria ->
        CategoriaDialog(
            categoria = categoria,
            onDismiss = { editandoCategoria = null },
            onGuardar = { nombre, activo, onResultado ->
                viewModel.guardarCategoria(categoria.id, nombre, activo, onResultado)
            }
        )
    }

    eliminandoCategoria?.let { categoria ->
        ConfirmDialog(
            titulo = "Eliminar categoría",
            mensaje = "¿Eliminar \"${categoria.nombre}\"?",
            onConfirm = {
                viewModel.eliminarCategoria(categoria)
                eliminandoCategoria = null
            },
            onDismiss = { eliminandoCategoria = null }
        )
    }

    editandoProveedor?.let { proveedor ->
        ProveedorDialog(
            proveedor = proveedor,
            onDismiss = { editandoProveedor = null },
            onGuardar = { razon, ruc, telefono, activo, onResultado ->
                viewModel.guardarProveedor(proveedor.id, razon, ruc, telefono, activo, onResultado)
            }
        )
    }

    eliminandoProveedor?.let { proveedor ->
        ConfirmDialog(
            titulo = "Eliminar proveedor",
            mensaje = "¿Eliminar \"${proveedor.razonSocial}\"?",
            onConfirm = {
                viewModel.eliminarProveedor(proveedor)
                eliminandoProveedor = null
            },
            onDismiss = { eliminandoProveedor = null }
        )
    }
}

@Composable
private fun FilaCatalogo(
    titulo: String,
    detalle: String,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
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
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = detalle, style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun CategoriaDialog(
    categoria: CategoriaEntity,
    onDismiss: () -> Unit,
    onGuardar: (String, Boolean, (String?) -> Unit) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria.nombre) }
    var activo by remember { mutableStateOf(categoria.activo) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (categoria.id == 0) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    label = "Nombre"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activo, onCheckedChange = { activo = it })
                    Text(text = if (activo) "  Activa" else "  Inactiva")
                }
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancelar") }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(nombre, activo) { resultado ->
                        if (resultado == null) onDismiss() else error = resultado
                    }
                }
            ) { Text(text = "Guardar") }
        }
    )
}

@Composable
private fun ProveedorDialog(
    proveedor: ProveedorEntity,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String, Boolean, (String?) -> Unit) -> Unit
) {
    var razon by remember { mutableStateOf(proveedor.razonSocial) }
    var ruc by remember { mutableStateOf(proveedor.ruc) }
    var telefono by remember { mutableStateOf(proveedor.telefono) }
    var activo by remember { mutableStateOf(proveedor.activo) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (proveedor.id == 0) "Nuevo proveedor" else "Editar proveedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = razon,
                    onValueChange = { razon = it; error = null },
                    label = "Razón social"
                )
                AppTextField(
                    value = ruc,
                    onValueChange = { ruc = it; error = null },
                    label = "RUC (11 dígitos)",
                    keyboardType = KeyboardType.Number
                )
                AppTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = "Teléfono (opcional)",
                    keyboardType = KeyboardType.Phone
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activo, onCheckedChange = { activo = it })
                    Text(text = if (activo) "  Activo" else "  Inactivo")
                }
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancelar") }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(razon, ruc, telefono, activo) { resultado ->
                        if (resultado == null) onDismiss() else error = resultado
                    }
                }
            ) { Text(text = "Guardar") }
        }
    )
}
