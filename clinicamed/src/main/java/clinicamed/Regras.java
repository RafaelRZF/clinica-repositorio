package clinicamed;

import java.sql.*;

/** Regras de negócio que o esquema do banco não garante sozinho (CPF/CRM únicos, horário livre, etc.). */
public class Regras {

    static boolean existe(String sql, Object... args) throws SQLException {
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            try (ResultSet r = p.executeQuery()) { return r.next(); }
        }
    }

    public static boolean cpfExiste(String tabela, String cpf) throws SQLException {
        return existe("SELECT 1 FROM " + tabela + " WHERE REPLACE(REPLACE(cpf,'.',''),'-','')=? LIMIT 1", Auth.digitos(cpf));
    }

    public static boolean crmExiste(String crm) throws SQLException {
        return existe("SELECT 1 FROM medicos WHERE UPPER(crm)=UPPER(?) LIMIT 1", crm.trim());
    }

    /** Médico já tem consulta (não cancelada) nesse horário? ignorarId = consulta que está sendo editada (0 se nova). */
    public static boolean horarioOcupado(String medicoId, String dataHora, long ignorarId) throws SQLException {
        return existe("SELECT 1 FROM consultas WHERE id_medicos=? AND dataHora=? AND id_consultas<>? "
                + "AND (status IS NULL OR status<>'cancelada') LIMIT 1", Long.parseLong(medicoId), dataHora, ignorarId);
    }

    /** id do paciente dono da consulta, ou null se a consulta não existe. */
    public static String pacienteDaConsulta(long consultaId) throws SQLException {
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement("SELECT id_pacientes FROM consultas WHERE id_consultas=?")) {
            p.setLong(1, consultaId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }
}
