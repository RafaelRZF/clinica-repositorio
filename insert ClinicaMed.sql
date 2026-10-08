USE ClinicaMed;

-- 1. Cadastrar um Paciente
INSERT INTO pacientes (nome, cpf, endereco, email, telefone, senha)
VALUES ('João Silva', '123.456.789-00', 'Rua A, 100', 'joao@email.com', '11999998888', 'senha123');

-- 2. Cadastrar um Médico
INSERT INTO medicos (nome, especialidade, crm, email, senha)
VALUES ('Dra. Maria Souza', 'Cardiologia', 'CRM/SP 123456', 'maria@email.com', 'senha123');

-- 3. Cadastrar um Funcionário
INSERT INTO funcionarios (nome, cpf, endereco, email, telefone, senha)
VALUES ('Carlos Oliveira', '987.654.321-11', 'Rua B, 200', 'carlos@email.com', '11977776666', 'senha123');

-- 4. Agendar uma Consulta (usando ID 1 do paciente e ID 1 do médico criados acima)
INSERT INTO consultas (exame, dataHora, status, id_pacientes, id_medicos)
VALUES ('Eletrocardiograma', '2026-10-15 14:00:00', 'agendada', 1, 1);

-- 5. Registrar Prontuário (referenciando o paciente 1 e a consulta 1)
INSERT INTO prontuarios (descricao, data_registro, id_pacientes, id_consultas)
VALUES ('Paciente relata dores no peito. Solicitado exames complementares.', NOW(), 1, 1);

-- 6. Registrar Pagamento (referenciando o paciente 1 e a consulta 1)
INSERT INTO pagamentos (valor, dataPagamento, formaPagamento, id_pacientes, id_consultas)
VALUES (250.00, NOW(), 'pix', 1, 1);

