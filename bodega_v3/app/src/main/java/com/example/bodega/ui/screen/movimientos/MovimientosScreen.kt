package com.example.bodega.ui.screen.movimientos

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bodega.constant.SyncEstado
import com.example.bodega.constant.TipoMovimiento
import com.example.bodega.data.local.MovimientoResumen
import com.example.bodega.ui.components.Cargando
import com.example.bodega.ui.components.EstadoVacio
import com.example.bodega.util.Formato
import com.example.bodega.viewmodel.DetalleDialogo
import com.example.bodega.viewmodel.MovimientosViewModel

/** Historial de movimientos guardados en el celular y envio de los pendientes. */
@Composable
fun MovimientosScreen(viewModel: MovimientosViewModel) {
    val context = LocalContext.current
    val movimientos by viewModel.movimientos.collectAsStateWithLifecycle()
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()
    val sincronizando by viewModel.sincronizando.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val detalle by viewModel.detalle.collectAsStateWithLifecycle()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumirMensaje()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pendientes de envío: $pendientes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Los movimientos se guardan en el celular y se envían a tu servidor al sincronizar (también se actualiza el catálogo).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = viewModel::sincronizar,
                    enabled = !sincronizando
                ) {
                    if (sincronizando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                        Text(text = " Enviar")
                    }
                }
            }
        }

        val lista = movimientos
        when {
            lista == null -> Cargando()

            lista.isEmpty() -> EstadoVacio("Aún no hay movimientos.\nPresiona + para registrar una entrada o salida.")

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = lista, key = { it.movimiento.id }) { resumen ->
                    MovimientoCard(
                        resumen = resumen,
                        onClick = { viewModel.verDetalle(resumen) }
                    )
                }
            }
        }
    }

    detalle?.let { d ->
        DetalleDialog(detalle = d, onDismiss = viewModel::cerrarDetalle)
    }
}

@Composable
private fun MovimientoCard(
    resumen: MovimientoResumen,
    onClick: () -> Unit
) {
    val m = resumen.movimiento
    val colorEstado = when (m.syncEstado) {
        SyncEstado.ENVIADO -> MaterialTheme.colorScheme.primary
        SyncEstado.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TipoMovimiento.etiqueta(m.tipo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formato.moneda(resumen.total),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "${Formato.fecha(m.fechaHora)} · ${resumen.items} producto(s)",
                style = MaterialTheme.typography.bodySmall
            )
            if (m.observacion.isNotBlank()) {
                Text(text = m.observacion, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = SyncEstado.etiqueta(m.syncEstado),
                style = MaterialTheme.typography.labelMedium,
                color = colorEstado,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DetalleDialog(
    detalle: DetalleDialogo,
    onDismiss: () -> Unit
) {
    val m = detalle.resumen.movimiento
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${TipoMovimiento.etiqueta(m.tipo)} · ${Formato.fecha(m.fechaHora)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detalle.lineas.forEach { linea ->
                    Column {
                        Text(text = linea.producto, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Cód. ${linea.codigo} · ${linea.cantidad} x ${Formato.moneda(linea.precioUnitario)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = "Total: ${Formato.moneda(detalle.resumen.total)}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Código interno: ${m.uuid.take(8)}…",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cerrar")
            }
        }
    )
}
