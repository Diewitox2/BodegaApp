package com.example.bodega.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bodega.constant.SyncEstado

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val rol: String,
    val activo: Boolean = true
)

@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val activo: Boolean = true
)

@Entity(
    tableName = "proveedores",
    indices = [Index(value = ["ruc"], unique = true)]
)
data class ProveedorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val razonSocial: String,
    val ruc: String,
    val telefono: String = "",
    val activo: Boolean = true
)

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"]
        ),
        ForeignKey(
            entity = ProveedorEntity::class,
            parentColumns = ["id"],
            childColumns = ["proveedorId"]
        )
    ],
    indices = [
        Index(value = ["codigo"], unique = true),
        Index(value = ["categoriaId"]),
        Index(value = ["proveedorId"])
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigo: String,
    val nombre: String,
    val categoriaId: Int,
    val proveedorId: Int? = null,
    val precioCompra: Double = 0.0,
    val precioVenta: Double = 0.0,
    val stockMinimo: Int = 0,
    /** Solo cambia al registrar movimientos; ninguna pantalla lo edita directamente. */
    val stockActual: Int = 0,
    val activo: Boolean = true
)

@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"]
        ),
        ForeignKey(
            entity = ProveedorEntity::class,
            parentColumns = ["id"],
            childColumns = ["proveedorId"]
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["usuarioId"]),
        Index(value = ["proveedorId"]),
        Index(value = ["fechaHora"])
    ]
)
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Identificador unico generado en el celular: evita duplicados al sincronizar. */
    val uuid: String,
    val tipo: String,
    val fechaHora: Long,
    val usuarioId: Int,
    val proveedorId: Int? = null,
    val observacion: String = "",
    val syncEstado: String = SyncEstado.PENDIENTE
)

@Entity(
    tableName = "detalle_movimientos",
    foreignKeys = [
        ForeignKey(
            entity = MovimientoEntity::class,
            parentColumns = ["id"],
            childColumns = ["movimientoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"]
        )
    ],
    indices = [
        Index(value = ["movimientoId", "productoId"], unique = true),
        Index(value = ["productoId"])
    ]
)
data class DetalleMovimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movimientoId: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)
