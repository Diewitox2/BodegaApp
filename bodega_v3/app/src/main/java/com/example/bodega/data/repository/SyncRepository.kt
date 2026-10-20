package com.example.bodega.data.repository

import com.example.bodega.constant.SyncEstado
import com.example.bodega.data.local.CatalogoSyncDao
import com.example.bodega.data.local.CategoriaDao
import com.example.bodega.data.local.MovimientoDao
import com.example.bodega.data.local.ProductoDao
import com.example.bodega.data.local.ProveedorDao
import com.example.bodega.data.remote.ApiService
import com.example.bodega.data.remote.CatalogoDto
import com.example.bodega.data.remote.CategoriaDto
import com.example.bodega.data.remote.DetalleRequest
import com.example.bodega.data.remote.MovimientoRequest
import com.example.bodega.data.remote.ProductoDto
import com.example.bodega.data.remote.ProveedorDto
import com.example.bodega.data.remote.RetrofitClient
import com.example.bodega.session.SessionManager
import com.example.bodega.util.Formato
import kotlinx.coroutines.CancellationException
import java.io.IOException

data class ResultadoSync(
    val total: Int,
    val enviados: Int,
    val fallidos: Int,
    /** Ultimo motivo de rechazo del servidor (por ejemplo "Stock insuficiente..."). */
    val aviso: String? = null
)

/** El token del celular ya no sirve o no existe: hay que iniciar sesion con internet. */
class SesionExpiradaException : Exception(
    "Tu sesión no está activa en el servidor. Cierra sesión e ingresa con internet."
)

interface SyncRepository {

    /**
     * Sincronizacion en 3 pasos (todo con Retrofit):
     *  1) POST catalogo.php     -> sube categorias, proveedores y productos creados en el celular.
     *  2) POST movimientos.php  -> envia cada movimiento pendiente (el UUID evita duplicados).
     *  3) GET  catalogo.php     -> baja el catalogo del servidor y, si todo se envio, su stock.
     */
    suspend fun sincronizar(): ResultadoSync
}

class SyncRepositoryImpl(
    private val movimientoDao: MovimientoDao,
    private val productoDao: ProductoDao,
    private val categoriaDao: CategoriaDao,
    private val proveedorDao: ProveedorDao,
    private val catalogoSyncDao: CatalogoSyncDao,
    private val api: ApiService,
    private val session: SessionManager
) : SyncRepository {

    override suspend fun sincronizar(): ResultadoSync {
        val token = session.token
        if (token.isEmpty()) throw SesionExpiradaException()

        val pendientes = movimientoDao.pendientes()

        // 1) Subir catalogo
        val subida = api.subirCatalogo(token, construirCatalogo())
        if (subida.code() == 401) throw SesionExpiradaException()
        if (!subida.isSuccessful) throw IOException(RetrofitClient.mensajeError(subida))

        // 2) Enviar movimientos pendientes
        var enviados = 0
        var fallidos = 0
        var aviso: String? = null
        for (movimiento in pendientes) {
            try {
                val detalle = movimientoDao.detalleParaEnvio(movimiento.id).map {
                    DetalleRequest(
                        codigo = it.codigo,
                        cantidad = it.cantidad,
                        precioUnitario = it.precioUnitario
                    )
                }
                val ruc = movimiento.proveedorId?.let { proveedorDao.getById(it)?.ruc }
                val respuesta = api.enviarMovimiento(
                    token,
                    MovimientoRequest(
                        uuid = movimiento.uuid,
                        tipo = movimiento.tipo,
                        fecha = Formato.fechaIso(movimiento.fechaHora),
                        observacion = movimiento.observacion,
                        proveedorRuc = ruc,
                        detalle = detalle
                    )
                )
                if (respuesta.isSuccessful) {
                    movimientoDao.marcar(movimiento.id, SyncEstado.ENVIADO)
                    enviados++
                } else if (respuesta.code() == 401) {
                    throw SesionExpiradaException()
                } else {
                    movimientoDao.marcar(movimiento.id, SyncEstado.ERROR)
                    fallidos++
                    aviso = RetrofitClient.mensajeError(respuesta)
                }
            } catch (e: SesionExpiradaException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                movimientoDao.marcar(movimiento.id, SyncEstado.ERROR)
                fallidos++
            }
        }

        // 3) Bajar catalogo (el stock del servidor solo se aplica si no quedan pendientes)
        try {
            val bajada = api.catalogo(token)
            val cuerpo = bajada.body()
            if (bajada.isSuccessful && cuerpo != null) {
                val sinPendientes = fallidos == 0 && movimientoDao.contarPendientes() == 0
                catalogoSyncDao.aplicar(cuerpo, sinPendientes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si falla la bajada, los movimientos ya enviados quedan marcados igual.
        }

        return ResultadoSync(
            total = pendientes.size,
            enviados = enviados,
            fallidos = fallidos,
            aviso = aviso
        )
    }

    private suspend fun construirCatalogo(): CatalogoDto {
        val proveedores = proveedorDao.todos()
        val rucPorId = proveedores.associate { it.id to it.ruc }
        return CatalogoDto(
            categorias = categoriaDao.todas().map { CategoriaDto(it.nombre, it.activo) },
            proveedores = proveedores.map {
                ProveedorDto(it.razonSocial, it.ruc, it.telefono, it.activo)
            },
            productos = productoDao.todosConCategoria().map { vista ->
                val p = vista.producto
                ProductoDto(
                    codigo = p.codigo,
                    nombre = p.nombre,
                    categoria = vista.categoriaNombre,
                    proveedorRuc = p.proveedorId?.let { id -> rucPorId[id] },
                    precioCompra = p.precioCompra,
                    precioVenta = p.precioVenta,
                    stockMinimo = p.stockMinimo,
                    activo = p.activo
                )
            }
        )
    }
}
