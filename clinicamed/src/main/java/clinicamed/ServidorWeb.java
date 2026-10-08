package clinicamed;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ServidorWeb {

    private static final Set<String> PERFIS_VALIDOS = Set.of("PACIENTE", "FUNCIONARIO", "MEDICO");

    public static void main(String[] args) {
        SpringApplication.run(ServidorWeb.class, args);
    }

    private static ResponseEntity<Void> redirecionar(String destino) {
        return ResponseEntity.status(302).header("Location", destino).build();
    }

    /**
     * Recebe o <form method="post" action="/api/login"> do HTML (sem JavaScript).
     * Campos esperados: name="usuario" (CPF ou CRM), name="senha" e, opcionalmente, name="perfil"
     * (PACIENTE, MEDICO ou FUNCIONARIO). Se "perfil" não vier, tenta os três.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> processarLogin(@RequestParam("usuario") String usuario,
                                            @RequestParam("senha") String senha,
                                            @RequestParam(name = "perfil", required = false) String perfil,
                                            HttpSession http) {
        String[] tentativas;
        if (perfil != null && PERFIS_VALIDOS.contains(perfil.toUpperCase())) {
            tentativas = new String[]{perfil.toUpperCase()};
        } else {
            tentativas = new String[]{"PACIENTE", "FUNCIONARIO", "MEDICO"};
        }

        boolean erroBanco = false;
        for (String p : tentativas) {
            try {
                Sessao sessao = Auth.autenticar(p, usuario, senha);
                if (sessao != null) {
                    http.setAttribute("sessao", sessao);
                    return redirecionar("/app");
                }
            } catch (SQLException e) {
                erroBanco = true;
                System.err.println("Erro ao conectar ao banco de dados: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Diferencia "banco fora do ar" de "usuário/senha errados"
        if (erroBanco)
            return pagina(500, "Erro no servidor", "Não foi possível acessar o banco de dados. Verifique se o MySQL está ligado.",
                    "/index.html", "Voltar ao login");
        return pagina(401, "Login inválido", "CPF/CRM ou senha incorretos.", "/index.html", "Tentar novamente");
    }

    // ------------------------------------------------------------ cadastro de paciente
    private static final Crud PACIENTES = new Crud("pacientes", "id_pacientes",
            new String[]{"nome", "cpf", "endereco", "email", "telefone", "senha"},
            new String[]{"Nome", "CPF", "Endereço", "E-mail", "Telefone", "Senha"},
            "nome", "cpf", "telefone", "senha");

    private static ResponseEntity<String> pagina(int status, String titulo, String mensagem,
                                                 String link, String textoLink) {
        String html = "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titulo + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;background:#eef3f8;display:flex;"
                + "align-items:center;justify-content:center;min-height:100vh;margin:0}"
                + ".caixa{background:#fff;padding:32px;border-radius:12px;max-width:380px;text-align:center;"
                + "box-shadow:0 4px 20px rgba(0,0,0,.1)}a{display:inline-block;margin-top:16px;color:#0b6fa4}"
                + "</style></head><body><div class=\"caixa\"><h2>" + titulo + "</h2><p>" + mensagem + "</p>"
                + "<a href=\"" + link + "\">" + textoLink + "</a></div></body></html>";
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(html);
    }

    /** Recebe o formulário de cadastro.html. Quem se cadastra aqui entra como PACIENTE (CPF + senha). */
    @PostMapping(value = "/cadastro/paciente", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> cadastrarPaciente(@RequestParam("nome") String nome,
                                                    @RequestParam("cpf") String cpf,
                                                    @RequestParam(name = "endereco", defaultValue = "") String endereco,
                                                    @RequestParam(name = "email", defaultValue = "") String email,
                                                    @RequestParam("telefone") String telefone,
                                                    @RequestParam("senha") String senha,
                                                    @RequestParam("senha2") String senha2) {
        String voltar = "/cadastro.html";
        if (nome.isBlank() || telefone.isBlank())
            return pagina(400, "Dados incompletos", "Preencha nome e telefone.", voltar, "Voltar");
        if (Auth.digitos(cpf).length() != 11)
            return pagina(400, "CPF inválido", "O CPF deve ter 11 números.", voltar, "Voltar");
        if (senha.length() < 6)
            return pagina(400, "Senha fraca", "A senha deve ter pelo menos 6 caracteres.", voltar, "Voltar");
        if (!senha.equals(senha2))
            return pagina(400, "Senhas diferentes", "A confirmação da senha não confere.", voltar, "Voltar");

        try {
            if (Regras.cpfExiste("pacientes", cpf))
                return pagina(409, "CPF já cadastrado", "Já existe um paciente com esse CPF.", "/index.html", "Ir para o login");

            Map<String, String> v = new LinkedHashMap<>();
            v.put("nome", nome.trim());
            v.put("cpf", Auth.digitos(cpf));
            v.put("endereco", endereco.trim());
            v.put("email", email.trim());
            v.put("telefone", telefone.trim());
            v.put("senha", Auth.hash(senha));
            PACIENTES.criar(v);
            return pagina(200, "Cadastro concluído!", "Agora entre com seu CPF e senha.", "/index.html", "Ir para o login");
        } catch (SQLException e) {
            e.printStackTrace();
            return pagina(500, "Erro no servidor", "Não foi possível salvar o cadastro. Tente novamente.", voltar, "Voltar");
        }
    }

    // ------------------------------------------------------------ cadastro de médico e funcionário
    private static final Crud MEDICOS = new Crud("medicos", "id_medicos",
            new String[]{"nome", "especialidade", "crm", "senha"},
            new String[]{"Nome", "Especialidade", "CRM", "Senha"},
            "nome", "especialidade", "crm", "senha");
    private static final Crud FUNCIONARIOS = new Crud("funcionarios", "id_funcionarios",
            new String[]{"nome", "cpf", "endereco", "email", "telefone", "senha"},
            new String[]{"Nome", "CPF", "Endereço", "E-mail", "Telefone", "Senha"},
            "nome", "cpf", "telefone", "senha");

    /** Formulário de cadastroMedico.html: nome, especialidade, crm, senha, senha2. */
    @PostMapping(value = "/cadastro/medico", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> cadastrarMedico(@RequestParam("nome") String nome,
                                                  @RequestParam("especialidade") String especialidade,
                                                  @RequestParam("crm") String crm,
                                                  @RequestParam("senha") String senha,
                                                  @RequestParam("senha2") String senha2) {
        String voltar = "/cadastroMedico.html";
        if (nome.isBlank() || especialidade.isBlank() || crm.isBlank())
            return pagina(400, "Dados incompletos", "Preencha nome, especialidade e CRM.", voltar, "Voltar");
        if (senha.length() < 6)
            return pagina(400, "Senha fraca", "A senha deve ter pelo menos 6 caracteres.", voltar, "Voltar");
        if (!senha.equals(senha2))
            return pagina(400, "Senhas diferentes", "A confirmação da senha não confere.", voltar, "Voltar");
        try {
            if (Regras.crmExiste(crm))
                return pagina(409, "CRM já cadastrado", "Já existe um médico com esse CRM.", "/index.html", "Ir para o login");
            Map<String, String> v = new LinkedHashMap<>();
            v.put("nome", nome.trim());
            v.put("especialidade", especialidade.trim());
            v.put("crm", crm.trim().toUpperCase());
            v.put("senha", Auth.hash(senha));
            MEDICOS.criar(v);
            return pagina(200, "Cadastro concluído!", "Agora entre com seu CRM e senha.", "/index.html", "Ir para o login");
        } catch (SQLException e) {
            e.printStackTrace();
            return pagina(500, "Erro no servidor", "Não foi possível salvar o cadastro. Tente novamente.", voltar, "Voltar");
        }
    }

    /** Formulário de cadastroFuncionario.html: mesmos campos do paciente. */
    @PostMapping(value = "/cadastro/funcionario", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> cadastrarFuncionario(@RequestParam("nome") String nome,
                                                       @RequestParam("cpf") String cpf,
                                                       @RequestParam(name = "endereco", defaultValue = "") String endereco,
                                                       @RequestParam(name = "email", defaultValue = "") String email,
                                                       @RequestParam("telefone") String telefone,
                                                       @RequestParam("senha") String senha,
                                                       @RequestParam("senha2") String senha2) {
        String voltar = "/cadastroFuncionario.html";
        if (nome.isBlank() || telefone.isBlank())
            return pagina(400, "Dados incompletos", "Preencha nome e telefone.", voltar, "Voltar");
        if (Auth.digitos(cpf).length() != 11)
            return pagina(400, "CPF inválido", "O CPF deve ter 11 números.", voltar, "Voltar");
        if (senha.length() < 6)
            return pagina(400, "Senha fraca", "A senha deve ter pelo menos 6 caracteres.", voltar, "Voltar");
        if (!senha.equals(senha2))
            return pagina(400, "Senhas diferentes", "A confirmação da senha não confere.", voltar, "Voltar");
        try {
            if (Regras.cpfExiste("funcionarios", cpf))
                return pagina(409, "CPF já cadastrado", "Já existe um funcionário com esse CPF.", "/index.html", "Ir para o login");
            Map<String, String> v = new LinkedHashMap<>();
            v.put("nome", nome.trim());
            v.put("cpf", Auth.digitos(cpf));
            v.put("endereco", endereco.trim());
            v.put("email", email.trim());
            v.put("telefone", telefone.trim());
            v.put("senha", Auth.hash(senha));
            FUNCIONARIOS.criar(v);
            return pagina(200, "Cadastro concluído!", "Agora entre com seu CPF e senha.", "/index.html", "Ir para o login");
        } catch (SQLException e) {
            e.printStackTrace();
            return pagina(500, "Erro no servidor", "Não foi possível salvar o cadastro. Tente novamente.", voltar, "Voltar");
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession http) {
        http.invalidate();
        return redirecionar("/index.html");
    }
}