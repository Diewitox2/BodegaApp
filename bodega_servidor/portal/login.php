<?php
require_once __DIR__ . '/_base.php';

$error = null;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verificar_csrf();
    $st = pdo()->prepare('SELECT id, nombre, email, rol, password_hash FROM usuarios WHERE email = ? AND activo = 1');
    $st->execute([strtolower(trim($_POST['email'] ?? ''))]);
    $u = $st->fetch();
    if ($u && password_verify($_POST['password'] ?? '', $u['password_hash'])) {
        if ($u['rol'] !== 'ADMIN') {
            $error = 'Solo los administradores pueden entrar al portal.';
        } else {
            session_regenerate_id(true);
            $_SESSION['u'] = ['id' => (int)$u['id'], 'nombre' => $u['nombre'], 'email' => $u['email']];
            header('Location: index.php');
            exit;
        }
    } else {
        $error = 'Correo o contraseña incorrectos.';
    }
}
cabecera('Ingresar');
?>
<h1>Portal administrativo</h1>
<?php mensaje(null, $error); ?>
<form class="f" method="post">
  <?= campo_csrf() ?>
  <label>Correo</label><input type="email" name="email" required autofocus>
  <label>Contraseña</label><input type="password" name="password" required>
  <p><button class="btn">Ingresar</button></p>
  <p style="font-size:.85em;color:#666">¿Primera vez y no hay usuarios? Usa <a href="crear_admin.php">crear_admin.php</a>
  o registra la primera cuenta desde la app (queda como administrador).</p>
</form>
<?php pie();
