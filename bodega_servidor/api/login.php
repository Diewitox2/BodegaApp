<?php
// POST /api/login.php  {email, password}  ->  {token, usuario}
require_once __DIR__ . '/../lib.php';

api_inicio('POST');
api_ejecutar(function () {
    $d = leer_json();
    $email = strtolower(trim((string)($d['email'] ?? '')));
    $clave = (string)($d['password'] ?? '');

    $pdo = pdo();
    $st = $pdo->prepare('SELECT id, nombre, email, rol, password_hash FROM usuarios WHERE email = ? AND activo = 1');
    $st->execute([$email]);
    $u = $st->fetch();
    if (!$u || !password_verify($clave, $u['password_hash'])) {
        throw new ErrorApi('Correo o contraseña incorrectos', 401);
    }
    $token = crear_token($pdo, (int)$u['id'], 'App Bodega');
    json_out([
        'token' => $token,
        'usuario' => ['id' => (int)$u['id'], 'nombre' => $u['nombre'], 'email' => $u['email'], 'rol' => $u['rol']],
    ]);
});
