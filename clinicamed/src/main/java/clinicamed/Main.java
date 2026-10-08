package clinicamed;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

public class Main {
    static final Scanner in = new Scanner(System.in);

    // Um Crud por tabela do banco ClinicaMed (campos com * no prompt são obrigatórios)
    static final Crud PACIENTES = new Crud("pacientes", "id_pacientes",
            new String[]{"nome", "cpf", "endereco", "email", "telefone", "senha"},
            new String[]{"Nome", "CPF", "Endereço", "E-mail", "Telefone", "Senha"},
            "nome", "cpf", "telefone", "senha");
    static final Crud MEDICOS = new Crud("medicos", "id_medicos",
            new String[]{"nome", "especialidade", "crm", "senha"},
            new String[]{"Nome", "Especialidade", "CRM", "Senha"},
            "nome", "especialidade", "crm", "senha");
    static final Crud FUNCIONARIOS = new Crud("funcionarios", "id_funcionarios",
            new String[]{"nome", "cpf", "endereco", "email", "telefone", "senha"},
            new String[]{"Nome", "CPF", "Endereço", "E-mail", "Telefone", "Senha"},
            "nome", "cpf", "telefone", "senha");
    static final Crud CONSULTAS = new Crud("consultas", "id_consultas",
            new String[]{"exame", "dataHora", "status", "id_pacientes", "id_medicos"},
            new String[]{"Exame/tipo", "Data/hora (AAAA-MM-DD HH:MM)", "Status (agendada/pendente/cancelada; vazio = pendente)",
                    "ID do paciente", "ID do médico"},
            "exame", "dataHora", "id_pacientes", "id_medicos");
    static final Crud PRONTUARIOS = new Crud("prontuarios", "id_prontuarios",
            new String[]{"descricao", "id_pacientes", "id_consultas"},
            new String[]{"Descrição", "ID do paciente", "ID da consulta"},
            "descricao", "id_pacientes", "id_consultas");
    static final Crud PAGAMENTOS = new Crud("pagamentos", "id_pagamentos",
            new String[]{"valor", "dataPagamento", "formaPagamento", "id_pacientes", "id_consultas"},
            new String[]{"Valor (ex.: 150,00)", "Data do pagamento (AAAA-MM-DD HH:MM)",
                    "Forma (cartaoCredito/cartaoDebito/pix/dinheiro)", "ID do paciente", "ID da consulta"},
            "valor", "dataPagamento", "id_pacientes", "id_consultas");

    static void login() throws SQLException {
        System.out.print("Entrar como: 1-Paciente (CPF)  2-Médico (CRM)  3-Funcionário (CPF): ");
        String t = in.nextLine().trim();
        String perfil = t.equals("1") ? "PACIENTE" : t.equals("2") ? "MEDICO" : t.equals("3") ? "FUNCIONARIO" : null;
        if (perfil == null) { System.out.println("Opção inválida."); return; }
        Sessao s = Auth.autenticar(perfil, ler(perfil.equals("MEDICO") ? "CRM" : "CPF"), ler("Senha"));
        if (s == null) { System.out.println("Acesso negado: dados inválidos."); return; }
        System.out.println("\nBem-vindo(a), " + s.nome() + " [" + s.perfil() + "]");
        switch (s.perfil()) {
            case "FUNCIONARIO" -> menuFuncionario();
            case "MEDICO" -> menuMedico(s.id());
            case "PACIENTE" -> menuPaciente(s.id());
        }
    }

    /** Quem ainda não tem acesso se cadastra como paciente (CPF + senha entre os dados). */
    static void autoCadastroPaciente() throws SQLException {
        Map<String, String> v = lerCampos(PACIENTES, null);
        String erro = validar(PACIENTES, v, null);
        if (erro != null) { System.out.println(erro); return; }
        PACIENTES.criar(v);
        System.out.println("Cadastro concluído! Entre com seu CPF e senha.");
    }

    // ---------------------------------------------------------------- perfis
    static void menuFuncionario() {
        while (true) {
            System.out.print("\n[FUNCIONÁRIO] 1-Pacientes 2-Médicos 3-Funcionários 4-Consultas 5-Prontuários 6-Pagamentos 0-Sair: ");
            switch (in.nextLine().trim()) {
                case "1" -> menuCrud("Pacientes", PACIENTES);
                case "2" -> menuCrud("Médicos", MEDICOS);
                case "3" -> menuCrud("Funcionários", FUNCIONARIOS);
                case "4" -> menuCrud("Consultas", CONSULTAS);
                case "5" -> menuCrud("Prontuários", PRONTUARIOS);
                case "6" -> menuCrud("Pagamentos", PAGAMENTOS);
                case "0" -> { return; }
            }
        }
    }

    static void menuMedico(long medicoId) {
        while (true) {
            System.out.print("\n[MÉDICO] 1-Pacientes 2-Minhas consultas 3-Confirmar/cancelar consulta 4-Prontuários 0-Sair: ");
            switch (in.nextLine().trim()) {
                case "1" -> executar(() -> imprimir(PACIENTES.listar(null)));
                case "2" -> executar(() -> imprimir(CONSULTAS.listar("id_medicos=?", medicoId)));
                case "3" -> executar(() -> {
                    long id = Long.parseLong(ler("ID da consulta"));
                    if (consultaDoMedico(id, medicoId) == null) { System.out.println("Consulta não encontrada ou não é sua."); return; }
                    String novo = ler("Novo status (agendada/cancelada)");
                    if (!novo.equals("agendada") && !novo.equals("cancelada")) { System.out.println("Status inválido."); return; }
                    CONSULTAS.atualizar(id, Map.of("status", novo));
                    System.out.println("Consulta atualizada.");
                });
                case "4" -> menuProntuariosMedico(medicoId);
                case "0" -> { return; }
            }
        }
    }

    static Map<String, String> consultaDoMedico(long consultaId, long medicoId) throws SQLException {
        Map<String, String> c = CONSULTAS.buscar(consultaId);
        return c != null && String.valueOf(medicoId).equals(c.get("id_medicos")) ? c : null;
    }

    static Map<String, String> prontuarioDoMedico(long id, long medicoId) throws SQLException {
        Map<String, String> p = PRONTUARIOS.buscar(id);
        return p != null && consultaDoMedico(Long.parseLong(p.get("id_consultas")), medicoId) != null ? p : null;
    }

    static void menuProntuariosMedico(long medicoId) {
        while (true) {
            System.out.print("\n[PRONTUÁRIOS] 1-Novo 2-Listar os meus 3-Atualizar 4-Excluir 0-Voltar: ");
            String op = in.nextLine().trim();
            if (op.equals("0")) return;
            executar(() -> {
                switch (op) {
                    case "1" -> {
                        Map<String, String> c = consultaDoMedico(Long.parseLong(ler("ID da consulta")), medicoId);
                        if (c == null) { System.out.println("Consulta não encontrada ou não é sua."); return; }
                        Map<String, String> v = new LinkedHashMap<>();
                        v.put("descricao", ler("Descrição"));
                        v.put("id_pacientes", c.get("id_pacientes"));
                        v.put("id_consultas", c.get("id_consultas"));
                        System.out.println("Prontuário criado, id=" + PRONTUARIOS.criar(v));
                    }
                    case "2" -> imprimir(PRONTUARIOS.listar(
                            "id_consultas IN (SELECT id_consultas FROM consultas WHERE id_medicos=?)", medicoId));
                    case "3", "4" -> {
                        long id = Long.parseLong(ler("ID do prontuário"));
                        Map<String, String> p = prontuarioDoMedico(id, medicoId);
                        if (p == null) { System.out.println("Prontuário não encontrado ou não é seu."); return; }
                        if (op.equals("4")) { PRONTUARIOS.excluir(id); System.out.println("Excluído."); return; }
                        String novo = ler("Descrição [" + p.get("descricao") + "] (Enter mantém)");
                        PRONTUARIOS.atualizar(id, Map.of("descricao", novo.isBlank() ? p.get("descricao") : novo));
                        System.out.println("Atualizado.");
                    }
                }
            });
        }
    }

    static void menuPaciente(long pacienteId) {
        while (true) {
            System.out.print("\n[PACIENTE] 1-Meus dados 2-Agendar consulta 3-Minhas consultas 4-Cancelar consulta 5-Meus prontuários 6-Meus pagamentos 0-Sair: ");
            switch (in.nextLine().trim()) {
                case "1" -> executar(() -> imprimir(PACIENTES.listar("id_pacientes=?", pacienteId)));
                case "2" -> executar(() -> {
                    imprimir(MEDICOS.listar(null));
                    Map<String, String> v = new LinkedHashMap<>();
                    v.put("id_pacientes", String.valueOf(pacienteId));
                    v.put("id_medicos", ler("ID do médico"));
                    v.put("exame", ler("Exame/tipo de consulta"));
                    v.put("dataHora", normalizaData(ler("Data/hora (AAAA-MM-DD HH:MM)")));
                    v.put("status", "pendente");   // a clínica confirma depois
                    String erro = validar(CONSULTAS, v, null);
                    if (erro != null) { System.out.println(erro); return; }
                    System.out.println("Consulta solicitada (pendente), id=" + CONSULTAS.criar(v));
                });
                case "3" -> executar(() -> imprimir(CONSULTAS.listar("id_pacientes=?", pacienteId)));
                case "4" -> executar(() -> {
                    long id = Long.parseLong(ler("ID da consulta"));
                    Map<String, String> c = CONSULTAS.buscar(id);
                    if (c == null || !String.valueOf(pacienteId).equals(c.get("id_pacientes"))) { System.out.println("Consulta não é sua."); return; }
                    CONSULTAS.atualizar(id, Map.of("status", "cancelada"));
                    System.out.println("Consulta cancelada.");
                });
                case "5" -> executar(() -> imprimir(PRONTUARIOS.listar("id_pacientes=?", pacienteId)));
                case "6" -> executar(() -> imprimir(PAGAMENTOS.listar("id_pacientes=?", pacienteId)));
                case "0" -> { return; }
            }
        }
    }

    // ---------------------------------------------------------------- CRUD genérico
    static void menuCrud(String titulo, Crud crud) {
        while (true) {
            System.out.print("\n[" + titulo.toUpperCase() + "] 1-Cadastrar 2-Listar 3-Buscar por ID 4-Atualizar 5-Excluir 0-Voltar: ");
            String op = in.nextLine().trim();
            if (op.equals("0")) return;
            executar(() -> {
                switch (op) {
                    case "1" -> {
                        Map<String, String> v = lerCampos(crud, null);
                        if (crud == CONSULTAS && v.get("status").isBlank()) v.put("status", "pendente");
                        String erro = validar(crud, v, null);
                        if (erro != null) { System.out.println(erro); return; }
                        System.out.println("Cadastrado com id=" + crud.criar(v));
                    }
                    case "2" -> imprimir(crud.listar(null));
                    case "3" -> imprimir(crud.listar(crud.pk + "=?", Long.parseLong(ler("ID"))));
                    case "4" -> {
                        long id = Long.parseLong(ler("ID"));
                        Map<String, String> atual = crud.buscar(id);
                        if (atual == null) { System.out.println("Não encontrado."); return; }
                        Map<String, String> v = lerCampos(crud, atual);
                        String erro = validar(crud, v, atual);
                        if (erro != null) { System.out.println(erro); return; }
                        System.out.println(crud.atualizar(id, v) ? "Atualizado." : "Nada alterado.");
                    }
                    case "5" -> {
                        long id = Long.parseLong(ler("ID"));
                        if (crud.buscar(id) == null) { System.out.println("Não encontrado."); return; }
                        if (!ler("Confirma exclusão? (s/n)").equalsIgnoreCase("s")) return;
                        crud.excluir(id);
                        System.out.println("Excluído.");
                    }
                }
            });
        }
    }

    /** Regras de negócio antes de gravar. Retorna a mensagem de erro, ou null se estiver tudo certo. */
    static String validar(Crud crud, Map<String, String> v, Map<String, String> atual) throws SQLException {
        long idAtual = atual == null ? 0 : Long.parseLong(atual.get(crud.pk));
        if (v.containsKey("cpf")
                && (atual == null || !Auth.digitos(v.get("cpf")).equals(Auth.digitos(atual.get("cpf"))))
                && Regras.cpfExiste(crud.tabela, v.get("cpf")))
            return "Já existe cadastro com esse CPF.";
        if (v.containsKey("crm")
                && (atual == null || !v.get("crm").equalsIgnoreCase(atual.get("crm")))
                && Regras.crmExiste(v.get("crm")))
            return "Já existe médico com esse CRM.";
        if (crud == CONSULTAS && !"cancelada".equals(v.get("status"))
                && Regras.horarioOcupado(v.get("id_medicos"), v.get("dataHora"), idAtual))
            return "Esse médico já tem consulta nesse horário.";
        if (v.containsKey("id_consultas") && v.containsKey("id_pacientes")) {
            String dono = Regras.pacienteDaConsulta(Long.parseLong(v.get("id_consultas")));
            if (dono == null) return "Consulta não encontrada.";
            if (!dono.equals(v.get("id_pacientes"))) return "Essa consulta não pertence a esse paciente.";
        }
        return null;
    }

    // ---------------------------------------------------------------- utilitários
    interface Acao { void run() throws SQLException; }

    static void executar(Acao a) {
        try { a.run(); }
        catch (NumberFormatException e) { System.out.println("Valor numérico inválido."); }
        catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Operação bloqueada: ID inexistente ou registro com vínculos (consultas, prontuários, pagamentos).");
        }
        catch (SQLException e) { System.out.println("Erro: " + e.getMessage()); }
    }

    static String ler(String rotulo) {
        System.out.print(rotulo + ": ");
        return in.nextLine().trim();
    }

    static String normalizaData(String s) {
        return s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}") ? s + ":00" : s;
    }

    /** Cadastro (atual == null): campos obrigatórios são repetidos até preencher. Atualização: Enter mantém o valor. */
    static Map<String, String> lerCampos(Crud crud, Map<String, String> atual) {
        Map<String, String> v = new LinkedHashMap<>();
        for (int i = 0; i < crud.campos.length; i++) {
            String campo = crud.campos[i], rot = crud.rotulos[i];
            boolean obrig = crud.obrigatorios.contains(campo);
            String atualVal = atual == null ? null : atual.get(campo);
            String valor;
            if (campo.equals("senha")) {
                if (atual == null) {
                    String s;
                    do { s = ler(rot + " *"); } while (s.isBlank());
                    valor = Auth.hash(s);
                } else {
                    String n = ler("Nova senha (Enter mantém)");
                    valor = n.isBlank() ? atualVal : Auth.hash(n);
                }
            } else if (atual == null) {
                do { valor = ler(rot + (obrig ? " *" : "")); } while (obrig && valor.isBlank());
            } else {
                String n = ler(rot + " [" + atualVal + "] (Enter mantém)");
                valor = n.isBlank() ? atualVal : n;
            }
            if (valor != null) {
                if (campo.equals("dataHora") || campo.equals("dataPagamento")) valor = normalizaData(valor);
                if (campo.equals("valor")) valor = valor.replace(',', '.');
            }
            v.put(campo, valor);
        }
        return v;
    }

    static void imprimir(List<Map<String, String>> linhas) {
        if (linhas.isEmpty()) { System.out.println("(nenhum registro)"); return; }
        for (Map<String, String> l : linhas) {
            StringJoiner sj = new StringJoiner(" | ");
            l.forEach((k, x) -> { if (!k.equals("senha")) sj.add(k + "=" + x); });
            System.out.println(sj);

            



        }
    }
}
