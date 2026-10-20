<?php
require_once __DIR__ . '/../lib.php';

session_set_cookie_params(['httponly' => true, 'samesite' => 'Lax']);
session_start();

function requiere_admin(): array
{
    if (empty($_SESSION['u'])) {
        header('Location: login.php');
        exit;
    }
    return $_SESSION['u'];
}

function csrf(): string
{
    if (empty($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(16));
    return $_SESSION['csrf'];
}

function campo_csrf(): string
{
    return '<input type="hidden" name="csrf" value="' . h(csrf()) . '">';
}

function verificar_csrf(): void
{
    if (!hash_equals($_SESSION['csrf'] ?? '', (string)($_POST['csrf'] ?? ''))) {
        http_response_code(400);
        exit('Solicitud inválida. Vuelve atrás y reintenta.');
    }
}

function cabecera(string $titulo, ?array $usuario = null): void
{
    $enlaces = [
        'tabla.php?t=usuarios' => 'Usuarios', 'tabla.php?t=categorias' => 'Categorías',
        'tabla.php?t=proveedores' => 'Proveedores', 'tabla.php?t=clientes' => 'Clientes',
        'tabla.php?t=productos' => 'Productos', 'movimientos.php' => 'Movimientos',
        'tabla.php?t=detalle_movimientos' => 'Detalle mov.', 'tabla.php?t=tokens_acceso' => 'Tokens',
    ];
    echo '<!doctype html><html lang="es"><head><meta charset="utf-8">'
        . '<meta name="viewport" content="width=device-width, initial-scale=1">'
        . '<title>' . h($titulo) . ' · Bodega</title><style>
        body{font-family:system-ui,Arial,sans-serif;margin:0;background:#f4f6f4;color:#222}
        header{background:#2e7d32;color:#fff;padding:10px 16px;display:flex;flex-wrap:wrap;gap:6px 14px;align-items:center}
        header a{color:#fff;text-decoration:none;padding:4px 8px;border-radius:6px}
        header a:hover{background:#1b5e20} header .yo{margin-left:auto;font-size:.9em}
        main{max-width:1100px;margin:18px auto;padding:0 14px}
        table{border-collapse:collapse;width:100%;background:#fff;box-shadow:0 1px 3px #0002}
        th,td{padding:8px 10px;border-bottom:1px solid #e3e3e3;text-align:left;font-size:.92em}
        th{background:#e8f5e9} .tabla{overflow-x:auto}
        .btn{display:inline-block;background:#2e7d32;color:#fff;border:0;border-radius:6px;padding:7px 14px;cursor:pointer;text-decoration:none;font-size:.95em}
        .btn.sec{background:#757575} .btn.rojo{background:#c62828} .btn.chico{padding:3px 9px;font-size:.85em}
        form.f{background:#fff;padding:16px;border-radius:8px;box-shadow:0 1px 3px #0002;max-width:560px}
        form.f label{display:block;margin:10px 0 3px;font-weight:600;font-size:.9em}
        form.f input[type=text],form.f input[type=email],form.f input[type=password],form.f input[type=number],form.f select{width:100%;padding:8px;border:1px solid #bbb;border-radius:6px;box-sizing:border-box}
        .msg{padding:10px 14px;border-radius:6px;margin:12px 0} .ok{background:#e8f5e9;color:#1b5e20} .err{background:#ffebee;color:#b71c1c}
        .inactivo{color:#999} h1{font-size:1.4em} .barra{display:flex;justify-content:space-between;align-items:center;margin-bottom:10px}
        </style></head><body><header><strong>BodegaApp</strong>';
    if ($usuario) {
        foreach ($enlaces as $url => $texto) echo '<a href="' . h($url) . '">' . h($texto) . '</a>';
        echo '<span class="yo">' . h($usuario['nombre']) . ' · <a href="logout.php">Salir</a></span>';
    }
    echo '</header><main>';
}

function pie(): void
{
    echo '</main></body></html>';
}

function mensaje(?string $ok, ?string $err): void
{
    if ($ok) echo '<div class="msg ok">' . h($ok) . '</div>';
    if ($err) echo '<div class="msg err">' . h($err) . '</div>';
}

function mensaje_pdo(PDOException $e): string
{
    $codigo = $e->errorInfo[1] ?? 0;
    if ($codigo == 1062) return 'Ya existe un registro con ese valor único (código, correo, RUC o nombre).';
    if ($codigo == 1451) return 'No se puede eliminar: tiene registros relacionados. Desactívalo en su lugar.';
    if ($codigo == 1452) return 'La relación elegida no existe.';
    if ($codigo == 4025 || $codigo == 3819) return 'Algún valor no cumple las reglas de la tabla (por ejemplo, negativos).';
    return 'No se pudo guardar el registro.';
}
