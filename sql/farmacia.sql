-- ============================================================
-- BASE DE DATOS: farmacia
-- Este script parte EXACTAMENTE del archivo "Farmacia (1).sql"
-- que se entregó como parte de los recursos de elaboración.
-- No se eliminó ni se renombró ninguna tabla ni columna original.
--
-- Se agregaron únicamente las columnas que los Requerimientos
-- Funcionales (RF03 y RF10) y las Historias de Usuario (3 y 11)
-- exigen pero que no existían en el script original. Cada columna
-- añadida tiene un comentario "-- AÑADIDO:" indicando qué
-- requerimiento la pide y por qué era necesaria.
-- ============================================================

drop database if exists farmacia;
create database farmacia;
use farmacia;

create table if not exists medicamento(
idmedicamento int primary key auto_increment not null,
nombre_comercial varchar(100) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
nombre_sustancia varchar(100) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
presentacion varchar(50) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
fecha_caducidad date not null,
fecha_entrada date not null,
-- AÑADIDO (RF03 / Historia 3): "Lote" y "Concentración" son campos
-- obligatorios del formulario de registro de medicamento y no existían
-- en el script original.
numero_lote varchar(30) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
concentracion varchar(30) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
-- AÑADIDO (RF10 / Historia 11): la clase abstracta ProductoFarmaceutico
-- exige un atributo "precio base" (validado > 0) y un atributo "stock"
-- (validado >= 0) que tampoco existían en la tabla original.
precio_base decimal(10,2) not null,
stock int not null default 0,
-- AÑADIDO (RF10 / Historia 11): permite reconstruir en Java la subclase
-- correcta (MedicamentoLibre o MedicamentoControlado) al leer un
-- registro desde la base de datos, para aplicar el polimorfismo.
tipo_medicamento varchar(20) character set utf8mb4 collate utf8mb4_spanish2_ci not null
) engine=innodb;

create table if not exists venta(
idventa int primary key auto_increment not null,
productos_venta varchar(150) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
cantidadventa int not null,
fechaventa date not null,
horaventa time not null,
-- AÑADIDO (RF10 / Historia 11): al despachar un MedicamentoControlado
-- el sistema debe validar la cédula del médico que lo prescribe. Es
-- un dato propio de la transacción de salida, por eso vive en "venta"
-- y no en "medicamento" (nulo para medicamentos libres).
cedula_medico varchar(20) character set utf8mb4 collate utf8mb4_spanish2_ci null
) engine=innodb;

create table if not exists usuario(
id_usuario int primary key auto_increment not null,
nombre varchar(50) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
apellidos varchar(80) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
contrasenia varchar(40) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
telefono varchar(15) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
tipo varchar(30) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
id_ventas int not null,
foreign key (id_ventas) references venta(idventa) on delete restrict on update cascade
) engine=innodb;

create table if not exists detalles(
id_detalles int primary key auto_increment not null,
fecha_salida date not null,
fecha_entrada date not null,
usuario varchar(50) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
medicamento varchar(100) character set utf8mb4 collate utf8mb4_spanish2_ci not null,
cantidad int not null,
-- AÑADIDO (Historia 9 - Bitácora de Movimientos): la tabla de bitácora debe
-- mostrar obligatoriamente la "Acción (Entrada/Salida)" de cada movimiento,
-- y el script original no tenía ninguna columna para distinguirlo.
tipo_movimiento varchar(10) character set utf8mb4 collate utf8mb4_spanish2_ci not null
) engine=innodb;

create table if not exists registra(
id_usuario int not null,
id_medicamento int not null,
foreign key (id_usuario) references usuario(id_usuario) on delete restrict on update cascade,
foreign key (id_medicamento) references medicamento(idmedicamento) on delete restrict on update cascade
) engine=innodb;

create table if not exists agrega(
id_medicamento int not null,
id_detalles int not null,
foreign key (id_medicamento) references medicamento(idmedicamento) on delete restrict on update cascade,
foreign key (id_detalles) references detalles(id_detalles) on delete restrict on update cascade
) engine=innodb;

create table if not exists tiene(
id_ventas int not null,
id_detalles int not null,
foreign key (id_ventas) references venta(idventa) on delete restrict on update cascade,
foreign key (id_detalles) references detalles(id_detalles) on delete restrict on update cascade
) engine=innodb;

create table if not exists gestion_personal(
id_administrador int not null,
id_empleados int not null,
foreign key (id_administrador) references usuario(id_usuario) on delete restrict on update cascade,
foreign key (id_empleados) references usuario(id_usuario) on delete restrict on update cascade
) engine=innodb;

-- ============================================================
-- AÑADIDO: fila "placeholder" en venta.
-- La tabla usuario exige id_ventas NOT NULL (todo usuario debe
-- apuntar a una venta), pero un usuario recién creado normalmente
-- no ha registrado ninguna venta todavía. Para no violar la
-- restricción de la llave foránea, se crea una venta "vacía" con
-- id = 1 a la que apuntan los usuarios nuevos hasta que registran
-- su primera salida real. UsuarioDAO.java hace referencia a este
-- mismo id (VENTA_PLACEHOLDER = 1).
-- ============================================================
insert into venta (productos_venta, cantidadventa, fechaventa, horaventa)
values ('SIN VENTA ASOCIADA (REGISTRO PLACEHOLDER)', 0, curdate(), curtime());

-- ============================================================
-- AÑADIDO: usuario administrador inicial.
-- Sin al menos un Administrador ya creado en la base de datos,
-- nadie podría iniciar sesión para crear al primer usuario
-- (RF08 exige que solo un Administrador cree usuarios).
-- Usuario: admin   Contraseña: Admin123!
-- ============================================================
insert into usuario (nombre, apellidos, contrasenia, telefono, tipo, id_ventas)
values ('admin', 'Administrador General', 'Admin123!', '0000000000', 'Administrador', 1);
