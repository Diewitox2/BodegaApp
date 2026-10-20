-- =====================================================================
-- MODELO FISICO - SISTEMA DE BODEGA (version para HOSTING)
-- Motor: MySQL 8.0.16+ (necesario para que se apliquen los CHECK)
-- Almacenamiento: InnoDB | Codificacion: utf8mb4
-- =====================================================================

SET NAMES utf8mb4;

-- En el hosting la base de datos se crea desde el panel (phpMyAdmin / MySQL Databases).
-- Selecciona esa base en phpMyAdmin y luego importa este archivo.
-- (Si falla por collation, el motor usara la que tenga por defecto; no afecta al sistema.)

-- ---------------------------------------------------------------------
-- 1. ACCESO
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre         VARCHAR(100)    NOT NULL,
  email          VARCHAR(120)    NOT NULL,
  password_hash  VARCHAR(255)    NOT NULL,            -- bcrypt/argon2, nunca texto plano
  rol            ENUM('ADMIN','VENDEDOR') NOT NULL DEFAULT 'VENDEDOR',
  activo         TINYINT(1)      NOT NULL DEFAULT 1,
  created_at     TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuarios_email (email)
) ENGINE=InnoDB;

CREATE TABLE tokens_acceso (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id          BIGINT UNSIGNED NOT NULL,
  nombre_dispositivo  VARCHAR(100)    NULL,
  token_hash          CHAR(64)        NOT NULL,       -- se guarda el hash, no el token
  expira_en           DATETIME        NOT NULL,
  ultimo_uso          DATETIME        NULL,
  created_at          TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tokens_hash (token_hash),
  KEY idx_tokens_usuario (usuario_id),
  CONSTRAINT fk_tokens_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. CATALOGO
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(80)     NOT NULL,
  activo      TINYINT(1)      NOT NULL DEFAULT 1,
  created_at  TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categorias_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE proveedores (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  razon_social  VARCHAR(150)    NOT NULL,
  ruc           CHAR(11)        NOT NULL,
  telefono      VARCHAR(20)     NULL,
  activo        TINYINT(1)      NOT NULL DEFAULT 1,
  created_at    TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_proveedores_ruc (ruc)
) ENGINE=InnoDB;

CREATE TABLE clientes (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(120)    NOT NULL,
  documento   VARCHAR(15)     NULL,                   -- DNI o RUC
  telefono    VARCHAR(20)     NULL,
  activo      TINYINT(1)      NOT NULL DEFAULT 1,
  created_at  TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_clientes_documento (documento)
) ENGINE=InnoDB;

CREATE TABLE productos (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo         VARCHAR(30)     NOT NULL,
  nombre         VARCHAR(150)    NOT NULL,
  categoria_id   BIGINT UNSIGNED NOT NULL,
  proveedor_id   BIGINT UNSIGNED NULL,
  precio_compra  DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
  precio_venta   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
  stock_minimo   INT UNSIGNED    NOT NULL DEFAULT 0,
  -- Solo lo modifica el servidor al registrar movimientos (en transaccion).
  stock_actual   INT             NOT NULL DEFAULT 0,
  activo         TINYINT(1)      NOT NULL DEFAULT 1,
  created_at     TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_productos_codigo (codigo),
  KEY idx_productos_nombre (nombre),
  KEY idx_productos_categoria (categoria_id),
  KEY idx_productos_proveedor (proveedor_id),
  KEY idx_productos_updated (updated_at),             -- sincronizacion incremental
  CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id)
    REFERENCES categorias (id),
  CONSTRAINT fk_productos_proveedor FOREIGN KEY (proveedor_id)
    REFERENCES proveedores (id),
  CONSTRAINT ck_productos_precios CHECK (precio_compra >= 0 AND precio_venta >= 0),
  CONSTRAINT ck_productos_stock   CHECK (stock_actual >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. OPERACION
-- ---------------------------------------------------------------------
CREATE TABLE movimientos (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  uuid          CHAR(36)        NOT NULL,             -- lo genera el celular
  tipo          ENUM('ENTRADA','SALIDA','AJUSTE_POS','AJUSTE_NEG') NOT NULL,
  fecha_hora    DATETIME        NOT NULL,             -- momento real en el dispositivo
  usuario_id    BIGINT UNSIGNED NOT NULL,
  proveedor_id  BIGINT UNSIGNED NULL,
  cliente_id    BIGINT UNSIGNED NULL,
  observacion   VARCHAR(255)    NULL,
  created_at    TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP,   -- momento de recepcion en el servidor
  PRIMARY KEY (id),
  UNIQUE KEY uq_movimientos_uuid (uuid),              -- evita duplicados al reenviar
  KEY idx_movimientos_fecha (fecha_hora),
  KEY idx_movimientos_tipo (tipo),
  KEY idx_movimientos_usuario (usuario_id),
  CONSTRAINT fk_mov_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuarios (id),
  CONSTRAINT fk_mov_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
  CONSTRAINT fk_mov_cliente   FOREIGN KEY (cliente_id)   REFERENCES clientes (id),
  -- Proveedor solo en entradas, cliente solo en salidas
  CONSTRAINT ck_mov_proveedor CHECK (proveedor_id IS NULL OR tipo = 'ENTRADA'),
  CONSTRAINT ck_mov_cliente   CHECK (cliente_id   IS NULL OR tipo = 'SALIDA')
) ENGINE=InnoDB;

CREATE TABLE detalle_movimientos (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  movimiento_id    BIGINT UNSIGNED NOT NULL,
  producto_id      BIGINT UNSIGNED NOT NULL,
  cantidad         INT UNSIGNED    NOT NULL,          -- siempre positiva; el tipo define el signo
  precio_unitario  DECIMAL(10,2)   NOT NULL,          -- precio vigente al momento del movimiento
  PRIMARY KEY (id),
  UNIQUE KEY uq_detalle_mov_prod (movimiento_id, producto_id),
  KEY idx_detalle_producto (producto_id),
  CONSTRAINT fk_det_movimiento FOREIGN KEY (movimiento_id)
    REFERENCES movimientos (id),
  CONSTRAINT fk_det_producto FOREIGN KEY (producto_id)
    REFERENCES productos (id),
  CONSTRAINT ck_det_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_det_precio   CHECK (precio_unitario >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. DATOS INICIALES (sin usuarios ni contrasenas)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO categorias (nombre) VALUES
  ('Abarrotes'), ('Bebidas'), ('Lácteos'), ('Limpieza'), ('Snacks'), ('Otros');

-- ---------------------------------------------------------------------
-- NOTAS DE DISENO
-- * El stock_actual se actualiza en el servidor, dentro de una transaccion,
--   al recibir cada movimiento. Siempre se puede recalcular con:
--     SELECT d.producto_id,
--            SUM(CASE WHEN m.tipo IN ('ENTRADA','AJUSTE_POS') THEN d.cantidad
--                     ELSE -d.cantidad END) AS stock_calculado
--     FROM detalle_movimientos d
--     JOIN movimientos m ON m.id = d.movimiento_id
--     GROUP BY d.producto_id;
-- * Los movimientos no se editan ni se borran: se corrigen con un
--   movimiento contrario (por ejemplo, AJUSTE_NEG para anular una entrada).
-- * Los registros del catalogo no se borran; se desactivan con activo = 0.
-- * El usuario administrador inicial NO se crea aqui (nunca hay contrasenas en este
--   archivo). Se crea de dos formas: registrando la primera cuenta desde la app movil
--   (api/register.php la deja como ADMIN) o ejecutando portal/crear_admin.php una sola vez.
-- * El servidor guarda la contrasena con password_hash() de PHP (bcrypt) y los tokens de
--   acceso como hash SHA-256 (CHAR(64)); el token en claro solo lo conoce el celular.
-- * Si el hosting no permite CREATE DATABASE, usa bodega_hosting.sql (sin esas dos lineas).
-- =====================================================================
