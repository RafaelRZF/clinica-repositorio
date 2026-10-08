create database ClinicaMed;
use ClinicaMed;

create table pacientes(
id_pacientes int auto_increment primary key,
nome varchar(200) not null,
cpf varchar(18) not null, 
endereco text,
email text,
telefone varchar(20) not null,
senha varchar(250) not null);

create table medicos(
id_medicos int auto_increment primary key,
nome varchar(200) not null,
especialidade varchar(100) not null,
crm varchar(10) not null,
senha varchar(250) not null);

create table funcionarios(
id_funcionarios int auto_increment primary key,
nome varchar(200) not null,
cpf varchar(18) not null, 
endereco text,
email text,
telefone varchar(20) not null,
senha varchar(250) not null);

create table consultas(
id_consultas int auto_increment primary key,
exame varchar(100) not null,
dataHora datetime not null,
status enum('agendada', 'pendente', 'cancelada'),
id_pacientes int not null,
id_medicos int not null,
foreign key (id_pacientes) references pacientes(id_pacientes),
foreign key (id_medicos) references medicos(id_medicos)
);

create table prontuarios(
id_prontuarios int auto_increment primary key,
descricao TEXT NOT NULL,
data_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
id_pacientes int not null,
id_consultas int not null,
foreign key (id_pacientes) references pacientes(id_pacientes),
foreign key (id_consultas) references consultas(id_consultas)
);

create table pagamentos(
id_pagamentos int auto_increment primary key,
valor decimal(8,2) not null,
dataPagamento datetime not null,
formaPagamento enum ('cartaoCredito', 'cartaoDebito', 'pix', 'dinheiro'),
id_pacientes int not null,
id_consultas int not null,
foreign key (id_pacientes) references pacientes(id_pacientes),
foreign key (id_consultas) references consultas(id_consultas)
);

-- ---------------------------------------------------------------
-- Acesso inicial (sem ele ninguém consegue logar para cadastrar os demais).
-- Login de funcionário: CPF 000.000.000-00 | senha: admin123
-- (a senha é gravada como SHA-256, igual ao que o sistema Java faz)
-- ---------------------------------------------------------------
insert into funcionarios (nome, cpf, telefone, senha)
values ('Administrador', '000.000.000-00', '00000000000', SHA2('admin123', 256));
