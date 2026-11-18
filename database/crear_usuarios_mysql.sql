-- Script de creación de usuarios para MySQL / MariaDB (XAMPP)
CREATE USER IF NOT EXISTS 'consulta'@'localhost' IDENTIFIED BY 'consulta123';
CREATE USER IF NOT EXISTS 'operador'@'localhost' IDENTIFIED BY 'operador123';
CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'admin123';

GRANT ALL PRIVILEGES ON venta_vehiculos.* TO 'consulta'@'localhost';
GRANT ALL PRIVILEGES ON venta_vehiculos.* TO 'operador'@'localhost';
GRANT ALL PRIVILEGES ON venta_vehiculos.* TO 'admin'@'localhost';

FLUSH PRIVILEGES;
