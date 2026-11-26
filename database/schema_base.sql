-- ==============================================================================
-- ARCHIVO: schema_base.sql
-- COMPONENTE: Base de Datos MySQL (Esquema Reproducible Base - venta_vehiculos)
-- ROL: Define de forma idempotente el DDL base del dominio Java (Spring Boot:
--      usuarios_api, vehiculos y reservas) sin datos personales ni credenciales
--      reales, manteniendo separadas las tablas del dominio Python (compradores
--      y ventas) que son administradas mediante migraciones de Alembic.
-- ACTIVIDADES APE 04 CUBIERTAS:
--   - [Actividad 1 y Actividad 2]: Script DDL reproducible para levantar la base
--     venta_vehiculos en un entorno limpio con restricciones de integridad
--     (claves primarias, unicidad de username/placa/chasis/operacion_id, llave
--     foranea y restriccion CHECK sobre estados de reserva).
-- ==============================================================================

-- Esquema base reproducible de MySQL para APE 04 (sin datos personales ni credenciales reales)
-- Las tablas compradores y ventas se crean mediante Alembic en ventas-api (alembic upgrade head)

-- [Actividad 1 y Actividad 2] Crea la base de datos venta_vehiculos si no existe, usando UTF-8 completo (utf8mb4) y colacion Unicode insensible a mayusculas.
CREATE DATABASE IF NOT EXISTS `venta_vehiculos`
  -- Juego de caracteres utf8mb4 para soportar tildes, enies y caracteres Unicode completos.
  CHARACTER SET utf8mb4
  -- Colacion estandar Unicode para ordenamiento y comparacion consistente de textos.
  COLLATE utf8mb4_unicode_ci;

-- Selecciona la base de datos venta_vehiculos como contexto activo para crear las tablas del dominio Java.
USE `venta_vehiculos`;

-- [Actividad 1 y Actividad 2] Tabla de usuarios de autenticacion del microservicio Java (emision de JWT en /oauth/token).
CREATE TABLE IF NOT EXISTS `usuarios_api` (
  -- Clave primaria autoincremental del usuario de la API.
  `id` int(11) NOT NULL AUTO_INCREMENT,
  -- Nombre de usuario unico para inicio de sesion (maximo 50 caracteres).
  `username` varchar(50) NOT NULL,
  -- Hash seguro de la contrasena del usuario (por ejemplo BCrypt; nunca texto plano ni credenciales reales en este script).
  `password` varchar(255) NOT NULL,
  -- Rol de autorizacion asignado al usuario (por ejemplo ADMIN, VENDEDOR, CONSULTA).
  `rol` varchar(20) NOT NULL,
  -- Define la columna id como clave primaria de la tabla.
  PRIMARY KEY (`id`),
  -- Restriccion de unicidad que impide registrar dos usuarios con el mismo username.
  UNIQUE KEY `username` (`username`)
-- Motor transaccional InnoDB con soporte ACID y codificacion utf8mb4.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [Actividad 1 y Actividad 2] Tabla principal del catalogo de vehiculos administrado por las instancias Spring Boot (:8080 y :8082).
CREATE TABLE IF NOT EXISTS `vehiculos` (
  -- Clave primaria autoincremental del vehiculo.
  `id_vehiculo` int(11) NOT NULL AUTO_INCREMENT,
  -- Fabricante o marca del vehiculo (obligatorio).
  `marca` varchar(50) NOT NULL,
  -- Modelo comercial del vehiculo (obligatorio).
  `modelo` varchar(50) NOT NULL,
  -- Numero de placa vehicular; debe ser unico en todo el inventario.
  `placa` varchar(10) NOT NULL,
  -- Numero de serie o VIN/chasis de hasta 17 caracteres; debe ser unico en todo el inventario.
  `chasis` varchar(17) NOT NULL,
  -- Anio de fabricacion del vehiculo.
  `anio` int(11) NOT NULL,
  -- Color exterior registrado del vehiculo.
  `color` varchar(30) NOT NULL,
  -- Precio de venta con precision exacta de hasta 12 digitos totales y 2 decimales.
  `precio` decimal(12,2) NOT NULL,
  -- Estado comercial actual del vehiculo (por ejemplo DISPONIBLE, RESERVADO, VENDIDO).
  `estado` varchar(255) NOT NULL,
  -- Define id_vehiculo como clave primaria referenciada desde reservas.
  PRIMARY KEY (`id_vehiculo`),
  -- [Actividad 1 y Actividad 2] Garantiza unicidad estricta de la placa a nivel de base de datos.
  UNIQUE KEY `placa` (`placa`),
  -- [Actividad 1 y Actividad 2] Garantiza unicidad estricta del numero de chasis a nivel de base de datos.
  UNIQUE KEY `chasis` (`chasis`)
-- Motor transaccional InnoDB necesario para integridad referencial y bloqueos de reserva/venta.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [Actividad 1 y Actividad 2] Tabla de reservas transaccionales del dominio Java usadas durante la coordinacion de ventas con ventas-api.
CREATE TABLE IF NOT EXISTS `reservas` (
  -- Clave primaria UUID (36 caracteres) que identifica de manera unica cada reserva.
  `id` varchar(36) NOT NULL,
  -- Identificador unico de operacion distribuida (idempotencia y trazabilidad con ventas-api y notificaciones).
  `operacion_id` varchar(64) NOT NULL,
  -- Referencia obligatoria al vehiculo reservado en la tabla vehiculos.
  `vehiculo_id` int(11) NOT NULL,
  -- Precio congelado del vehiculo al momento de generar la reserva (precision decimal exacta).
  `precio` decimal(12,2) NOT NULL,
  -- Codigo ISO de moneda de 3 caracteres (por ejemplo USD).
  `moneda` varchar(3) NOT NULL,
  -- Estado del ciclo de vida de la reserva (restringido a RESERVADA, CONFIRMADA o LIBERADA).
  `estado` varchar(20) NOT NULL,
  -- Define el UUID id como clave primaria de la tabla reservas.
  PRIMARY KEY (`id`),
  -- [Actividad 1 y Actividad 2] Garantiza que cada operacion_id genere como maximo una reserva (idempotencia).
  UNIQUE KEY `operacion_id` (`operacion_id`),
  -- Indice secundario sobre vehiculo_id para optimizar uniones y validacion de llave foranea.
  KEY `fk_reserva_vehiculos` (`vehiculo_id`),
  -- Restriccion de integridad referencial que vincula cada reserva con un vehiculo existente.
  CONSTRAINT `fk_reserva_vehiculos` FOREIGN KEY (`vehiculo_id`) REFERENCES `vehiculos` (`id_vehiculo`),
  -- [Actividad 1 y Actividad 2] Restriccion CHECK que solo permite los tres estados validos del ciclo de reserva.
  CONSTRAINT `ck_reserva_estado` CHECK (`estado` in ('RESERVADA','CONFIRMADA','LIBERADA'))
-- Motor transaccional InnoDB con codificacion utf8mb4 para consistencia ACID.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
