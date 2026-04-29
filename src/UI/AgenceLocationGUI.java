package UI;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class AgenceLocationGUI extends JFrame {
    private JTextArea logArea;

    public AgenceLocationGUI() {
        setTitle("Gestion Agence de Location - SMI1002_030");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel(new GridLayout(9, 1, 5, 5));
        String[] labels = {"Gestion Clients", "Voitures Dispo", "Créer Réservation", 
                           "Enregistrer Location", "Gérer Paiement", "Enregistrer Retour", 
                           "Suivre Entretien", "Journal de Trace", "Quitter"};
        
        JButton[] buttons = new JButton[labels.length];
        for(int i=0; i<labels.length; i++) {
            buttons[i] = new JButton(labels[i]);
            menuPanel.add(buttons[i]);
        }

        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(menuPanel, BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Actions
        buttons[0].addActionListener(e -> gestionClient());
        buttons[1].addActionListener(e -> consulterVoitures());
        buttons[2].addActionListener(e -> creerReservation());
        buttons[3].addActionListener(e -> creerLocation());
        buttons[4].addActionListener(e -> gererPaiement());
        buttons[5].addActionListener(e -> enregistrerRetour());
        buttons[6].addActionListener(e -> suivreEntretien());
        buttons[7].addActionListener(e -> voirJournal());
        buttons[8].addActionListener(e -> System.exit(0));
    }

    private Connection getConn() throws SQLException {
        try { Class.forName("oracle.jdbc.OracleDriver"); } catch (Exception e) {}
        String url = "jdbc:oracle:thin:@//gaia.emp.uqtr.ca:1521/coursbd.uqtr.ca";
        return DriverManager.getConnection(url, "SMI1002_030", "69ajxa84");
    }

    // 1. CLIENT (Respecte image_116c37)
    private void gestionClient() {
        String nom = JOptionPane.showInputDialog("Nom :");
        String prenom = JOptionPane.showInputDialog("Prénom :");
        String tel = JOptionPane.showInputDialog("Téléphone :");
        String permis = JOptionPane.showInputDialog("Numéro Permis :");
        
        String sql = "INSERT INTO CLIENT (ID_CLIENT, NOM, PRENOM, TELEPHONE, NUMERO_PERMIS) VALUES (seq_client.NEXTVAL, ?, ?, ?, ?)";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom); ps.setString(2, prenom); ps.setString(3, tel); ps.setString(4, permis);
            ps.executeUpdate();
            logArea.setText("✅ Client ajouté.");
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 2. VOITURE (Respecte image_dce683 : PRIX_JOURNALIER, STATUT)
    private void consulterVoitures() {
        logArea.setText("--- VÉHICULES DISPONIBLES ---\n");
        String sql = "SELECT ID_VOITURE, MARQUE, MODELE, PRIX_JOURNALIER FROM VOITURE WHERE STATUT = 'DISPONIBLE'";
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logArea.append(rs.getString(1) + " | " + rs.getString(2) + " " + rs.getString(3) + " (" + rs.getDouble(4) + "$/jour)\n");
            }
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 3. RÉSERVATION (Respecte image_116c37 : DATE_DEBUT_PREVUE, DATE_FIN_PREVUE)
    private void creerReservation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        String idE = JOptionPane.showInputDialog("ID Employé :");
        String d1 = JOptionPane.showInputDialog("Début (YYYY-MM-DD) :");
        String d2 = JOptionPane.showInputDialog("Fin (YYYY-MM-DD) :");

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            String sqlRes = "INSERT INTO RESERVATION (ID_RESERVATION, DATE_RESERVATION, DATE_DEBUT_PREVUE, DATE_FIN_PREVUE, STATUT_RESERVATION, ID_CLIENT, ID_VOITURE, ID_EMPLOYE) VALUES (seq_res.NEXTVAL, SYSDATE, TO_DATE(?,'YYYY-MM-DD'), TO_DATE(?,'YYYY-MM-DD'), 'CONFIRMEE', ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlRes)) {
                ps.setString(1, d1); ps.setString(2, d2); ps.setString(3, idC); ps.setString(4, idV); ps.setString(5, idE);
                ps.executeUpdate();
                conn.createStatement().executeUpdate("UPDATE VOITURE SET STATUT = 'RESERVEE' WHERE ID_VOITURE = " + idV);
                conn.commit();
                logArea.setText("✅ Réservation réussie.");
            } catch (SQLException ex) { conn.rollback(); throw ex; }
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 4. LOCATION (Respecte image_116c37 : MONTANT_TOTAL, STATUT_LOCATION)
    private void creerLocation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        String idE = JOptionPane.showInputDialog("ID Employé :");
        
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            // Verrouillage concurrence
            String lock = "SELECT STATUT FROM VOITURE WHERE ID_VOITURE = ? FOR UPDATE";
            PreparedStatement psL = conn.prepareStatement(lock);
            psL.setString(1, idV);
            ResultSet rs = psL.executeQuery();
            
            if (rs.next() && (rs.getString(1).equals("DISPONIBLE") || rs.getString(1).equals("RESERVEE"))) {
                String ins = "INSERT INTO LOCATION (ID_LOCATION, DATE_LOCATION, DATE_DEBUT, STATUT_LOCATION, ID_CLIENT, ID_VOITURE, ID_EMPLOYE) VALUES (seq_loc.NEXTVAL, SYSDATE, SYSDATE, 'EN_COURS', ?, ?, ?)";
                PreparedStatement psI = conn.prepareStatement(ins);
                psI.setString(1, idC); psI.setString(2, idV); psI.setString(3, idE);
                psI.executeUpdate();
                conn.createStatement().executeUpdate("UPDATE VOITURE SET STATUT = 'LOUEE' WHERE ID_VOITURE = " + idV);
                conn.commit();
                logArea.setText("✅ Location enregistrée.");
            } else { conn.rollback(); logArea.setText("❌ Voiture non disponible."); }
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 5. PAIEMENT (Respecte image_116c37 : MODE_PAIEMENT, STATUT_PAIEMENT)
    private void gererPaiement() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        String montant = JOptionPane.showInputDialog("Montant :");
        String mode = JOptionPane.showInputDialog("Mode (CARTE/COMPTANT) :");

        String sql = "INSERT INTO PAIEMENT (ID_PAIEMENT, DATE_PAIEMENT, MONTANT, MODE_PAIEMENT, STATUT_PAIEMENT, ID_LOCATION) VALUES (seq_paie.NEXTVAL, SYSDATE, ?, ?, 'VALIDE', ?)";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Double.parseDouble(montant)); ps.setString(2, mode); ps.setString(3, idL);
            ps.executeUpdate();
            logArea.setText("✅ Paiement ajouté.");
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 6. RETOUR (Respecte image_116c37 : KILOMETRAGE_RETOUR, NIVEAU_CARBURANT)
    private void enregistrerRetour() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        String km = JOptionPane.showInputDialog("Kilométrage retour :");
        String idE = JOptionPane.showInputDialog("ID Employé :");

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            String sqlRet = "INSERT INTO RETOUR (ID_RETOUR, DATE_RETOUR, KILOMETRAGE_RETOUR, NIVEAU_CARBURANT, ID_LOCATION, ID_EMPLOYE) VALUES (seq_ret.NEXTVAL, SYSDATE, ?, 'PLEIN', ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sqlRet);
            ps.setString(1, km); ps.setString(2, idL); ps.setString(3, idE);
            ps.executeUpdate();
            
            conn.createStatement().executeUpdate("UPDATE VOITURE SET STATUT = 'DISPONIBLE' WHERE ID_VOITURE = (SELECT ID_VOITURE FROM LOCATION WHERE ID_LOCATION = " + idL + ")");
            conn.commit();
            logArea.setText("✅ Retour traité.");
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 7. ENTRETIEN (Respecte image_116c37 : COUT, TYPE_ENTRETIEN)
    private void suivreEntretien() {
        logArea.setText("--- HISTORIQUE ENTRETIEN ---\n");
        String sql = "SELECT E.ID_ENTRETIEN, E.TYPE_ENTRETIEN, E.COUT, V.MARQUE FROM ENTRETIEN E JOIN VOITURE V ON E.ID_VOITURE = V.ID_VOITURE";
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logArea.append("ID:" + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getDouble(3) + "$ | Véhicule: " + rs.getString(4) + "\n");
            }
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    // 8. TRACE (Journalisation des triggers)
    private void voirJournal() {
        logArea.setText("--- AUDIT SYSTEME (TRACE) ---\n");
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM TRACE_LOG ORDER BY DATE_ACTION DESC")) {
            while (rs.next()) {
                logArea.append(rs.getString("DATE_ACTION") + " | " + rs.getString("TYPE_ACTION") + " | " + rs.getString("TABLE_VISEE") + "\n");
            }
        } catch (SQLException ex) { logArea.setText("❌ Erreur : " + ex.getMessage()); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AgenceLocationGUI().setVisible(true));
    }
}