package clinicamed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

/** Login: paciente e funcionário entram com CPF; médico entra com CRM. Senha guardada em SHA-256. */
public class Auth {

    public static String hash(String texto) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String digitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /**
     * perfil: PACIENTE, MEDICO ou FUNCIONARIO. Retorna null se login/senha não conferem.
     */
    public static Sessao autenticar(String perfil, String identificador, String senha) throws SQLException {
        String tabela, pk, cond, valor;
        switch (perfil) {
            case "PACIENTE" -> {
                tabela = "pacientes";
                pk = "id_pacientes";
                cond = "REPLACE(REPLACE(cpf,'.',''),'-','')=?";
                valor = digitos(identificador);
            }
            case "MEDICO" -> {
                tabela = "medicos";
                pk = "id_medicos";
                cond = "UPPER(crm)=UPPER(?)";
                valor = identificador.trim();
            }
            default -> {
                tabela = "funcionarios";
                pk = "id_funcionarios";
                cond = "REPLACE(REPLACE(cpf,'.',''),'-','')=?";
                valor = digitos(identificador);
            }
        }
        String sql = "SELECT " + pk + ", nome FROM " + tabela + " WHERE " + cond + " AND senha=?";
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, valor);
            p.setString(2, hash(senha));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Sessao(r.getLong(1), r.getString(2), perfil) : null;
            }
        }
    }
}