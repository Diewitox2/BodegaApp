<?php
// POST /api/movimientos.php -> recibe un movimiento hecho en el celular.
//   {uuid, tipo, fecha, observacion, proveedor_ruc, detalle:[{codigo, cantidad, precio_unitario}]}
//   201 {ok:true, duplicado:false} | 200 {ok:true, duplicado:true} | 409 stock insuficiente | 422 datos invalidos
// GET  /api/movimientos.php?limite=20 -> ultimos movimientos (para revisar desde Postman).
require_once __DIR__ . '/../lib.php';

api_inicio('GET', 'POST');
api_ejecutar(function () {
    $pdo = pdo();
    $usuario = usuario_autenticado($pdo);

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $limite = max(1, min(100, (int)($_GET['limite'] ?? 20)));
        $st = $pdo->query(
            "SELECT m.uuid, m.tipo, m.fecha_hora, u.nombre AS usuario,
                    (SELECT COUNT(*) FROM detalle_movimientos d WHERE d.movimiento_id = m.id) AS items
             FROM movimientos m JOIN usuarios u ON u.id = m.usuario_id
             ORDER BY m.fecha_hora DESC LIMIT $limite"
        );
        json_out(['movimientos' => $st->fetchAll()]);
    }

    $resultado = registrar_movimiento($pdo, leer_json(), (int)$usuario['id']);
    json_out(['ok' => true, 'duplicado' => $resultado['duplicado']], $resultado['duplicado'] ? 200 : 201);
});
