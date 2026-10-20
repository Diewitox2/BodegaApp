# Servidor BodegaApp (API REST + Portal administrativo)

PHP 8 + MySQL/MariaDB. Funciona en cualquier hosting gratuito con PHP y MySQL (sin Composer ni extensiones especiales).

## Instalación en el hosting
1. En el panel del hosting crea una **base de datos MySQL** y anota: servidor, nombre, usuario y contraseña.
2. En phpMyAdmin, selecciona esa base e **importa `bodega_hosting.sql`**.
3. Edita **`config.php`** con los datos del paso 1.
4. Sube por FTP/administrador de archivos a una carpeta, por ejemplo `htdocs/bodega/`:
   `api/`, `portal/`, `lib.php`, `config.php`.
5. Abre `https://TU-DOMINIO/bodega/portal/crear_admin.php`, crea el administrador y **borra ese archivo**.
6. Entra a `https://TU-DOMINIO/bodega/portal/login.php`.
7. En la app Android cambia `API_BASE_URL` en `constant/Constants.kt` por `https://TU-DOMINIO/bodega/api/`.

> Prueba local con XAMPP: copia la carpeta a `htdocs/bodega/` y en la app usa `http://10.0.2.2/bodega/api/` (emulador).
> Si tu hosting devuelve una página HTML de "verificación de navegador" en vez de JSON (pasa en algunos hostings gratuitos),
> la app no podrá conectarse: prueba la URL en Postman y, si falla, cambia de hosting.

## Servicios REST (todos devuelven JSON)
| Método | URL | Descripción |
|---|---|---|
| POST | `/api/register.php` | `{nombre,email,password}` → crea cuenta (la 1.ª es ADMIN) y devuelve `{token,usuario}` |
| POST | `/api/login.php` | `{email,password}` → `{token,usuario}` |
| GET | `/api/catalogo.php` | Categorías, proveedores y productos (con stock) |
| POST | `/api/catalogo.php` | Sube catálogo del celular (upsert por nombre / RUC / código). El stock nunca se acepta del celular |
| POST | `/api/movimientos.php` | Recibe un movimiento. 201 creado, 200 duplicado (mismo `uuid`), 409 stock insuficiente, 422 datos inválidos |
| GET | `/api/movimientos.php?limite=20` | Últimos movimientos |

El token va en la cabecera **`X-Token`**. Se guarda solo su hash SHA-256 (`tokens_acceso.token_hash`) y vence a los 30 días.

## Portal administrativo (solo ADMIN)
Mantenimiento (listar, crear, editar, eliminar) de: usuarios, categorías, proveedores, clientes y productos.
Movimientos: listado, detalle y registro (no se editan ni se borran). Detalle de movimientos: solo lectura.
Tokens: listar y revocar. El stock cambia únicamente con movimientos, dentro de una transacción.

## Seguridad aplicada
Contraseñas con `password_hash` (bcrypt), consultas con PDO preparado, CSRF en formularios del portal,
sesión con cookie HttpOnly, tokens guardados como hash. Recomendado: poner `PERMITIR_REGISTRO = false` en `config.php`
cuando todos tus compañeros ya tengan cuenta.
