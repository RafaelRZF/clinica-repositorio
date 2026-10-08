package clinicamed;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/app")
public class PaginaPrincipal {

    // um Crud por tabela (aqui só usamos o nome da tabela e da chave primária)
    Crud pacientes = new Crud("pacientes", "id_pacientes", new String[0], new String[0]);
    Crud medicos = new Crud("medicos", "id_medicos", new String[0], new String[0]);
    Crud consultas = new Crud("consultas", "id_consultas", new String[0], new String[0]);
    Crud prontuarios = new Crud("prontuarios", "id_prontuarios", new String[0], new String[0]);
    Crud pagamentos = new Crud("pagamentos", "id_pagamentos", new String[0], new String[0]);

    // ---------- funções auxiliares ----------
    String limpar(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    String nome(Crud crud, String id) {
        try {
            Map<String, String> m = crud.buscar(Long.parseLong(id));
            return m == null ? id : limpar(m.get("nome"));
        } catch (Exception e) {
            return id;
        }
    }

    ResponseEntity<String> ir(String url) {
        return ResponseEntity.status(302).header("Location", url).build();
    }

    ResponseEntity<String> voltar(String aba, String erro) {
        String url = "/app?aba=" + aba;
        if (erro != null) url += "&erro=" + URLEncoder.encode(erro, StandardCharsets.UTF_8);
        return ir(url);
    }

    String tabela(List<String[]> linhas, String... colunas) {
        if (linhas.isEmpty()) return "<p>Nenhum registro.</p>";
        String t = "<table><tr>";
        for (String c : colunas) t += "<th>" + c + "</th>";
        t += "</tr>";
        for (String[] linha : linhas) {
            t += "<tr>";
            for (String c : linha) t += "<td>" + (c == null ? "" : c) + "</td>";
            t += "</tr>";
        }
        return t + "</table>";
    }

    String botao(String id, String status, String texto) {
        return "<form method='post' action='/app/status'><input type='hidden' name='id' value='" + id
                + "'><input type='hidden' name='status' value='" + status + "'><button>" + texto + "</button></form>";
    }

    String pagina(Sessao s, String conteudo) {
        String[] abas;
        if (s.perfil().equals("PACIENTE"))
            abas = new String[]{"inicio:Início", "agendar:Marcar consulta", "consultas:Minhas consultas", "prontuarios:Prontuários", "pagamentos:Pagamentos"};
        else if (s.perfil().equals("MEDICO"))
            abas = new String[]{"inicio:Início", "consultas:Consultas", "prontuarios:Prontuários"};
        else
            abas = new String[]{"inicio:Início", "consultas:Consultas", "pacientes:Pacientes", "medicos:Médicos", "prontuarios:Prontuários", "pagamentos:Pagamentos"};
        String menu = "";
        for (String a : abas) menu += "<a href='/app?aba=" + a.split(":")[0] + "'>" + a.split(":")[1] + "</a>";
        return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'><title>ClínicaMed</title>"
                + "<link rel='stylesheet' href='/estilo.css'></head><body>"
                + "<header><b>ClínicaMed</b><span>" + limpar(s.nome()) + " (" + s.perfil() + ") <a href='/api/logout'>Sair</a></span></header>"
                + "<nav>" + menu + "</nav><main>" + conteudo + "</main></body></html>";
    }

    // ---------- página principal ----------
    @GetMapping
    public ResponseEntity<String> app(@RequestParam(name = "aba", defaultValue = "inicio") String aba,
                                      @RequestParam(name = "erro", required = false) String erro, HttpSession http) {
        Sessao s = (Sessao) http.getAttribute("sessao");
        if (s == null) return ir("/index.html");
        String p = s.perfil();
        String c = "<h2>Olá, " + limpar(s.nome()) + "!</h2><p>Escolha uma opção no menu.</p>";
        try {
            if (aba.equals("agendar") && p.equals("PACIENTE")) c = formAgendar();
            else if (aba.equals("consultas")) c = listaConsultas(s);
            else if (aba.equals("prontuarios")) c = listaProntuarios(s);
            else if (aba.equals("pagamentos") && !p.equals("MEDICO")) c = listaPagamentos(s);
            else if (aba.equals("pacientes") && p.equals("FUNCIONARIO")) c = listaPacientes();
            else if (aba.equals("medicos") && p.equals("FUNCIONARIO")) c = listaMedicos();
        } catch (Exception e) {
            e.printStackTrace();
            c = "<p class='erro'>Erro ao acessar o banco de dados.</p>";
        }
        if (erro != null) c = "<p class='erro'>" + limpar(erro) + "</p>" + c;
        return ResponseEntity.status(200).contentType(MediaType.parseMediaType("text/html;charset=UTF-8")).body(pagina(s, c));
    }

    String formAgendar() throws Exception {
        String opcoes = "";
        for (Map<String, String> m : medicos.listar(null))
            opcoes += "<option value='" + m.get("id_medicos") + "'>" + limpar(m.get("nome")) + " - " + limpar(m.get("especialidade")) + "</option>";
        return "<h2>Marcar consulta</h2><form method='post' action='/app/agendar'>"
                + "<label>Médico</label><select name='id_medicos'>" + opcoes + "</select>"
                + "<label>Exame / tipo de consulta</label><input type='text' name='exame' required>"
                + "<label>Data e hora</label><input type='datetime-local' name='dataHora' required>"
                + "<button>Marcar</button></form>";
    }

    String listaConsultas(Sessao s) throws Exception {
        List<Map<String, String>> lista;
        if (s.perfil().equals("PACIENTE")) lista = consultas.listar("id_pacientes=?", s.id());
        else if (s.perfil().equals("MEDICO")) lista = consultas.listar("id_medicos=?", s.id());
        else lista = consultas.listar(null);

        List<String[]> linhas = new ArrayList<>();
        for (Map<String, String> c : lista) {
            String status = c.get("status");
            String acoes = "";
            if (!"cancelada".equals(status)) {
                acoes = botao(c.get("id_consultas"), "cancelada", "Cancelar");
                if (!s.perfil().equals("PACIENTE") && !"agendada".equals(status))
                    acoes = botao(c.get("id_consultas"), "agendada", "Confirmar") + acoes;
            }
            linhas.add(new String[]{c.get("dataHora"), nome(pacientes, c.get("id_pacientes")), nome(medicos, c.get("id_medicos")),
                    limpar(c.get("exame")), status, acoes});
        }
        return "<h2>Consultas</h2>" + tabela(linhas, "Data", "Paciente", "Médico", "Exame", "Status", "");
    }

    String listaProntuarios(Sessao s) throws Exception {
        List<Map<String, String>> lista;
        if (s.perfil().equals("PACIENTE")) lista = prontuarios.listar("id_pacientes=?", s.id());
        else if (s.perfil().equals("MEDICO"))
            lista = prontuarios.listar("id_consultas IN (SELECT id_consultas FROM consultas WHERE id_medicos=?)", s.id());
        else lista = prontuarios.listar(null);

        List<String[]> linhas = new ArrayList<>();
        for (Map<String, String> p : lista)
            linhas.add(new String[]{nome(pacientes, p.get("id_pacientes")), "Consulta " + p.get("id_consultas"), limpar(p.get("descricao"))});

        String form = "";
        if (s.perfil().equals("MEDICO")) {
            String opcoes = "";
            for (Map<String, String> c : consultas.listar("id_medicos=?", s.id()))
                opcoes += "<option value='" + c.get("id_consultas") + "'>" + c.get("dataHora") + " - " + nome(pacientes, c.get("id_pacientes")) + "</option>";
            form = "<h3>Novo prontuário</h3><form method='post' action='/app/prontuario'>"
                    + "<label>Consulta</label><select name='id_consultas'>" + opcoes + "</select>"
                    + "<label>Descrição</label><textarea name='descricao' required></textarea><button>Salvar</button></form>";
        }
        return "<h2>Prontuários</h2>" + form + tabela(linhas, "Paciente", "Consulta", "Descrição");
    }

    String listaPagamentos(Sessao s) throws Exception {
        List<Map<String, String>> lista;
        if (s.perfil().equals("PACIENTE")) lista = pagamentos.listar("id_pacientes=?", s.id());
        else lista = pagamentos.listar(null);

        List<String[]> linhas = new ArrayList<>();
        for (Map<String, String> p : lista)
            linhas.add(new String[]{p.get("dataPagamento"), nome(pacientes, p.get("id_pacientes")), p.get("formaPagamento"), "R$ " + p.get("valor")});

        String form = "";
        if (s.perfil().equals("FUNCIONARIO")) {
            String opcoes = "";
            for (Map<String, String> c : consultas.listar(null))
                opcoes += "<option value='" + c.get("id_consultas") + "'>" + c.get("dataHora") + " - " + nome(pacientes, c.get("id_pacientes")) + "</option>";
            form = "<h3>Registrar pagamento</h3><form method='post' action='/app/pagamento'>"
                    + "<label>Consulta</label><select name='id_consultas'>" + opcoes + "</select>"
                    + "<label>Valor (ex.: 150,00)</label><input type='text' name='valor' required>"
                    + "<label>Forma de pagamento</label><select name='formaPagamento'><option>pix</option><option>dinheiro</option>"
                    + "<option>cartaoCredito</option><option>cartaoDebito</option></select><button>Registrar</button></form>";
        }
        return "<h2>Pagamentos</h2>" + form + tabela(linhas, "Data", "Paciente", "Forma", "Valor");
    }

    String listaPacientes() throws Exception {
        List<String[]> linhas = new ArrayList<>();
        for (Map<String, String> p : pacientes.listar(null))
            linhas.add(new String[]{limpar(p.get("nome")), limpar(p.get("cpf")), limpar(p.get("telefone")), limpar(p.get("email"))});
        return "<h2>Pacientes</h2>" + tabela(linhas, "Nome", "CPF", "Telefone", "E-mail");
    }

    String listaMedicos() throws Exception {
        List<String[]> linhas = new ArrayList<>();
        for (Map<String, String> m : medicos.listar(null))
            linhas.add(new String[]{limpar(m.get("nome")), limpar(m.get("especialidade")), limpar(m.get("crm"))});
        return "<h2>Médicos</h2>" + tabela(linhas, "Nome", "Especialidade", "CRM");
    }

    // ---------- ações (formulários) ----------
    @PostMapping("/agendar")
    public ResponseEntity<String> agendar(@RequestParam("id_medicos") String medico, @RequestParam("exame") String exame,
                                          @RequestParam("dataHora") String dataHora, HttpSession http) {
        Sessao s = (Sessao) http.getAttribute("sessao");
        if (s == null || !s.perfil().equals("PACIENTE")) return ir("/index.html");
        try {
            if (LocalDateTime.parse(dataHora).isBefore(LocalDateTime.now())) return voltar("agendar", "Escolha uma data futura");
            String dh = dataHora.replace("T", " ") + ":00";
            if (Regras.horarioOcupado(medico, dh, 0)) return voltar("agendar", "Esse médico já tem consulta nesse horário");
            Map<String, String> v = new LinkedHashMap<>();
            v.put("exame", exame.trim());
            v.put("dataHora", dh);
            v.put("status", "pendente");
            v.put("id_pacientes", String.valueOf(s.id()));
            v.put("id_medicos", medico);
            consultas.criar(v);
            return voltar("consultas", null);
        } catch (Exception e) {
            e.printStackTrace();
            return voltar("agendar", "Não foi possível marcar a consulta");
        }
    }

    @PostMapping("/status")
    public ResponseEntity<String> status(@RequestParam("id") String id, @RequestParam("status") String status, HttpSession http) {
        Sessao s = (Sessao) http.getAttribute("sessao");
        if (s == null) return ir("/index.html");
        try {
            Map<String, String> c = consultas.buscar(Long.parseLong(id));
            if (c == null) return voltar("consultas", "Consulta não encontrada");
            boolean pode = false;
            if (s.perfil().equals("FUNCIONARIO")) pode = true;
            if (s.perfil().equals("MEDICO") && String.valueOf(s.id()).equals(c.get("id_medicos"))) pode = true;
            if (s.perfil().equals("PACIENTE") && String.valueOf(s.id()).equals(c.get("id_pacientes")) && status.equals("cancelada")) pode = true;
            if (!pode || !(status.equals("agendada") || status.equals("cancelada"))) return voltar("consultas", "Ação não permitida");
            consultas.atualizar(Long.parseLong(id), Map.of("status", status));
            return voltar("consultas", null);
        } catch (Exception e) {
            e.printStackTrace();
            return voltar("consultas", "Erro ao atualizar a consulta");
        }
    }

    @PostMapping("/prontuario")
    public ResponseEntity<String> prontuario(@RequestParam("id_consultas") String idConsulta,
                                             @RequestParam("descricao") String descricao, HttpSession http) {
        Sessao s = (Sessao) http.getAttribute("sessao");
        if (s == null || !s.perfil().equals("MEDICO")) return ir("/index.html");
        try {
            Map<String, String> c = consultas.buscar(Long.parseLong(idConsulta));
            if (c == null || !String.valueOf(s.id()).equals(c.get("id_medicos"))) return voltar("prontuarios", "Consulta não encontrada");
            Map<String, String> v = new LinkedHashMap<>();
            v.put("descricao", descricao.trim());
            v.put("id_pacientes", c.get("id_pacientes"));
            v.put("id_consultas", idConsulta);
            prontuarios.criar(v);
            return voltar("prontuarios", null);
        } catch (Exception e) {
            e.printStackTrace();
            return voltar("prontuarios", "Erro ao salvar o prontuário");
        }
    }

    @PostMapping("/pagamento")
    public ResponseEntity<String> pagamento(@RequestParam("id_consultas") String idConsulta, @RequestParam("valor") String valor,
                                            @RequestParam("formaPagamento") String forma, HttpSession http) {
        Sessao s = (Sessao) http.getAttribute("sessao");
        if (s == null || !s.perfil().equals("FUNCIONARIO")) return ir("/index.html");
        try {
            double numero = Double.parseDouble(valor.trim().replace(",", "."));
            Map<String, String> c = consultas.buscar(Long.parseLong(idConsulta));
            if (numero <= 0 || c == null) return voltar("pagamentos", "Dados inválidos");
            Map<String, String> v = new LinkedHashMap<>();
            v.put("valor", String.format(Locale.US, "%.2f", numero));
            v.put("dataPagamento", LocalDateTime.now().toString().replace("T", " ").substring(0, 19));
            v.put("formaPagamento", forma);
            v.put("id_pacientes", c.get("id_pacientes"));
            v.put("id_consultas", idConsulta);
            pagamentos.criar(v);
            return voltar("pagamentos", null);
        } catch (Exception e) {
            e.printStackTrace();
            return voltar("pagamentos", "Valor inválido ou erro ao salvar");
        }
    }
}