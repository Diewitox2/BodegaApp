<?php
require_once __DIR__ . '/_base.php';
$u = requiere_admin();
$pdo = pdo();
$conteo = fn(string $sql) => (int)$pdo->query($sql)->fetchColumn();

$tarjetas = [
    ['Productos activos', $conteo('SELECT COUNT(*) FROM productos WHERE activo = 1'), 'tabla.php?t=productos'],
    ['Stock bajo', $conteo('SELECT COUNT(*) FROM productos WHERE activo = 1 AND stock_actual <= stock_minimo'), 'tabla.php?t=productos'],
    ['Movimientos', $conteo('SELECT COUNT(*) FROM movimientos'), 'movimientos.php'],
    ['Usuarios', $conteo('SELECT COUNT(*) FROM usuarios'), 'tabla.php?t=usuarios'],
];
cabecera('Inicio', $u);
?>
<h1>Resumen</h1>
<div style="display:flex;flex-wrap:wrap;gap:12px">
<?php foreach ($tarjetas as [$titulo, $valor, $url]): ?>
  <a href="<?= h($url) ?>" style="text-decoration:none;color:inherit;background:#fff;padding:16px 22px;border-radius:10px;box-shadow:0 1px 3px #0002;min-width:150px">
    <div style="font-size:2em;font-weight:700;color:#2e7d32"><?= $valor ?></div><?= h($titulo) ?>
  </a>
<?php endforeach; ?>
</div>
<h2>Productos con stock bajo</h2>
<?php
$bajos = $pdo->query('SELECT codigo, nombre, stock_actual, stock_minimo FROM productos
                      WHERE activo = 1 AND stock_actual <= stock_minimo ORDER BY stock_actual LIMIT 20')->fetchAll();
if (!$bajos) echo '<p>Todo en orden.</p>'; else {
    echo '<div class="tabla"><table><tr><th>Código</th><th>Producto</th><th>Stock</th><th>Mínimo</th></tr>';
    foreach ($bajos as $b) echo '<tr><td>' . h($b['codigo']) . '</td><td>' . h($b['nombre']) . '</td><td>' . h($b['stock_actual']) . '</td><td>' . h($b['stock_minimo']) . '</td></tr>';
    echo '</table></div>';
}
pie();
