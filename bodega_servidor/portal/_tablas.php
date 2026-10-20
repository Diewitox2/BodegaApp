<?php
/**
 * Definicion de cada tabla del portal: que se lista, que campos se editan y que acciones hay.
 * Tipos de campo: text, email, password, int, number, select (opciones), fk (tabla + etiqueta), bool.
 */
return [
    'usuarios' => [
        'titulo' => 'Usuarios',
        'sql' => 'SELECT id, nombre, email, rol, activo FROM usuarios ORDER BY id DESC LIMIT 500',
        'campos' => [
            ['name' => 'nombre', 'label' => 'Nombre', 'type' => 'text', 'req' => true, 'max' => 100],
            ['name' => 'email', 'label' => 'Correo', 'type' => 'email', 'req' => true, 'max' => 120],
            ['name' => 'password', 'label' => 'Contraseña (al editar, vacío = no cambiar)', 'type' => 'password'],
            ['name' => 'rol', 'label' => 'Rol', 'type' => 'select', 'req' => true, 'opciones' => ['ADMIN', 'VENDEDOR']],
            ['name' => 'activo', 'label' => 'Activo', 'type' => 'bool'],
        ],
    ],
    'categorias' => [
        'titulo' => 'Categorías',
        'sql' => 'SELECT id, nombre, activo FROM categorias ORDER BY nombre LIMIT 500',
        'campos' => [
            ['name' => 'nombre', 'label' => 'Nombre', 'type' => 'text', 'req' => true, 'max' => 80],
            ['name' => 'activo', 'label' => 'Activa', 'type' => 'bool'],
        ],
    ],
    'proveedores' => [
        'titulo' => 'Proveedores',
        'sql' => 'SELECT id, razon_social, ruc, telefono, activo FROM proveedores ORDER BY razon_social LIMIT 500',
        'campos' => [
            ['name' => 'razon_social', 'label' => 'Razón social', 'type' => 'text', 'req' => true, 'max' => 150],
            ['name' => 'ruc', 'label' => 'RUC (11 dígitos)', 'type' => 'text', 'req' => true, 'max' => 11, 'regex' => '/^\d{11}$/'],
            ['name' => 'telefono', 'label' => 'Teléfono', 'type' => 'text', 'max' => 20],
            ['name' => 'activo', 'label' => 'Activo', 'type' => 'bool'],
        ],
    ],
    'clientes' => [
        'titulo' => 'Clientes',
        'sql' => 'SELECT id, nombre, documento, telefono, activo FROM clientes ORDER BY nombre LIMIT 500',
        'campos' => [
            ['name' => 'nombre', 'label' => 'Nombre', 'type' => 'text', 'req' => true, 'max' => 120],
            ['name' => 'documento', 'label' => 'DNI o RUC', 'type' => 'text', 'max' => 15],
            ['name' => 'telefono', 'label' => 'Teléfono', 'type' => 'text', 'max' => 20],
            ['name' => 'activo', 'label' => 'Activo', 'type' => 'bool'],
        ],
    ],
    'productos' => [
        'titulo' => 'Productos',
        'nota' => 'El stock actual no se edita aquí: cambia solo con movimientos.',
        'sql' => 'SELECT p.id, p.codigo, p.nombre, c.nombre AS categoria, v.razon_social AS proveedor,
                         p.precio_compra, p.precio_venta, p.stock_minimo, p.stock_actual, p.activo
                  FROM productos p JOIN categorias c ON c.id = p.categoria_id
                  LEFT JOIN proveedores v ON v.id = p.proveedor_id ORDER BY p.nombre LIMIT 500',
        'campos' => [
            ['name' => 'codigo', 'label' => 'Código', 'type' => 'text', 'req' => true, 'max' => 30],
            ['name' => 'nombre', 'label' => 'Nombre', 'type' => 'text', 'req' => true, 'max' => 150],
            ['name' => 'categoria_id', 'label' => 'Categoría', 'type' => 'fk', 'req' => true, 'tabla' => 'categorias', 'etiqueta' => 'nombre'],
            ['name' => 'proveedor_id', 'label' => 'Proveedor', 'type' => 'fk', 'tabla' => 'proveedores', 'etiqueta' => 'razon_social'],
            ['name' => 'precio_compra', 'label' => 'Precio de compra', 'type' => 'number', 'req' => true],
            ['name' => 'precio_venta', 'label' => 'Precio de venta', 'type' => 'number', 'req' => true],
            ['name' => 'stock_minimo', 'label' => 'Stock mínimo', 'type' => 'int', 'req' => true],
            ['name' => 'activo', 'label' => 'Activo', 'type' => 'bool'],
        ],
    ],
    // Los movimientos y su detalle no se editan ni se borran (se corrigen con un movimiento contrario).
    'detalle_movimientos' => [
        'titulo' => 'Detalle de movimientos',
        'nota' => 'Solo lectura: los movimientos no se editan ni se borran; se corrigen con un movimiento contrario.',
        'solo_lectura' => true,
        'sql' => 'SELECT d.id, m.uuid AS movimiento, m.tipo, p.codigo, p.nombre AS producto, d.cantidad, d.precio_unitario
                  FROM detalle_movimientos d JOIN movimientos m ON m.id = d.movimiento_id
                  JOIN productos p ON p.id = d.producto_id ORDER BY d.id DESC LIMIT 500',
        'campos' => [],
    ],
    'tokens_acceso' => [
        'titulo' => 'Tokens de acceso (dispositivos)',
        'nota' => 'Eliminar un token cierra la sesión de ese celular.',
        'solo_eliminar' => true,
        'sql' => 'SELECT t.id, u.email AS usuario, t.nombre_dispositivo AS dispositivo, t.expira_en, t.ultimo_uso, t.created_at
                  FROM tokens_acceso t JOIN usuarios u ON u.id = t.usuario_id ORDER BY t.id DESC LIMIT 500',
        'campos' => [],
    ],
];
