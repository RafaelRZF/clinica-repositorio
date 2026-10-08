package clinicamed;

/** Quem está logado. id = id_pacientes, id_medicos ou id_funcionarios, conforme o perfil. */
public record Sessao(long id, String nome, String perfil) {}
