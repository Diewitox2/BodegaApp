<?php
// Mantenimiento generico (listar, crear, editar, eliminar) segun _tablas.php
require_once __DIR__ . '/_base.php';
$u = requiere_admin();
$pdo = pdo();
$TABLAS = require __DIR__ . '/_tablas.php';

$t = $_GET['t'] ?? '';
if (!isset($TABLAS[$t])) { http_response_code(404); exit('Tabla no encontrada'); }
$cfg = $TABLAS[$t];
$soloLectura = !empty($cfg['solo_lectura']);
$soloEliminar = !empty($cfg['solo_eliminar']);
$accion = $_GET['accion'] ?? 'listar';
$id = (int)($_GET['id'] ?? 0);
$ok = $_GET['ok'] ?? null;
$error = null;
$valores = [];

// ------------------------------------------------------------ POST: guardar / eliminar
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verificar_csrf();

    if (($_POST['accion'] ?? '') === 'eliminar' && !$soloLectura) {
        $idDel = (int)($_POST['id'] ?? 0);
        if ($t === 'usuarios' && $idDel === $u['id']) {
            $error = 'No puedes eliminar tu propio usuario.';
        } else {
            try {
                $pdo->prepare("DELETE FROM `$t` WHERE id = ?")->execute([$idDel]);
                header("Location: tabla.php?t=$t&ok=" . urlencode('Registro eliminado.'));
                exit;
            } catch (PDOException $e) {
                $error = mensaje_pdo($e);
            }
        }
    } elseif (!$soloLectura && !$soloEliminar) {
        $id = (int)($_POST['id'] ?? 0);
        $datos = [];
        foreach ($cfg['campos'] as $c) {
            $n = $c['name'];
            $v = $_POST[$n] ?? '';
            $req = !empty($c['req']);
            switch ($c['type']) {
                case 'bool':
                    $datos[$n] = isset($_POST[$n]) ? 1 : 0;
                    break;
                case 'password':
                    if ($v !== '') {
                        if (strlen($v) < 6) { $error = 'La contraseña debe tener al menos 6 caracteres.'; break; }
                        $datos['password_hash'] = password_hash($v, PASSWORD_DEFAULT);
                    } elseif ($id === 0) {
                        $error = 'Ingresa una contraseña.';
                    }
                    break;
                case 'int':
                    if (!preg_match('/^\d+$/', trim($v))) { $error = $c['label'] . ': ingresa un entero válido.'; break; }
                    $datos[$n] = (int)$v;
                    break;
                case 'number':
                    $v = str_replace(',', '.', trim($v));
                    if (!is_numeric($v) || (float)$v < 0) { $error = $c['label'] . ': ingresa un monto válido.'; break; }
                    $datos[$n] = round((float)$v, 2);
                    break;
                case 'fk':
                    if ($v === '') { if ($req) $error = $c['label'] . ' es obligatorio.'; $datos[$n] = null; }
                    else $datos[$n] = (int)$v;
                    break;
                case 'select':
                    if (!in_array($v, $c['opciones'], true)) { $error = $c['label'] . ' inválido.'; break; }
                    $datos[$n] = $v;
                    break;
                default: // text, email
                    $v = recortar(trim((string)$v), $c['max'] ?? 255);
                    if ($v === '' && $req) { $error = $c['label'] . ' es obligatorio.'; break; }
                    if ($v !== '' && $c['type'] === 'email' && !filter_var($v, FILTER_VALIDATE_EMAIL)) { $error = 'Correo inválido.'; break; }
                    if ($v !== '' && isset($c['regex']) && !preg_match($c['regex'], $v)) { $error = $c['label'] . ' no tiene el formato correcto.'; break; }
                    $datos[$n] = $c['type'] === 'email' ? strtolower($v) : ($v === '' ? null : $v);
            }
            if ($error) break;
        }
        if (!$error) {
            try {
                if ($id === 0) {
                    $cols = implode(',', array_map(fn($k) => "`$k`", array_keys($datos)));
                    $marcas = implode(',', array_fill(0, count($datos), '?'));
                    $pdo->prepare("INSERT INTO `$t` ($cols) VALUES ($marcas)")->execute(array_values($datos));
                } else {
                    $set = implode(',', array_map(fn($k) => "`$k` = ?", array_keys($datos)));
                    $pdo->prepare("UPDATE `$t` SET $set WHERE id = ?")->execute([...array_values($datos), $id]);
                }
                header("Location: tabla.php?t=$t&ok=" . urlencode('Guardado correctamente.'));
                exit;
            } catch (PDOException $e) {
                $error = mensaje_pdo($e);
            }
        }
        $valores = $_POST;          // se vuelve a mostrar el formulario con lo escrito
        $accion = $id === 0 ? 'nuevo' : 'editar';
    }
}

// ------------------------------------------------------------ vista
cabecera($cfg['titulo'], $u);

if (in_array($accion, ['nuevo', 'editar'], true) && !$soloLectura && !$soloEliminar) {
    if ($accion === 'editar' && !$valores) {
        $st = $pdo->prepare("SELECT * FROM `$t` WHERE id = ?");
        $st->execute([$id]);
        $valores = $st->fetch() ?: [];
        unset($valores['password_hash']);
        if (!$valores) { echo '<p>Registro no encontrado.</p>'; pie(); exit; }
    }
    echo '<h1>' . ($accion === 'nuevo' ? 'Nuevo' : 'Editar') . ' · ' . h($cfg['titulo']) . '</h1>';
    mensaje(null, $error);
    echo '<form class="f" method="post">' . campo_csrf() . '<input type="hidden" name="id" value="' . ($accion === 'editar' ? $id : 0) . '">';
    foreach ($cfg['campos'] as $c) {
        $n = $c['name'];
        $v = $valores[$n] ?? ($c['type'] === 'bool' ? 1 : '');
        if ($c['type'] === 'bool') {
            $marcado = !empty($v) && $v !== '0' ? ' checked' : '';
            if ($_SERVER['REQUEST_METHOD'] === 'POST' && !isset($_POST[$n])) $marcado = '';
            echo '<label><input type="checkbox" name="' . h($n) . '" value="1"' . $marcado . '> ' . h($c['label']) . '</label>';
            continue;
        }
        echo '<label>' . h($c['label']) . '</label>';
        if ($c['type'] === 'select') {
            echo '<select name="' . h($n) . '">';
            foreach ($c['opciones'] as $o) echo '<option' . ($o === $v ? ' selected' : '') . '>' . h($o) . '</option>';
            echo '</select>';
        } elseif ($c['type'] === 'fk') {
            echo '<select name="' . h($n) . '"><option value="">' . (!empty($c['req']) ? '— elige —' : '— ninguno —') . '</option>';
            foreach ($pdo->query("SELECT id, `{$c['etiqueta']}` AS e FROM `{$c['tabla']}` ORDER BY e")->fetchAll() as $f) {
                echo '<option value="' . $f['id'] . '"' . ((string)$f['id'] === (string)$v ? ' selected' : '') . '>' . h($f['e']) . '</option>';
            }
            echo '</select>';
        } else {
            $tipo = $c['type'] === 'number' ? 'number" step="0.01" min="0' : ($c['type'] === 'int' ? 'number" step="1" min="0' : $c['type']);
            $valor = $c['type'] === 'password' ? '' : h($v);
            echo '<input type="' . $tipo . '" name="' . h($n) . '" value="' . $valor . '"' . (!empty($c['req']) && $c['type'] !== 'password' ? ' required' : '') . '>';
        }
    }
    echo '<p><button class="btn">Guardar</button> <a class="btn sec" href="tabla.php?t=' . h($t) . '">Cancelar</a></p></form>';
    pie();
    exit;
}

// listado
$filas = $pdo->query($cfg['sql'])->fetchAll();
echo '<div class="barra"><h1>' . h($cfg['titulo']) . '</h1>';
if (!$soloLectura && !$soloEliminar) echo '<a class="btn" href="tabla.php?t=' . h($t) . '&accion=nuevo">+ Nuevo</a>';
echo '</div>';
if (!empty($cfg['nota'])) echo '<p style="color:#666">' . h($cfg['nota']) . '</p>';
mensaje($ok, $error);
if (!$filas) {
    echo '<p>No hay registros.</p>';
} else {
    echo '<div class="tabla"><table><tr>';
    foreach (array_keys($filas[0]) as $col) echo '<th>' . h(str_replace('_', ' ', $col)) . '</th>';
    if (!$soloLectura) echo '<th></th>';
    echo '</tr>';
    foreach ($filas as $f) {
        $inactivo = array_key_exists('activo', $f) && !$f['activo'];
        echo '<tr' . ($inactivo ? ' class="inactivo"' : '') . '>';
        foreach ($f as $col => $val) echo '<td>' . ($col === 'activo' ? ($val ? 'Sí' : 'No') : h($val ?? '—')) . '</td>';
        if (!$soloLectura) {
            echo '<td style="white-space:nowrap">';
            if (!$soloEliminar) echo '<a class="btn chico sec" href="tabla.php?t=' . h($t) . '&accion=editar&id=' . (int)$f['id'] . '">Editar</a> ';
            echo '<form method="post" style="display:inline" onsubmit="return confirm(\'¿Eliminar este registro?\')">'
                . campo_csrf() . '<input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="' . (int)$f['id']
                . '"><button class="btn chico rojo">Eliminar</button></form></td>';
        }
        echo '</tr>';
    }
    echo '</table></div>';
}
pie();
