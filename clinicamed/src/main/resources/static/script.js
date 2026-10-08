function fazerLogin() {
    // Oculta a página de login e exibe o dashboard central
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('dashboardPage').style.display = 'flex';
}

function logout() {
    // Retorna para a tela de login
    document.getElementById('dashboardPage').style.display = 'none';
    document.getElementById('loginPage').style.display = 'flex';
}

function alternarForm() {
    const loginF = document.getElementById('loginForm');
    const regF = document.getElementById('registerForm');
    if(loginF.style.display === 'none') {
        loginF.style.display = 'block';
        regF.style.display = 'none';
    } else {
        loginF.style.display = 'none';
        regF.style.display = 'block';
    }
}

function mudarAba(role, botao) {
    // Esconde todas as seções de visão do sistema
    document.querySelectorAll('.role-section').forEach(sec => sec.classList.remove('active'));
    // Remove o destaque visual do botão anterior
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    
    // Mostra a seção correspondente e destaca o botão clicado
    document.getElementById('aba-' + role).classList.add('active');
    botao.classList.add('active');
}
