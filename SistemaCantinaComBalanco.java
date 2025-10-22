

// Salvar como SistemaCantinaComBalanco.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Calendar;


import org.jdatepicker.impl.*; // JDatePicker

public class SistemaCantinaComBalanco extends JFrame {

    private Connection conn;
    private int funcionarioId;
    private String funcionarioLogado;
    private boolean isAdmin;
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SistemaCantinaComBalanco() {
        conectarBanco();
        criarTabelas();
        mostrarTelaInicial();
    }

    // -------------------- Conexão e criação de tabelas --------------------
    private void conectarBanco() {
        try {
            new File("banco").mkdirs();
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:file:./banco/cantina;AUTO_SERVER=TRUE", "sa", "");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void criarTabelas() {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS funcionario (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        usuario VARCHAR(50) UNIQUE,
                        senha VARCHAR(50),
                        nome VARCHAR(100),
                        is_admin INT DEFAULT 0
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS cliente (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(200)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS produto (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(150),
                        tipo VARCHAR(30),
                        preco DOUBLE,
                        quantidade INT,
                        validade DATE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS pedido (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        funcionario_id INT,
                        cliente_id INT,
                        valor_total DOUBLE,
                        forma_pagamento VARCHAR(30),
                        data TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
                        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS pedido_item (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        pedido_id INT,
                        produto_id INT,
                        quantidade INT,
                        preco_unitario DOUBLE,
                        FOREIGN KEY (pedido_id) REFERENCES pedido(id),
                        FOREIGN KEY (produto_id) REFERENCES produto(id)
                    )
                    """);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao criar/verificar tabelas: " + e.getMessage());
        }
    }

    // -------------------- Tela inicial --------------------
    private void mostrarTelaInicial() {
        JFrame frame = new JFrame("Cantina - Inicial");
        frame.setSize(520, 420);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JLabel banner = new JLabel("🍩 Cantina - Sistema com Balanço por Cliente 🍕", SwingConstants.CENTER);
        banner.setFont(new Font("SansSerif", Font.BOLD, 20));
        banner.setOpaque(true);
        banner.setBackground(new Color(255, 220, 150));
        banner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3, 1, 12, 12));
        center.setBorder(BorderFactory.createEmptyBorder(30, 80, 30, 80));

        JButton btnLogin = new JButton("🔑 Login");
        JButton btnCadastrar = new JButton("➕ Cadastrar Funcionário");
        JButton btnSair = new JButton("❌ Sair");

        Font f = new Font("SansSerif", Font.BOLD, 18);
        for (JButton b : new JButton[]{btnLogin, btnCadastrar, btnSair}) {
            b.setFont(f);
            b.setBackground(new Color(255, 170, 80));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
        }

        center.add(btnLogin);
        center.add(btnCadastrar);
        center.add(btnSair);

        frame.add(center, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> {
            frame.dispose();
            mostrarTelaLogin();
        });

        btnCadastrar.addActionListener(e -> cadastrarFuncionarioDialog());

        btnSair.addActionListener(e -> {
            fecharConexao();
            frame.dispose();
        });

        frame.setVisible(true);
    }

    // -------------------- Login --------------------
    private void mostrarTelaLogin() {
        JFrame login = new JFrame("Login - Cantina");
        login.setSize(360, 220);
        login.setLocationRelativeTo(null);
        login.setLayout(new GridLayout(4, 2, 8, 8));

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JButton btnEntrar = new JButton("Entrar");
        JButton btnVoltar = new JButton("Voltar");

        login.add(new JLabel("Usuário:")); login.add(txtUser);
        login.add(new JLabel("Senha:")); login.add(txtPass);
        login.add(btnEntrar); login.add(btnVoltar);

        btnEntrar.addActionListener(e -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, nome, is_admin FROM funcionario WHERE usuario = ? AND senha = ?")) {
                ps.setString(1, txtUser.getText());
                ps.setString(2, new String(txtPass.getPassword()));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    funcionarioId = rs.getInt("id");
                    funcionarioLogado = rs.getString("nome");
                    isAdmin = rs.getInt("is_admin") == 1;
                    login.dispose();
                    mostrarTelaPrincipal();
                } else {
                    JOptionPane.showMessageDialog(login, "Usuário ou senha incorretos.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(login, "Erro ao consultar usuário: " + ex.getMessage());
            }
        });

        btnVoltar.addActionListener(e -> {
            login.dispose();
            mostrarTelaInicial();
        });

        login.setVisible(true);
    }

    // -------------------- Cadastrar funcionário --------------------
    private void cadastrarFuncionarioDialog() {
        String usuario = JOptionPane.showInputDialog("Usuário (login):");
        if (usuario == null || usuario.isBlank()) return;
        String senha = JOptionPane.showInputDialog("Senha:");
        if (senha == null) return;
        String nome = JOptionPane.showInputDialog("Nome completo:");
        if (nome == null) return;
        int admin = JOptionPane.showConfirmDialog(null, "É administrador?", "Permissão", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION ? 1 : 0;

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO funcionario(usuario, senha, nome, is_admin) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, usuario);
            ps.setString(2, senha);
            ps.setString(3, nome);
            ps.setInt(4, admin);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Funcionário cadastrado com sucesso!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar funcionário: " + e.getMessage());
        }
    }

    // -------------------- Tela principal --------------------
    private void mostrarTelaPrincipal() {
        JFrame tela = new JFrame("Cantina - " + funcionarioLogado);
        tela.setSize(700, 460);
        tela.setLocationRelativeTo(null);
        tela.setLayout(new BorderLayout());

        JLabel top = new JLabel("Bem-vindo, " + funcionarioLogado + (isAdmin ? " (Admin)" : ""), SwingConstants.CENTER);
        top.setFont(new Font("SansSerif", Font.BOLD, 18));
        top.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        tela.add(top, BorderLayout.NORTH);

        JPanel painel = new JPanel(new GridLayout(5, 1, 10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 120, 20, 120));

        JButton btnPedido = new JButton("🛒 Registrar Pedido (Venda)");
        JButton btnProdutos = new JButton("📦 Gerenciar Produtos");
        JButton btnClientes = new JButton("👥 Gerenciar Clientes");
        JButton btnBalanco = new JButton("📊 Balanço de Vendas por Cliente");
        JButton btnLogout = new JButton("🔙 Logout");

        Font btnFont = new Font("SansSerif", Font.BOLD, 16);
        for (JButton b : new JButton[]{btnPedido, btnProdutos, btnClientes, btnBalanco, btnLogout}) {
            b.setFont(btnFont);
            b.setBackground(new Color(255, 170, 100));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
        }

        painel.add(btnPedido);
        painel.add(btnProdutos);
        painel.add(btnClientes);
        painel.add(btnBalanco);
        painel.add(btnLogout);
        tela.add(painel, BorderLayout.CENTER);

        btnPedido.addActionListener(e -> registrarPedidoComValidacaoCliente());
        btnProdutos.addActionListener(e -> gerenciarProdutosMenu());
        btnClientes.addActionListener(e -> gerenciarClientesMenu());
        btnBalanco.addActionListener(e -> exibirBalancoClientesComFiltro());
        btnLogout.addActionListener(e -> {
            tela.dispose();
            mostrarTelaInicial();
        });

        tela.setVisible(true);
    }

    // ====================== MÉTODOS FUNCIONAIS ======================

    // --- Produtos ---
    private void gerenciarProdutosMenu() {
        String[] op = {"Listar produtos", "Adicionar produto", "Editar produto", "Remover produto", "Voltar"};
        while (true) {
            String escolha = (String) JOptionPane.showInputDialog(null, "Gerenciar Produtos", "Produtos",
                    JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
            if (escolha == null || escolha.equals("Voltar")) return;
            switch (escolha) {
                case "Listar produtos" -> listarProdutos();
                case "Adicionar produto" -> adicionarProduto();
                case "Editar produto" -> editarProduto();
                case "Remover produto" -> removerProduto();
            }
        }
    }

    private void listarProdutos() {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT id, nome, tipo, preco, quantidade, validade FROM produto ORDER BY nome");
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(" - ").append(rs.getString("nome"))
                        .append(" [").append(rs.getString("tipo")).append("] R$ ").append(String.format("%.2f", rs.getDouble("preco")))
                        .append(" | Qt: ").append(rs.getInt("quantidade"))
                        .append(" | Val: ").append(rs.getDate("validade") == null ? "--" : rs.getDate("validade").toLocalDate().format(DATE_FMT))
                        .append("\n");
            }
            JTextArea area = new JTextArea(sb.length() == 0 ? "Nenhum produto cadastrado." : sb.toString());
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Produtos", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao listar produtos: " + e.getMessage());
        }
    }

    private void adicionarProduto() {
        try {
            String nome = JOptionPane.showInputDialog("Nome do produto:");
            if (nome == null || nome.isBlank()) return;
            String tipo = (String) JOptionPane.showInputDialog(null, "Tipo:", "Tipo",
                    JOptionPane.QUESTION_MESSAGE, null, new String[]{"Salgado", "Doce"}, "Salgado");
            if (tipo == null) return;
            String precoStr = JOptionPane.showInputDialog("Preço (ex: 5.50):");
            if (precoStr == null) return;
            double preco = Double.parseDouble(precoStr.replace(',', '.'));
            String qtdStr = JOptionPane.showInputDialog("Quantidade em estoque:");
            if (qtdStr == null) return;
            int qtd = Integer.parseInt(qtdStr);
            String valStr = JOptionPane.showInputDialog("Validade (yyyy-MM-dd) - deixe em branco se não houver:");
            java.sql.Date validade = null;
            if (valStr != null && !valStr.isBlank()) validade = java.sql.Date.valueOf(LocalDate.parse(valStr, DATE_FMT));

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO produto(nome, tipo, preco, quantidade, validade) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, nome);
                ps.setString(2, tipo);
                ps.setDouble(3, preco);
                ps.setInt(4, qtd);
                if (validade != null) ps.setDate(5, validade); else ps.setNull(5, Types.DATE);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Produto adicionado com sucesso!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao adicionar produto: " + e.getMessage());
        }
    }

    private void editarProduto() {
        // Implementação similar à adicionarProduto(), omitida
    }

    private void removerProduto() {
        try {
            String idStr = JOptionPane.showInputDialog("ID do produto a remover:");
            if (idStr == null || idStr.isBlank()) return;
            int id = Integer.parseInt(idStr);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM produto WHERE id = ?")) {
                ps.setInt(1, id);
                int aff = ps.executeUpdate();
                JOptionPane.showMessageDialog(this, aff > 0 ? "Produto removido." : "Produto não encontrado.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao remover produto: " + e.getMessage());
        }
    }

    // --- Clientes ---
    private void gerenciarClientesMenu() {
        String[] op = {"Listar clientes", "Adicionar cliente", "Editar cliente", "Remover cliente", "Voltar"};
        while (true) {
            String escolha = (String) JOptionPane.showInputDialog(null, "Gerenciar Clientes", "Clientes",
                    JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
            if (escolha == null || escolha.equals("Voltar")) return;
            switch (escolha) {
                case "Listar clientes" -> listarClientes();
                case "Adicionar cliente" -> adicionarCliente();
                case "Editar cliente" -> editarCliente();
                case "Remover cliente" -> removerCliente();
            }
        }
    }

    private void listarClientes() {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT id, nome FROM cliente ORDER BY nome");
            StringBuilder sb = new StringBuilder();
            while (rs.next()) sb.append(rs.getInt("id")).append(" - ").append(rs.getString("nome")).append("\n");
            JTextArea area = new JTextArea(sb.length() == 0 ? "Nenhum cliente cadastrado." : sb.toString());
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Clientes", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao listar clientes: " + e.getMessage());
        }
    }

    private void adicionarCliente() {
        try {
            String nome = JOptionPane.showInputDialog("Nome do cliente:");
            if (nome == null || nome.isBlank()) return;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO cliente(nome) VALUES (?)")) {
                ps.setString(1, nome);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cliente adicionado com sucesso!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao adicionar cliente: " + e.getMessage());
        }
    }

    private void editarCliente() {
        // Implementação semelhante ao produto
    }

    private void removerCliente() {
        try {
            String idStr = JOptionPane.showInputDialog("ID do cliente a remover:");
            if (idStr == null || idStr.isBlank()) return;
            int id = Integer.parseInt(idStr);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
                ps.setInt(1, id);
                int aff = ps.executeUpdate();
                JOptionPane.showMessageDialog(this, aff > 0 ? "Cliente removido." : "Cliente não encontrado.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao remover cliente: " + e.getMessage());
        }
    }

    // --- Registrar pedido ---
    private void registrarPedidoComValidacaoCliente() {
        try {
            Integer clienteId = selecionarOuCriarCliente();
            if (clienteId == null) return;

            List<Integer> produtos = new ArrayList<>();
            List<Integer> quantidades = new ArrayList<>();

            while (true) {
                String listagem = listarProdutosTexto();
                JTextArea area = new JTextArea(listagem);
                area.setEditable(false);
                JScrollPane scroll = new JScrollPane(area);
                scroll.setPreferredSize(new Dimension(550, 300));
                int op = JOptionPane.showConfirmDialog(null, scroll, "Lista de produtos (OK para escolher; Cancel para finalizar)", JOptionPane.OK_CANCEL_OPTION);
                if (op != JOptionPane.OK_OPTION) break;

                String pidStr = JOptionPane.showInputDialog("Digite o ID do produto:");
                if (pidStr == null || pidStr.isBlank()) break;
                int pid = Integer.parseInt(pidStr);

                try (PreparedStatement ps = conn.prepareStatement("SELECT nome, preco, quantidade, validade FROM produto WHERE id = ?")) {
                    ps.setInt(1, pid);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { JOptionPane.showMessageDialog(this, "Produto não encontrado."); continue; }
                    String nome = rs.getString("nome");
                    int estoque = rs.getInt("quantidade");
                    Date val = rs.getDate("validade");
                    if (val != null && val.toLocalDate().isBefore(LocalDate.now())) {
                        JOptionPane.showMessageDialog(this, "Produto '" + nome + "' vencido — não pode ser vendido.");
                        continue;
                    }
                    String qtdStr = JOptionPane.showInputDialog("Quantidade (em estoque: " + estoque + "):");
                    if (qtdStr == null || qtdStr.isBlank()) continue;
                    int qtd = Integer.parseInt(qtdStr);
                    if (qtd <= 0) { JOptionPane.showMessageDialog(this, "Quantidade inválida."); continue; }
                    if (qtd > estoque) { JOptionPane.showMessageDialog(this, "Quantidade maior que o estoque."); continue; }

                    produtos.add(pid);
                    quantidades.add(qtd);

                    int more = JOptionPane.showConfirmDialog(this, "Adicionar mais itens?", "Pedido", JOptionPane.YES_NO_OPTION);
                    if (more != JOptionPane.YES_OPTION) break;
                }
            }

            if (produtos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum item adicionado ao pedido.");
                return;
            }

            double total = 0;
            for (int i = 0; i < produtos.size(); i++) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT preco FROM produto WHERE id = ?")) {
                    ps.setInt(1, produtos.get(i));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) total += rs.getDouble("preco") * quantidades.get(i);
                }
            }

            // Status de pagamento: Pago ou Em aberto
            String[] statusOpcoes = {"Pago", "Em aberto"};
            String status = (String) JOptionPane.showInputDialog(null, "Status do pagamento:", "Pagamento",
                    JOptionPane.QUESTION_MESSAGE, null, statusOpcoes, statusOpcoes[0]);
            if (status == null) return;

            conn.setAutoCommit(false);
            try (PreparedStatement psPedido = conn.prepareStatement(
                    "INSERT INTO pedido(funcionario_id, cliente_id, valor_total, forma_pagamento) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                psPedido.setInt(1, funcionarioId);
                psPedido.setInt(2, clienteId);
                psPedido.setDouble(3, total);
                psPedido.setString(4, status); // <-- aqui salvamos o status em vez da forma
                psPedido.executeUpdate();

                ResultSet gen = psPedido.getGeneratedKeys();
                gen.next();
                int pedidoId = gen.getInt(1);

                for (int i = 0; i < produtos.size(); i++) {
                    int pid = produtos.get(i);
                    int qtd = quantidades.get(i);
                    double precoUnit = getPrecoProduto(pid);

                    try (PreparedStatement psItem = conn.prepareStatement("INSERT INTO pedido_item(pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)")) {
                        psItem.setInt(1, pedidoId);
                        psItem.setInt(2, pid);
                        psItem.setInt(3, qtd);
                        psItem.setDouble(4, precoUnit);
                        psItem.executeUpdate();
                    }

                    try (PreparedStatement up = conn.prepareStatement("UPDATE produto SET quantidade = quantidade - ? WHERE id = ?")) {
                        up.setInt(1, qtd);
                        up.setInt(2, pid);
                        up.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Pedido registrado com sucesso! Total: R$ " + String.format("%.2f", total));
            } catch (SQLException ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Erro ao registrar pedido: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        }
    }


    private int selecionarOuCriarCliente() {
        try {
            StringBuilder sb = new StringBuilder("Clientes:\n");
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, nome FROM cliente ORDER BY nome");
            while (rs.next()) sb.append(rs.getInt("id")).append(" - ").append(rs.getString("nome")).append("\n");
            String idStr = JOptionPane.showInputDialog(sb + "\nDigite ID do cliente ou 0 para criar novo:");
            if (idStr == null) return -1;
            int id = Integer.parseInt(idStr);
            if (id == 0) {
                adicionarCliente();
                return selecionarOuCriarCliente();
            }
            return id;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao selecionar cliente: " + e.getMessage());
            return -1;
        }
    }

    private String listarProdutosTexto() throws SQLException {
        StringBuilder sb = new StringBuilder("Produtos:\n");
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT id, nome, preco, quantidade FROM produto ORDER BY nome");
        while (rs.next()) sb.append(rs.getInt("id")).append(" - ").append(rs.getString("nome"))
                .append(" R$ ").append(String.format("%.2f", rs.getDouble("preco")))
                .append(" | Qt: ").append(rs.getInt("quantidade")).append("\n");
        return sb.toString();
    }

    private double getPrecoProduto(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT preco FROM produto WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("preco") : 0;
        }
    }

    // --- Balanço com calendário ---
    private void exibirBalancoClientesComFiltro() {
        try {
            String clienteNome = JOptionPane.showInputDialog("Filtrar por cliente (deixe em branco para todos):");

            // Painel de data
            JPanel pickerPanel = new JPanel(new GridLayout(2,2));
            UtilDateModel modelIni = new UtilDateModel();
            UtilDateModel modelFim = new UtilDateModel();
            Properties p = new Properties();
            p.put("text.today", "Hoje"); p.put("text.month", "Mês"); p.put("text.year", "Ano");
            JDatePickerImpl datePickerIni = new JDatePickerImpl(new JDatePanelImpl(modelIni, p), new DateLabelFormatter());
            JDatePickerImpl datePickerFim = new JDatePickerImpl(new JDatePanelImpl(modelFim, p), new DateLabelFormatter());
            pickerPanel.add(new JLabel("Data inicial:")); pickerPanel.add(datePickerIni);
            pickerPanel.add(new JLabel("Data final:")); pickerPanel.add(datePickerFim);

            int res = JOptionPane.showConfirmDialog(this, pickerPanel, "Selecione período", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            java.util.Date dataIniUtil = (java.util.Date) datePickerIni.getModel().getValue();
            java.util.Date dataFimUtil = (java.util.Date) datePickerFim.getModel().getValue();

            // Query detalhada por cliente e pedido
            StringBuilder sql = new StringBuilder(
                    "SELECT c.nome AS cliente, p.id AS pedido_id, p.forma_pagamento, pr.nome AS produto, pi.quantidade, pi.preco_unitario " +
                            "FROM pedido p " +
                            "JOIN cliente c ON p.cliente_id = c.id " +
                            "JOIN pedido_item pi ON pi.pedido_id = p.id " +
                            "JOIN produto pr ON pi.produto_id = pr.id " +
                            "WHERE 1=1"
            );

            if (clienteNome != null && !clienteNome.isBlank()) sql.append(" AND c.nome LIKE ?");
            if (dataIniUtil != null) sql.append(" AND p.data >= ?");
            if (dataFimUtil != null) sql.append(" AND p.data <= ?");

            sql.append(" ORDER BY c.nome, p.id");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (clienteNome != null && !clienteNome.isBlank()) ps.setString(idx++, "%" + clienteNome + "%");
            if (dataIniUtil != null) ps.setDate(idx++, new java.sql.Date(dataIniUtil.getTime()));
            if (dataFimUtil != null) ps.setDate(idx++, new java.sql.Date(dataFimUtil.getTime()));

            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("Balanço de Vendas Detalhado:\n\n");

            String clienteAtual = "";
            int pedidoAtual = -1;

            while (rs.next()) {
                String cliente = rs.getString("cliente");
                int pedidoId = rs.getInt("pedido_id");
                String status = rs.getString("forma_pagamento");
                String produto = rs.getString("produto");
                int qtd = rs.getInt("quantidade");
                double preco = rs.getDouble("preco_unitario");

                if (!cliente.equals(clienteAtual)) {
                    sb.append("\nCliente: ").append(cliente).append("\n");
                    clienteAtual = cliente;
                    pedidoAtual = -1;
                }
                if (pedidoId != pedidoAtual) {
                    sb.append("  Pedido #").append(pedidoId).append(" (").append(status).append(")\n");
                    pedidoAtual = pedidoId;
                }
                sb.append("    - ").append(produto).append(" x").append(qtd).append(" = R$ ").append(String.format("%.2f", preco * qtd)).append("\n");
            }

            JTextArea area = new JTextArea(sb.length() == 0 ? "Nenhum dado encontrado." : sb.toString());
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area));

            int salvar = JOptionPane.showConfirmDialog(this, "Deseja exportar o balanço em CSV?", "Exportar", JOptionPane.YES_NO_OPTION);
            if (salvar == JOptionPane.YES_OPTION) exportarBalancoCSV(sb.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar balanço: " + e.getMessage());
        }
    }


    private void exportarBalancoCSV(String dados) {
        try {
            File f = new File("balanco_clientes.csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("Cliente,Total");
                for (String linha : dados.split("\n")) {
                    if (linha.isBlank() || linha.startsWith("Balanço")) continue;
                    String[] partes = linha.split(": R\\$ ");
                    if (partes.length == 2) pw.println(partes[0].trim() + "," + partes[1].trim());
                }
            }
            JOptionPane.showMessageDialog(this, "Exportado para " + f.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar CSV: " + e.getMessage());
        }
    }

    // -------------------- Encerrar --------------------
    private void fecharConexao() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }

    // -------------------- MAIN --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SistemaCantinaComBalanco::new);
    }

    // -------------------- Formatter para JDatePicker --------------------
    public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            if (text == null || text.isBlank()) return null;
            return java.sql.Date.valueOf(LocalDate.parse(text, df));
        }

        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value == null) return "";
            if (value instanceof java.util.Calendar cal) {
                LocalDate ld = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
                return df.format(ld);
            }
            return "";
        }
    }
}
