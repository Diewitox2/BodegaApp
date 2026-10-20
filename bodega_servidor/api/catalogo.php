<?php
// GET  /api/catalogo.php -> {categorias[], proveedores[], productos[]}
// POST /api/catalogo.php -> sube categorias, proveedores y productos del celular (upsert por clave natural:
//                           categoria por nombre, proveedor por RUC, producto por codigo). El stock NUNCA se acepta del celular.
require_once __DIR__ . '/../lib.php';

api_inicio('GET', 'POST');
api_ejecutar(function () {
    $pdo = pdo();
    usuario_autenticado($pdo);

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $cats = array_map(fn($r) => ['nombre' => $r['nombre'], 'activo' => (bool)$r['activo']],
            $pdo->query('SELECT nombre, activo FROM categorias ORDER BY nombre')->fetchAll());
        $provs = array_map(fn($r) => [
            'razon_social' => $r['razon_social'], 'ruc' => $r['ruc'],
            'telefono' => $r['telefono'] ?? '', 'activo' => (bool)$r['activo'],
        ], $pdo->query('SELECT razon_social, ruc, telefono, activo FROM proveedores ORDER BY razon_social')->fetchAll());
        $prods = array_map(fn($r) => [
            'codigo' => $r['codigo'], 'nombre' => $r['nombre'], 'categoria' => $r['categoria'],
            'proveedor_ruc' => $r['proveedor_ruc'],
            'precio_compra' => (float)$r['precio_compra'], 'precio_venta' => (float)$r['precio_venta'],
            'stock_minimo' => (int)$r['stock_minimo'], 'stock_actual' => (int)$r['stock_actual'],
            'activo' => (bool)$r['activo'],
        ], $pdo->query(
            'SELECT p.codigo, p.nombre, c.nombre AS categoria, v.ruc AS proveedor_ruc, p.precio_compra,
                    p.precio_venta, p.stock_minimo, p.stock_actual, p.activo
             FROM productos p JOIN categorias c ON c.id = p.categoria_id
             LEFT JOIN proveedores v ON v.id = p.proveedor_id ORDER BY p.nombre'
        )->fetchAll());
        json_out(['categorias' => $cats, 'proveedores' => $provs, 'productos' => $prods]);
    }

    // ---- POST ----
    $d = leer_json();
    $pdo->beginTransaction();
    try {
        $n = ['categorias' => 0, 'proveedores' => 0, 'productos' => 0];

        foreach (($d['categorias'] ?? []) as $c) {
            $nombre = trim((string)($c['nombre'] ?? ''));
            if ($nombre === '') continue;
            $pdo->prepare('INSERT INTO categorias (nombre, activo) VALUES (?, ?)
                           ON DUPLICATE KEY UPDATE activo = VALUES(activo)')
                ->execute([$nombre, !empty($c['activo']) ? 1 : 0]);
            $n['categorias']++;
        }
        foreach (($d['proveedores'] ?? []) as $p) {
            $ruc = trim((string)($p['ruc'] ?? ''));
            $razon = trim((string)($p['razon_social'] ?? ''));
            if (!preg_match('/^\d{11}$/', $ruc) || $razon === '') continue;
            $pdo->prepare('INSERT INTO proveedores (razon_social, ruc, telefono, activo) VALUES (?, ?, ?, ?)
                           ON DUPLICATE KEY UPDATE razon_social = VALUES(razon_social),
                           telefono = VALUES(telefono), activo = VALUES(activo)')
                ->execute([$razon, $ruc, trim((string)($p['telefono'] ?? '')) ?: null, !empty($p['activo']) ? 1 : 0]);
            $n['proveedores']++;
        }
        foreach (($d['productos'] ?? []) as $p) {
            $codigo = trim((string)($p['codigo'] ?? ''));
            $nombre = trim((string)($p['nombre'] ?? ''));
            if ($codigo === '' || $nombre === '') continue;

            $q = $pdo->prepare('SELECT id FROM categorias WHERE nombre = ?');
            $q->execute([trim((string)($p['categoria'] ?? ''))]);
            $categoriaId = $q->fetchColumn();
            if (!$categoriaId) continue;                    // categoria desconocida: se omite

            $proveedorId = null;
            if (!empty($p['proveedor_ruc'])) {
                $q = $pdo->prepare('SELECT id FROM proveedores WHERE ruc = ?');
                $q->execute([$p['proveedor_ruc']]);
                $proveedorId = $q->fetchColumn() ?: null;
            }
            $compra = max(0, (float)($p['precio_compra'] ?? 0));
            $venta = max(0, (float)($p['precio_venta'] ?? 0));
            $minimo = max(0, (int)($p['stock_minimo'] ?? 0));

            $pdo->prepare('INSERT INTO productos (codigo, nombre, categoria_id, proveedor_id, precio_compra,
                               precio_venta, stock_minimo, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                           ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), categoria_id = VALUES(categoria_id),
                               proveedor_id = VALUES(proveedor_id), precio_compra = VALUES(precio_compra),
                               precio_venta = VALUES(precio_venta), stock_minimo = VALUES(stock_minimo),
                               activo = VALUES(activo)')
                ->execute([$codigo, $nombre, $categoriaId, $proveedorId, $compra, $venta, $minimo, !empty($p['activo']) ? 1 : 0]);
            $n['productos']++;
        }
        $pdo->commit();
        json_out(['ok' => true] + $n);
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
});
