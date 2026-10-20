<?php
require_once __DIR__ . '/config.php';

/** Error controlado: se convierte en una respuesta JSON con su codigo HTTP. */
class ErrorApi extends Exception
{
    public function __construct(string $mensaje, public int $http = 400)
    {
        parent::__construct($mensaje);
    }
}

function pdo(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        $pdo = new PDO(
            'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
            DB_USER,
            DB_PASS,
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]
        );
        $pdo->exec("SET time_zone = '-05:00'");
    }
    return $pdo;
}

/** Recorta a N caracteres (UTF-8) sin depender de la extension mbstring. */
function recortar(string $texto, int $max): string
{
    return preg_match('/^.{0,' . $max . '}/us', $texto, $m) ? $m[0] : substr($texto, 0, $max);
}

function largo(string $texto): int
{
    return (int)preg_match_all('/./us', $texto);
}

function h($valor): string
{
    return htmlspecialchars((string)$valor, ENT_QUOTES, 'UTF-8');
}

// ---------------------------------------------------------------------
// API (JSON)
// ---------------------------------------------------------------------
function json_out($datos, int $http = 200): never
{
    http_response_code($http);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($datos, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function api_inicio(string ...$metodos): void
{
    if (!in_array($_SERVER['REQUEST_METHOD'], $metodos, true)) {
        json_out(['error' => 'Método no permitido'], 405);
    }
}

function leer_json(): array
{
    $datos = json_decode(file_get_contents('php://input') ?: '', true);
    if (!is_array($datos)) {
        throw new ErrorApi('El cuerpo debe ser JSON válido', 400);
    }
    return $datos;
}

/** Ejecuta un endpoint y convierte cualquier error en JSON. */
function api_ejecutar(callable $accion): void
{
    try {
        $accion();
    } catch (ErrorApi $e) {
        json_out(['error' => $e->getMessage()], $e->http);
    } catch (Throwable $e) {
        error_log($e->getMessage());
        json_out(['error' => 'Error interno del servidor'], 500);
    }
}

function crear_token(PDO $pdo, int $usuarioId, string $dispositivo = 'Celular'): string
{
    $token = bin2hex(random_bytes(32));                       // 64 caracteres, solo lo conoce el celular
    $st = $pdo->prepare(
        'INSERT INTO tokens_acceso (usuario_id, nombre_dispositivo, token_hash, expira_en)
         VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ' . (int)TOKEN_DIAS . ' DAY))'
    );
    $st->execute([$usuarioId, recortar($dispositivo, 100), hash('sha256', $token)]);
    return $token;
}

/** Devuelve el usuario dueño del token (cabecera X-Token) o responde 401. */
function usuario_autenticado(PDO $pdo): array
{
    $token = $_SERVER['HTTP_X_TOKEN'] ?? '';
    if ($token === '' && preg_match('/Bearer\s+(\S+)/', $_SERVER['HTTP_AUTHORIZATION'] ?? '', $m)) {
        $token = $m[1];
    }
    if ($token === '') {
        throw new ErrorApi('Falta el token de acceso', 401);
    }
    $st = $pdo->prepare(
        'SELECT u.id, u.nombre, u.email, u.rol, t.id AS token_id
         FROM tokens_acceso t JOIN usuarios u ON u.id = t.usuario_id
         WHERE t.token_hash = ? AND t.expira_en > NOW() AND u.activo = 1'
    );
    $st->execute([hash('sha256', $token)]);
    $usuario = $st->fetch();
    if (!$usuario) {
        throw new ErrorApi('Token inválido o vencido', 401);
    }
    $pdo->prepare('UPDATE tokens_acceso SET ultimo_uso = NOW() WHERE id = ?')->execute([$usuario['token_id']]);
    return $usuario;
}

// ---------------------------------------------------------------------
// Registro de movimientos (lo usan la API y el portal)
// ---------------------------------------------------------------------
/**
 * Registra un movimiento y actualiza el stock en UNA transaccion.
 * $m: uuid, tipo, fecha, observacion, proveedor_ruc | proveedor_id, cliente_id,
 *     detalle[] = {codigo | producto_id, cantidad, precio_unitario}
 * Devuelve ['duplicado' => bool]. Lanza ErrorApi 422 (datos) o 409 (stock insuficiente).
 */
function registrar_movimiento(PDO $pdo, array $m, int $usuarioId): array
{
    $tipos = ['ENTRADA', 'SALIDA', 'AJUSTE_POS', 'AJUSTE_NEG'];
    $uuid = trim((string)($m['uuid'] ?? ''));
    $tipo = (string)($m['tipo'] ?? '');
    $detalle = $m['detalle'] ?? [];

    if (!preg_match('/^[0-9a-fA-F-]{36}$/', $uuid)) throw new ErrorApi('uuid inválido', 422);
    if (!in_array($tipo, $tipos, true)) throw new ErrorApi('Tipo de movimiento inválido', 422);
    if (!is_array($detalle) || count($detalle) === 0) throw new ErrorApi('El movimiento no tiene productos', 422);

    $ts = strtotime((string)($m['fecha'] ?? ''));
    $fecha = date('Y-m-d H:i:s', $ts ?: time());
    $observacion = recortar(trim((string)($m['observacion'] ?? '')), 255);
    $suma = in_array($tipo, ['ENTRADA', 'AJUSTE_POS'], true);

    $pdo->beginTransaction();
    try {
        $st = $pdo->prepare('SELECT id FROM movimientos WHERE uuid = ?');
        $st->execute([$uuid]);
        if ($st->fetch()) {                       // ya lo recibimos antes: no se duplica
            $pdo->rollBack();
            return ['duplicado' => true];
        }

        $proveedorId = null;
        if ($tipo === 'ENTRADA') {
            if (!empty($m['proveedor_id'])) {
                $proveedorId = (int)$m['proveedor_id'];
            } elseif (!empty($m['proveedor_ruc'])) {
                $p = $pdo->prepare('SELECT id FROM proveedores WHERE ruc = ?');
                $p->execute([$m['proveedor_ruc']]);
                $proveedorId = $p->fetchColumn() ?: null;
            }
        }
        $clienteId = ($tipo === 'SALIDA' && !empty($m['cliente_id'])) ? (int)$m['cliente_id'] : null;

        $pdo->prepare(
            'INSERT INTO movimientos (uuid, tipo, fecha_hora, usuario_id, proveedor_id, cliente_id, observacion)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        )->execute([$uuid, $tipo, $fecha, $usuarioId, $proveedorId, $clienteId, $observacion ?: null]);
        $movimientoId = (int)$pdo->lastInsertId();

        $vistos = [];
        foreach ($detalle as $linea) {
            if (!is_array($linea)) throw new ErrorApi('Línea inválida', 422);
            $cantidad = (int)($linea['cantidad'] ?? 0);
            $precio = (float)($linea['precio_unitario'] ?? 0);
            if ($cantidad <= 0) throw new ErrorApi('La cantidad debe ser mayor que cero', 422);
            if ($precio < 0) throw new ErrorApi('El precio no puede ser negativo', 422);

            if (!empty($linea['producto_id'])) {
                $q = $pdo->prepare('SELECT id, codigo, nombre, stock_actual FROM productos WHERE id = ? FOR UPDATE');
                $q->execute([(int)$linea['producto_id']]);
            } else {
                $q = $pdo->prepare('SELECT id, codigo, nombre, stock_actual FROM productos WHERE codigo = ? FOR UPDATE');
                $q->execute([(string)($linea['codigo'] ?? '')]);
            }
            $producto = $q->fetch();
            if (!$producto) {
                throw new ErrorApi('Producto no registrado en el servidor: ' . ($linea['codigo'] ?? $linea['producto_id'] ?? '?'), 422);
            }
            if (isset($vistos[$producto['id']])) {
                throw new ErrorApi('Producto repetido en el movimiento: ' . $producto['nombre'], 422);
            }
            $vistos[$producto['id']] = true;

            $delta = $suma ? $cantidad : -$cantidad;
            if ($producto['stock_actual'] + $delta < 0) {
                throw new ErrorApi("Stock insuficiente de {$producto['nombre']} (disponible: {$producto['stock_actual']})", 409);
            }
            $pdo->prepare(
                'INSERT INTO detalle_movimientos (movimiento_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)'
            )->execute([$movimientoId, $producto['id'], $cantidad, $precio]);
            $pdo->prepare('UPDATE productos SET stock_actual = stock_actual + ? WHERE id = ?')
                ->execute([$delta, $producto['id']]);
        }
        $pdo->commit();
        return ['duplicado' => false];
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
}
