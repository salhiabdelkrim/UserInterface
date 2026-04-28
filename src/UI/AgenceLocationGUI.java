package UI;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class AgenceLocationGUI extends JFrame {
    private JTextArea logArea;

    public AgenceLocationGUI() {
        setTitle("Gestion Agence de Location - SMI1002_030");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu avec tous les cas d'utilisation demandés
        JPanel menuPanel = new JPanel(new GridLayout(9, 1, 5, 5));
        
        JButton btnClient = new JButton("Gestion Clients (Ajout/Modif)");
        JButton btnDispo = new JButton("Voitures Disponibles (Index)");
        JButton btnReser = new JButton("Créer Réservation");
        JButton btnLouer = new JButton("Enregistrer Location (Transaction)");
        JButton btnPaie = new JButton("Gérer Paiements");
        JButton btnRetour = new JButton("Enregistrer Retour");
        JButton btnEntretien = new JButton("Suivre Entretien");
        JButton btnTrace = new JButton("Journal de Trace");
        JButton btnQuitter = new JButton("Quitter");

        menuPanel.add(btnClient);
        menuPanel.add(btnDispo);
        menuPanel.add(btnReser);
        menuPanel.add(btnLouer);
        menuPanel.add(btnPaie);
        menuPanel.add(btnRetour);
        menuPanel.add(btnEntretien);
        menuPanel.add(btnTrace);
        menuPanel.add(btnQuitter);

        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(menuPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        // Mapping des actions
        btnClient.addActionListener(e -> gestionClient());
        btnDispo.addActionListener(e -> consulterVoitures());
        btnReser.addActionListener(e -> creerReservation());
        btnLouer.addActionListener(e -> creerLocation());
        btnPaie.addActionListener(e -> gererPaiement());
        btnRetour.addActionListener(e -> enregistrerRetour());
        btnEntretien.addActionListener(e -> suivreEntretien());
        btnTrace.addActionListener(e -> voirJournal());
        btnQuitter.addActionListener(e -> System.exit(0));
    }

    private Connection getConn() throws SQLException {
        // Syntaxe avec '/' pour éviter l'erreur de Listener sur gaia
        String url = "jdbc:oracle:thin:@//gaia.emp.uqtr.ca:1521/coursbd.uqtr.ca";
        return DriverManager.getConnection(url, "SMI1002_030", "69ajxa84");
    }

    // 1. AJOUTER/MODIFIER CLIENT
    private void gestionClient() {
        String id = JOptionPane.showInputDialog("ID Client (vide pour nouvel ajout) :");
        String nom = JOptionPane.showInputDialog("Nom :");
        String prenom = JOptionPane.showInputDialog("Prénom :");
        
        String sql = (id == null || id.isEmpty()) 
            ? "INSERT INTO CLIENT (ID_CLIENT, NOM, PRENOM) VALUES (seq_client.NEXTVAL, ?, ?)"
            : "UPDATE CLIENT SET NOM = ?, PRENOM = ? WHERE ID_CLIENT = ?";

        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            if (id != null && !id.isEmpty()) ps.setString(3, id);
            ps.executeUpdate();
            logArea.setText("Client mis à jour avec succès.");
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 2. CONSULTER DISPONIBILITÉS (Optimisation Index)
    private void consulterVoitures() {
        logArea.setText("--- VOITURES DISPONIBLES (Scan via Index) ---\n");
        String sql = "SELECT id_voiture, marque, modele FROM voiture WHERE statut = 'DISPONIBLE'";
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logArea.append(rs.getString(1) + " | " + rs.getString(2) + " " + rs.getString(3) + "\n");
            }
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 3. CRÉER RÉSERVATION
    private void creerReservation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        String sql = "INSERT INTO RESERVATION (ID_RESERVATION, DATE_RESERVATION, ID_CLIENT, ID_VOITURE, STATUT_RESERVATION) VALUES (seq_res.NEXTVAL, SYSDATE, ?, ?, 'CONFIRMEE')";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idC); ps.setString(2, idV);
            ps.executeUpdate();
            logArea.setText("Réservation créée.");
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 4. ENREGISTRER LOCATION (Transaction + Concurrence)
    private void creerLocation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            String lock = "SELECT statut FROM voiture WHERE id_voiture = ? FOR UPDATE";
            PreparedStatement psL = conn.prepareStatement(lock);
            psL.setString(1, idV);
            ResultSet rs = psL.executeQuery();
            if (rs.next() && rs.getString(1).equals("DISPONIBLE")) {
                conn.createStatement().executeUpdate("INSERT INTO LOCATION (ID_LOCATION, ID_CLIENT, ID_VOITURE, DATE_DEBUT) VALUES (seq_loc.NEXTVAL, '"+idC+"', '"+idV+"', SYSDATE)");
                conn.createStatement().executeUpdate("UPDATE VOITURE SET STATUT = 'LOUEE' WHERE ID_VOITURE = '"+idV+"'");
                conn.commit();
                logArea.setText("Location réussie (Transaction validée).");
            } else { conn.rollback(); logArea.setText("Échec : Voiture non disponible."); }
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 5. GÉRER LES PAIEMENTS
    private void gererPaiement() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        String montant = JOptionPane.showInputDialog("Montant :");
        String sql = "INSERT INTO PAIEMENT (ID_PAIEMENT, MONTANT, DATE_PAIEMENT, ID_LOCATION) VALUES (seq_paie.NEXTVAL, ?, SYSDATE, ?)";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Double.parseDouble(montant));
            ps.setString(2, idL);
            ps.executeUpdate();
            logArea.setText("Paiement enregistré.");
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 6. ENREGISTRER RETOUR
    private void enregistrerRetour() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            // On libère la voiture liée à cette location
            String sqlUpd = "UPDATE VOITURE SET STATUT = 'DISPONIBLE' WHERE ID_VOITURE = (SELECT ID_VOITURE FROM LOCATION WHERE ID_LOCATION = ?)";
            PreparedStatement ps = conn.prepareStatement(sqlUpd);
            ps.setString(1, idL);
            ps.executeUpdate();
            // On enregistre le retour
            conn.createStatement().executeUpdate("INSERT INTO RETOUR (ID_RETOUR, DATE_RETOUR, ID_LOCATION) VALUES (seq_ret.NEXTVAL, SYSDATE, '"+idL+"')");
            conn.commit();
            logArea.setText("Retour effectué. Voiture de nouveau disponible.");
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    // 7. SUIVRE L'ENTRETIEN
    private void suivreEntretien() {
        logArea.setText("--- VÉHICULES EN ENTRETIEN ---\n");
        String sql = "SELECT id_voiture, marque, type_entretien FROM voiture v JOIN entretien e ON v.id_voiture = e.id_voiture WHERE statut = 'ENTRETIEN'";
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logArea.append(rs.getString(1) + " | " + rs.getString(2) + " | Type: " + rs.getString(3) + "\n");
            }
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    private void voirJournal() {
        logArea.setText("--- JOURNAL DE TRACE (Audit) ---\n");
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM trace_log ORDER BY date_action DESC")) {
            while (rs.next()) {
                logArea.append(rs.getString("DATE_ACTION") + " | " + rs.getString("TYPE_ACTION") + " sur " + rs.getString("TABLE_VISEE") + "\n");
            }
        } catch (SQLException ex) { logArea.append("Erreur : " + ex.getMessage()); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AgenceLocationGUI().setVisible(true));
    }
}