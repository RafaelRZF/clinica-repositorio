package clinicamed;

import java.sql.*;
import java.util.*;

/** CRUD genérico por tabela (JDBC + PreparedStatement). Nomes de tabela/coluna vêm só do código, nunca do usuário. */
public class Crud {
    public final String tabela, pk;
    public final String[] campos, rotulos;
    public final Set<String> obrigatorios;

    public Crud(String tabela, String pk, String[] campos, String[] rotulos, String... obrigatorios) {
        this.tabela = tabela; this.pk = pk; this.campos = campos; this.rotulos = rotulos;
        this.obrigatorios = new HashSet<>(Arrays.asList(obrigatorios));
    }

    /** CREATE - ignora valores em branco (o banco aplica DEFAULT/NULL). Retorna o id gerado. */
    public long criar(Map<String, String> v) throws SQLException {
        Map<String, String> dados = new LinkedHashMap<>();
        v.forEach((k, x) -> { if (x != null && !x.isBlank()) dados.put(k, x); });
        String sql = "INSERT INTO " + tabela + " (" + String.join(",", dados.keySet()) + ") VALUES ("
                + String.join(",", Collections.nCopies(dados.size(), "?")) + ")";
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            for (String x : dados.values()) p.setString(i++, x);
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        }
    }

    /** READ - where opcional, ex.: "id_medicos=?". */
    public List<Map<String, String>> listar(String where, Object... args) throws SQLException {
        String sql = "SELECT * FROM " + tabela + (where == null ? "" : " WHERE " + where) + " ORDER BY " + pk;
        List<Map<String, String>> lista = new ArrayList<>();
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            try (ResultSet r = p.executeQuery()) {
                ResultSetMetaData m = r.getMetaData();
                while (r.next()) {
                    Map<String, String> linha = new LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) linha.put(m.getColumnLabel(i), r.getString(i));
                    lista.add(linha);
                }
            }
        }
        return lista;
    }

    public Map<String, String> buscar(long id) throws SQLException {
        List<Map<String, String>> l = listar(pk + "=?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    /** UPDATE */
    public boolean atualizar(long id, Map<String, String> v) throws SQLException {
        StringJoiner set = new StringJoiner(",");
        for (String k : v.keySet()) set.add(k + "=?");
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement("UPDATE " + tabela + " SET " + set + " WHERE " + pk + "=?")) {
            int i = 1;
            for (String x : v.values()) p.setString(i++, x == null || x.isBlank() ? null : x);
            p.setLong(i, id);
            return p.executeUpdate() > 0;
        }
    }

    /** DELETE */
    public boolean excluir(long id) throws SQLException {
        try (Connection c = Conexao.get(); PreparedStatement p = c.prepareStatement("DELETE FROM " + tabela + " WHERE " + pk + "=?")) {
            p.setLong(1, id);
            return p.executeUpdate() > 0;
        }
    }
}
