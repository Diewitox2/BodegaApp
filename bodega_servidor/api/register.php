<?php
// POST /api/register.php  {nombre, email, password}  ->  201 {token, usuario}
// La primera cuenta del sistema queda como ADMIN; las demas como VENDEDOR.
require_once __DIR__ . '/../lib.php';

api_inicio('POST');
api_ejecutar(function () {
    $pdo = pdo();
    $total = (int)$pdo->query('SELECT COUNT(*) FROM usuarios')->fetchColumn();
    if (!PERMITIR_REGISTRO && $total > 0) {
        throw new ErrorApi('El registro está deshabilitado. Pide una cuenta al administrador', 403);
    }

    $d = leer_json();
    $nombre = trim((string)($d['nombre'] ?? ''));
    $email = strtolower(trim((string)($d['email'] ?? '')));
    $clave = (string)($d['password'] ?? '');

    if ($nombre === '' || largo($nombre) > 100) throw new ErrorApi('Nombre inválido', 422);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) > 120) throw new ErrorApi('Correo inválido', 422);
    if (strlen($clave) < 6) throw new ErrorApi('La contraseña debe tener al menos 6 caracteres', 422);

    $st = $pdo->prepare('SELECT 1 FROM usuarios WHERE email = ?');
    $st->execute([$email]);
    if ($st->fetch()) throw new ErrorApi('Ese correo ya está registrado', 409);

    $rol = $total === 0 ? 'ADMIN' : 'VENDEDOR';
    $pdo->prepare('INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)')
        ->execute([$nombre, $email, password_hash($clave, PASSWORD_DEFAULT), $rol]);
    $id = (int)$pdo->lastInsertId();

    json_out([
        'token' => crear_token($pdo, $id, 'App Bodega'),
        'usuario' => ['id' => $id, 'nombre' => $nombre, 'email' => $email, 'rol' => $rol],
    ], 201);
});
