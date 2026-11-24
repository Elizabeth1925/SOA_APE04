-- Esquema base reproducible de MySQL para APE 04 (sin datos personales ni credenciales reales)
-- Las tablas compradores y ventas se crean mediante Alembic en ventas-api (alembic upgrade head)

CREATE DATABASE IF NOT EXISTS `venta_vehiculos`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `venta_vehiculos`;

CREATE TABLE IF NOT EXISTS `usuarios_api` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `vehiculos` (
  `id_vehiculo` int(11) NOT NULL AUTO_INCREMENT,
  `marca` varchar(50) NOT NULL,
  `modelo` varchar(50) NOT NULL,
  `placa` varchar(10) NOT NULL,
  `chasis` varchar(17) NOT NULL,
  `anio` int(11) NOT NULL,
  `color` varchar(30) NOT NULL,
  `precio` decimal(12,2) NOT NULL,
  `estado` varchar(255) NOT NULL,
  PRIMARY KEY (`id_vehiculo`),
  UNIQUE KEY `placa` (`placa`),
  UNIQUE KEY `chasis` (`chasis`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `reservas` (
  `id` varchar(36) NOT NULL,
  `operacion_id` varchar(64) NOT NULL,
  `vehiculo_id` int(11) NOT NULL,
  `precio` decimal(12,2) NOT NULL,
  `moneda` varchar(3) NOT NULL,
  `estado` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `operacion_id` (`operacion_id`),
  KEY `fk_reserva_vehiculos` (`vehiculo_id`),
  CONSTRAINT `fk_reserva_vehiculos` FOREIGN KEY (`vehiculo_id`) REFERENCES `vehiculos` (`id_vehiculo`),
  CONSTRAINT `ck_reserva_estado` CHECK (`estado` in ('RESERVADA','CONFIRMADA','LIBERADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
