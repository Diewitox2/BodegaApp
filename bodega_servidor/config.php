<?php
// =====================================================================
// CONFIGURACION - cambia estos datos por los de tu hosting
// (panel del hosting > MySQL Databases / phpMyAdmin)
// =====================================================================
   const DB_HOST = 'localhost';
   const DB_NAME = 'bodega';
   const DB_USER = 'TU_USUARIO';
   const DB_PASS = 'TU_CONTRASEÑA';

// true  = la app movil puede crear cuentas VENDEDOR (la primera cuenta siempre es ADMIN).
// false = solo el administrador crea usuarios desde el portal.
const PERMITIR_REGISTRO = true;

// Dias que dura el token de acceso de un celular.
const TOKEN_DIAS = 30;

date_default_timezone_set('America/Lima');
