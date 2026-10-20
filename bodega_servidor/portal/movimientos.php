<?php
// Movimientos: listado, detalle y registro desde el portal. No se editan ni se borran.
require_once __DIR__ . '/_base.php';
$u = requiere_admin();
$pdo = pdo();
$error = null;
$ok = $_GET['ok'] ?? null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verificar_csrf();
    $detalle = [];
    foreach ($_POST['producto_id'] ?? [] as $i => $pid) {
        $cant = (int)($_POST['cantidad'][$i] ?? 0);
        if ($pid === '' && $cant === 0) continue;                 // fila vacia
        $detalle[] = [
            'producto_id' => (int)$pid,
            'cantidad' => $cant,
            'precio_unitario' => str_replace(',', '.', $_POST['precio'][$i] ?? '0'),
        ];
    }
    try {
        registrar_movimiento($pdo, [
            'uuid' => sprintf('%08x-%04x-%04x-%04x-%012x', random_int(0, 0xffffffff), random_int(0, 0xffff),
                random_int(0, 0xffff), random_int(0, 0xffff), random_int(0, 0xffffffffffff)),
            'tipo' => $_POST['tipo'] ?? '',
            'fecha' => date('c'),
            'observacion' => $_POST['observacion'] ?? '',
            'proveedor_id' => $_POST['proveedor_id'] ?? null,
            'cliente_id' => $_POST['cliente_id'] ?? null,
            'detalle' => $detalle,
        ], $u['id']);
        header('Location: movimientos.php?ok=' . urlencode('Movimiento registrado y stock actualizado.'));
        exit;
    } catch (ErrorApi $e) {
        $error = $e->getMessage();
    } catch (PDOException $e) {
        $error = mensaje_pdo($e);
    }
}

cabecera('Movimientos', $u);

// ---------------- detalle
if (isset($_GET['id'])) {
    $st = $pdo->prepare('SELECT m.*, u.nombre AS usuario, v.razon_social AS proveedor, c.nombre AS cliente
        FROM movimientos m JOIN usuarios u ON u.id = m.usuario_id
        LEFT JOIN proveedores v ON v.id = m.proveedor_id LEFT JOIN clientes c ON c.id = m.cliente_id WHERE m.id = ?');
    $st->execute([(int)$_GET['id']]);
    $m = $st->fetch();
    if (!$m) { echo '<p>No encontrado.</p>'; pie(); exit; }
    echo '<div class="barra"><h1>Movimiento ' . h($m['tipo']) . '</h1><a class="btn sec" href="movimientos.php">Volver</a></div>';
    echo '<p>' . h($m['fecha_hora']) . ' · Registrado por ' . h($m['usuario'])
        . ($m['proveedor'] ? ' · Proveedor: ' . h($m['proveedor']) : '')
        . ($m['cliente'] ? ' · Cliente: ' . h($m['cliente']) : '')
        . ($m['observacion'] ? '<br>Observación: ' . h($m['observacion']) : '')
        . '<br><small>UUID: ' . h($m['uuid']) . ' · Recibido: ' . h($m['created_at']) . '</small></p>';
    $d = $pdo->prepare('SELECT p.codigo, p.nombre, d.cantidad, d.precio_unitario, d.cantidad * d.precio_unitario AS subtotal
        FROM detalle_movimientos d JOIN productos p ON p.id = d.producto_id WHERE d.movimiento_id = ?');
    $d->execute([$m['id']]);
    echo '<div class="tabla"><table><tr><th>Código</th><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Subtotal</th></tr>';
    $total = 0;
    foreach ($d->fetchAll() as $l) {
        $total += $l['subtotal'];
        echo '<tr><td>' . h($l['codigo']) . '</td><td>' . h($l['nombre']) . '</td><td>' . h($l['cantidad']) . '</td><td>'
            . number_format($l['precio_unitario'], 2) . '</td><td>' . number_format($l['subtotal'], 2) . '</td></tr>';
    }
    echo '</table></div><p><strong>Total: S/ ' . number_format($total, 2) . '</strong></p>';
    pie();
    exit;
}

// ---------------- formulario nuevo
if (isset($_GET['nuevo']) || $error) {
    $productos = $pdo->query('SELECT id, codigo, nombre, stock_actual FROM productos WHERE activo = 1 ORDER BY nombre')->fetchAll();
    $provs = $pdo->query('SELECT id, razon_social FROM proveedores WHERE activo = 1 ORDER BY razon_social')->fetchAll();
    $clis = $pdo->query('SELECT id, nombre FROM clientes WHERE activo = 1 ORDER BY nombre')->fetchAll();
    echo '<h1>Nuevo movimiento</h1>';
    mensaje(null, $error);
    echo '<form class="f" method="post" style="max-width:760px">' . campo_csrf();
    echo '<label>Tipo</label><select name="tipo">';
    foreach (['ENTRADA', 'SALIDA', 'AJUSTE_POS', 'AJUSTE_NEG'] as $t) {
        echo '<option' . (($_POST['tipo'] ?? '') === $t ? ' selected' : '') . '>' . $t . '</option>';
    }
    echo '</select><label>Proveedor (solo ENTRADA)</label><select name="proveedor_id"><option value="">—</option>';
    foreach ($provs as $p) echo '<option value="' . $p['id'] . '">' . h($p['razon_social']) . '</option>';
    echo '</select><label>Cliente (solo SALIDA)</label><select name="cliente_id"><option value="">—</option>';
    foreach ($clis as $c) echo '<option value="' . $c['id'] . '">' . h($c['nombre']) . '</option>';
    echo '</select><label>Observación</label><input type="text" name="observacion" maxlength="255" value="' . h($_POST['observacion'] ?? '') . '">';
    echo '<label>Productos (hasta 5 líneas)</label>';
    for ($i = 0; $i < 5; $i++) {
        echo '<div style="display:flex;gap:6px;margin-bottom:6px"><select name="producto_id[]" style="flex:3"><option value="">—</option>';
        foreach ($productos as $p) {
            $sel = (($_POST['producto_id'][$i] ?? '') == $p['id']) ? ' selected' : '';
            echo '<option value="' . $p['id'] . '"' . $sel . '>' . h($p['nombre']) . ' (stock ' . $p['stock_actual'] . ')</option>';
        }
        echo '</select><input type="number" name="cantidad[]" min="1" placeholder="Cant." style="flex:1" value="' . h($_POST['cantidad'][$i] ?? '') . '">'
            . '<input type="number" name="precio[]" step="0.01" min="0" placeholder="Precio" style="flex:1" value="' . h($_POST['precio'][$i] ?? '') . '"></div>';
    }
    echo '<p><button class="btn">Registrar</button> <a class="btn sec" href="movimientos.php">Cancelar</a></p></form>';
    pie();
    exit;
}

// ---------------- listado
echo '<div class="barra"><h1>Movimientos</h1><a class="btn" href="movimientos.php?nuevo=1">+ Nuevo movimiento</a></div>';
mensaje($ok, null);
$filas = $pdo->query('SELECT m.id, m.fecha_hora, m.tipo, u.nombre AS usuario,
        (SELECT COUNT(*) FROM detalle_movimientos d WHERE d.movimiento_id = m.id) AS items,
        (SELECT COALESCE(SUM(d.cantidad * d.precio_unitario), 0) FROM detalle_movimientos d WHERE d.movimiento_id = m.id) AS total
    FROM movimientos m JOIN usuarios u ON u.id = m.usuario_id ORDER BY m.fecha_hora DESC LIMIT 300')->fetchAll();
if (!$filas) echo '<p>Todavía no hay movimientos.</p>'; else {
    echo '<div class="tabla"><table><tr><th>Fecha</th><th>Tipo</th><th>Usuario</th><th>Items</th><th>Total (S/)</th><th></th></tr>';
    foreach ($filas as $f) {
        echo '<tr><td>' . h($f['fecha_hora']) . '</td><td>' . h($f['tipo']) . '</td><td>' . h($f['usuario']) . '</td><td>' . h($f['items'])
            . '</td><td>' . number_format($f['total'], 2) . '</td><td><a class="btn chico sec" href="movimientos.php?id=' . (int)$f['id'] . '">Ver</a></td></tr>';
    }
    echo '</table></div>';
}
pie();
