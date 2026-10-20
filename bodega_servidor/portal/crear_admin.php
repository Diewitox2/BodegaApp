<?php
// Crea el PRIMER administrador. Solo funciona mientras no exista ningun usuario.
// Cuando termines, BORRA este archivo del hosting.
require_once __DIR__ . '/_base.php';

$pdo = pdo();
$hayUsuarios = (int)$pdo->query('SELECT COUNT(*) FROM usuarios')->fetchColumn() > 0;
$ok = $error = null;

if (!$hayUsuarios && $_SERVER['REQUEST_METHOD'] === 'POST') {
    verificar_csrf();
    $nombre = trim($_POST['nombre'] ?? '');
    $email = strtolower(trim($_POST['email'] ?? ''));
    $clave = (string)($_POST['password'] ?? '');
    if ($nombre === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($clave) < 6) {
        $error = 'Completa el nombre, un correo válido y una contraseña de 6 o más caracteres.';
    } else {
        $pdo->prepare("INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, 'ADMIN')")
            ->execute([$nombre, $email, password_hash($clave, PASSWORD_DEFAULT)]);
        $ok = 'Administrador creado. Ya puedes ingresar. Borra este archivo (crear_admin.php) del servidor.';
        $hayUsuarios = true;
    }
}
cabecera('Crear administrador');
?>
<h1>Crear administrador inicial</h1>
<?php mensaje($ok, $error); ?>
<?php if ($hayUsuarios): ?>
  <p>Ya existen usuarios; este asistente está deshabilitado. <a href="login.php">Ir al ingreso</a></p>
<?php else: ?>
<form class="f" method="post">
  <?= campo_csrf() ?>
  <label>Nombre</label><input type="text" name="nombre" required>
  <label>Correo</label><input type="email" name="email" required>
  <label>Contraseña (mínimo 6)</label><input type="password" name="password" required>
  <p><button class="btn">Crear administrador</button></p>
</form>
<?php endif; pie();
