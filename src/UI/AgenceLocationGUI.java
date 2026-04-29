package UI;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class AgenceLocationGUI extends JFrame {
    private JTextArea logArea;
    private String roleActuel;

    public AgenceLocationGUI() {
        roleActuel = choisirRole();

        setTitle("Gestion Agence de Location - Rôle : " + roleActuel);
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel(new GridLayout(10, 1, 5, 5));

        JButton btnClients = new JButton("Gestion Clients");
        JButton btnVoitures = new JButton("Voitures Disponibles");
        JButton btnReservation = new JButton("Créer Réservation");
        JButton btnLocation = new JButton("Enregistrer Location");
        JButton btnPaiement = new JButton("Gérer Paiement");
        JButton btnRetour = new JButton("Enregistrer Retour");
        JButton btnEntretien = new JButton("Suivre Entretien");
        JButton btnJournal = new JButton("Journal de Trace");
        JButton btnRapports = new JButton("Rapports / Statistiques");
        JButton btnQuitter = new JButton("Quitter");

        if (roleActuel.equals("ADMINISTRATEUR")) {
            menuPanel.add(btnClients);
            menuPanel.add(btnVoitures);
            menuPanel.add(btnReservation);
            menuPanel.add(btnLocation);
            menuPanel.add(btnPaiement);
            menuPanel.add(btnRetour);
            menuPanel.add(btnEntretien);
            menuPanel.add(btnJournal);
            menuPanel.add(btnRapports);
        } else if (roleActuel.equals("AGENT_LOCATION")) {
            menuPanel.add(btnClients);
            menuPanel.add(btnVoitures);
            menuPanel.add(btnReservation);
            menuPanel.add(btnLocation);
        } else if (roleActuel.equals("AGENT_RETOUR")) {
            menuPanel.add(btnVoitures);
            menuPanel.add(btnRetour);
        } else if (roleActuel.equals("CAISSIER")) {
            menuPanel.add(btnPaiement);
        } else if (roleActuel.equals("RESPONSABLE_ENTRETIEN")) {
            menuPanel.add(btnVoitures);
            menuPanel.add(btnEntretien);
        } else if (roleActuel.equals("GESTIONNAIRE")) {
            menuPanel.add(btnVoitures);
            menuPanel.add(btnJournal);
            menuPanel.add(btnRapports);
        }

        menuPanel.add(btnQuitter);

        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        add(menuPanel, BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        btnClients.addActionListener(e -> gestionClient());
        btnVoitures.addActionListener(e -> consulterVoitures());
        btnReservation.addActionListener(e -> creerReservation());
        btnLocation.addActionListener(e -> creerLocation());
        btnPaiement.addActionListener(e -> gererPaiement());
        btnRetour.addActionListener(e -> enregistrerRetour());
        btnEntretien.addActionListener(e -> suivreEntretien());
        btnJournal.addActionListener(e -> voirJournal());
        btnRapports.addActionListener(e -> afficherRapports());
        btnQuitter.addActionListener(e -> System.exit(0));
    }

    private String choisirRole() {
        String[] roles = {
                "ADMINISTRATEUR",
                "AGENT_LOCATION",
                "AGENT_RETOUR",
                "CAISSIER",
                "RESPONSABLE_ENTRETIEN",
                "GESTIONNAIRE"
        };

        String role = (String) JOptionPane.showInputDialog(
                null,
                "Choisir votre rôle :",
                "Connexion au système",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roles,
                roles[0]
        );

        if (role == null) {
            System.exit(0);
        }

        return role;
    }

    private Connection getConn() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (Exception e) {
            System.out.println("Driver Oracle introuvable.");
        }

        String url = "jdbc:oracle:thin:@//gaia.emp.uqtr.ca:1521/coursbd.uqtr.ca";
        return DriverManager.getConnection(url, "SMI1002_030", "69ajxa84");
    }

    private void gestionClient() {
        String nom = JOptionPane.showInputDialog("Nom :");
        String prenom = JOptionPane.showInputDialog("Prénom :");
        String adresse = JOptionPane.showInputDialog("Adresse :");
        String telephone = JOptionPane.showInputDialog("Téléphone :");
        String email = JOptionPane.showInputDialog("Email :");
        String permis = JOptionPane.showInputDialog("Numéro permis :");
        String expiration = JOptionPane.showInputDialog("Date expiration permis (YYYY-MM-DD) :");

        String sql = """
                INSERT INTO CLIENT
                (NOM, PRENOM, ADRESSE, TELEPHONE, EMAIL, NUMERO_PERMIS, DATE_EXPIRATION_PERMIS)
                VALUES (?, ?, ?, ?, ?, ?, TO_DATE(?,'YYYY-MM-DD'))
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, adresse);
            ps.setString(4, telephone);
            ps.setString(5, email);
            ps.setString(6, permis);
            ps.setString(7, expiration);

            ps.executeUpdate();
            logArea.setText("✅ Client ajouté avec succès.");

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void consulterVoitures() {
        logArea.setText("--- VÉHICULES DISPONIBLES ---\n");

        String sql = """
                SELECT ID_VOITURE, IMMATRICULATION, MARQUE, MODELE, ANNEE, PRIX_JOURNALIER, STATUT
                FROM VOITURE
                WHERE STATUT = 'DISPONIBLE'
                ORDER BY MARQUE, MODELE
                """;

        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logArea.append(
                        rs.getInt("ID_VOITURE") + " | " +
                        rs.getString("IMMATRICULATION") + " | " +
                        rs.getString("MARQUE") + " " +
                        rs.getString("MODELE") + " | " +
                        rs.getInt("ANNEE") + " | " +
                        rs.getDouble("PRIX_JOURNALIER") + "$/jour | " +
                        rs.getString("STATUT") + "\n"
                );
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void creerReservation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        String idE = JOptionPane.showInputDialog("ID Employé :");
        String d1 = JOptionPane.showInputDialog("Début prévu (YYYY-MM-DD) :");
        String d2 = JOptionPane.showInputDialog("Fin prévue (YYYY-MM-DD) :");

        String sql = """
                INSERT INTO RESERVATION
                (DATE_RESERVATION, DATE_DEBUT_PREVUE, DATE_FIN_PREVUE,
                 STATUT_RESERVATION, ID_CLIENT, ID_VOITURE, ID_EMPLOYE)
                VALUES
                (SYSDATE, TO_DATE(?,'YYYY-MM-DD'), TO_DATE(?,'YYYY-MM-DD'),
                 'CONFIRMEE', ?, ?, ?)
                """;

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, d1);
                ps.setString(2, d2);
                ps.setInt(3, Integer.parseInt(idC));
                ps.setInt(4, Integer.parseInt(idV));
                ps.setInt(5, Integer.parseInt(idE));

                ps.executeUpdate();
                conn.commit();

                logArea.setText("✅ Réservation créée avec succès.");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void creerLocation() {
        String idV = JOptionPane.showInputDialog("ID Voiture :");
        String idC = JOptionPane.showInputDialog("ID Client :");
        String idE = JOptionPane.showInputDialog("ID Employé :");
        String dateFin = JOptionPane.showInputDialog("Date fin prévue (YYYY-MM-DD) :");
        String montant = JOptionPane.showInputDialog("Montant total :");

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);

            String lockSql = "SELECT STATUT FROM VOITURE WHERE ID_VOITURE = ? FOR UPDATE";

            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setInt(1, Integer.parseInt(idV));

                ResultSet rs = lockStmt.executeQuery();

                if (rs.next()) {
                    String statut = rs.getString("STATUT");

                    if (statut.equals("DISPONIBLE") || statut.equals("RESERVEE")) {
                        String insertSql = """
                                INSERT INTO LOCATION
                                (DATE_LOCATION, DATE_DEBUT, DATE_FIN_PREVUE,
                                 MONTANT_TOTAL, STATUT_LOCATION, ID_CLIENT, ID_VOITURE, ID_EMPLOYE)
                                VALUES
                                (SYSDATE, SYSDATE, TO_DATE(?,'YYYY-MM-DD'),
                                 ?, 'EN_COURS', ?, ?, ?)
                                """;

                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setString(1, dateFin);
                            ps.setDouble(2, Double.parseDouble(montant));
                            ps.setInt(3, Integer.parseInt(idC));
                            ps.setInt(4, Integer.parseInt(idV));
                            ps.setInt(5, Integer.parseInt(idE));

                            ps.executeUpdate();
                        }

                        conn.commit();
                        logArea.setText("✅ Location enregistrée avec succès.");
                    } else {
                        conn.rollback();
                        logArea.setText("❌ Voiture non disponible pour location.");
                    }
                }

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void gererPaiement() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        String montant = JOptionPane.showInputDialog("Montant :");
        String mode = JOptionPane.showInputDialog("Mode (ESPECES/CARTE/VIREMENT/CHEQUE) :");

        String sql = """
                INSERT INTO PAIEMENT
                (DATE_PAIEMENT, MONTANT, MODE_PAIEMENT, STATUT_PAIEMENT, ID_LOCATION)
                VALUES
                (SYSDATE, ?, ?, 'PAYE', ?)
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, Double.parseDouble(montant));
            ps.setString(2, mode);
            ps.setInt(3, Integer.parseInt(idL));

            ps.executeUpdate();
            logArea.setText("✅ Paiement ajouté avec succès.");

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void enregistrerRetour() {
        String idL = JOptionPane.showInputDialog("ID Location :");
        String km = JOptionPane.showInputDialog("Kilométrage retour :");
        String carburant = JOptionPane.showInputDialog("Niveau carburant :");
        String etat = JOptionPane.showInputDialog("État retour :");
        String frais = JOptionPane.showInputDialog("Frais supplémentaires :");
        String idE = JOptionPane.showInputDialog("ID Employé :");

        String sql = """
                INSERT INTO RETOUR
                (DATE_RETOUR, NIVEAU_CARBURANT, KILOMETRAGE_RETOUR,
                 ETAT_RETOUR, FRAIS_SUPPLEMENTAIRES, ID_LOCATION, ID_EMPLOYE)
                VALUES
                (SYSDATE, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, carburant);
                ps.setDouble(2, Double.parseDouble(km));
                ps.setString(3, etat);
                ps.setDouble(4, Double.parseDouble(frais));
                ps.setInt(5, Integer.parseInt(idL));
                ps.setInt(6, Integer.parseInt(idE));

                ps.executeUpdate();
                conn.commit();

                logArea.setText("✅ Retour enregistré avec succès.");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void suivreEntretien() {
        logArea.setText("--- HISTORIQUE ENTRETIEN ---\n");

        String sql = """
                SELECT E.ID_ENTRETIEN, E.DATE_ENTRETIEN, E.TYPE_ENTRETIEN,
                       E.DESCRIPTION, E.COUT, E.STATUT_ENTRETIEN,
                       V.MARQUE, V.MODELE, V.IMMATRICULATION
                FROM ENTRETIEN E
                JOIN VOITURE V ON E.ID_VOITURE = V.ID_VOITURE
                ORDER BY E.DATE_ENTRETIEN DESC
                """;

        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logArea.append(
                        "ID: " + rs.getInt("ID_ENTRETIEN") +
                        " | Date: " + rs.getDate("DATE_ENTRETIEN") +
                        " | Type: " + rs.getString("TYPE_ENTRETIEN") +
                        " | Coût: " + rs.getDouble("COUT") + "$" +
                        " | Statut: " + rs.getString("STATUT_ENTRETIEN") +
                        " | Voiture: " + rs.getString("MARQUE") + " " +
                        rs.getString("MODELE") + " (" +
                        rs.getString("IMMATRICULATION") + ")\n"
                );
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void voirJournal() {
        logArea.setText("--- JOURNAL DE TRACE ---\n");

        String sql = """
                SELECT *
                FROM JOURNAL_OPERATIONS
                ORDER BY DATE_OPERATION DESC
                """;

        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logArea.append(
                        rs.getString("DATE_OPERATION") + " | " +
                        rs.getString("TYPE_OPERATION") + " | " +
                        rs.getString("NOM_TABLE") + " | " +
                        rs.getString("UTILISATEUR_BD") + "\n"
                );
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    private void afficherRapports() {
        logArea.setText("--- RAPPORTS / STATISTIQUES ---\n\n");

        String sqlVoitures = """
                SELECT STATUT, COUNT(*) AS NOMBRE
                FROM VOITURE
                GROUP BY STATUT
                """;

        String sqlLocations = """
                SELECT STATUT_LOCATION, COUNT(*) AS NOMBRE
                FROM LOCATION
                GROUP BY STATUT_LOCATION
                """;

        String sqlPaiements = """
                SELECT NVL(SUM(MONTANT), 0) AS TOTAL_PAYE
                FROM PAIEMENT
                WHERE STATUT_PAIEMENT = 'PAYE'
                """;

        try (Connection conn = getConn();
             Statement stmt = conn.createStatement()) {

            logArea.append("Nombre de voitures par statut :\n");
            ResultSet rs1 = stmt.executeQuery(sqlVoitures);
            while (rs1.next()) {
                logArea.append("- " + rs1.getString("STATUT") + " : " + rs1.getInt("NOMBRE") + "\n");
            }

            logArea.append("\nNombre de locations par statut :\n");
            ResultSet rs2 = stmt.executeQuery(sqlLocations);
            while (rs2.next()) {
                logArea.append("- " + rs2.getString("STATUT_LOCATION") + " : " + rs2.getInt("NOMBRE") + "\n");
            }

            logArea.append("\nTotal des paiements validés :\n");
            ResultSet rs3 = stmt.executeQuery(sqlPaiements);
            if (rs3.next()) {
                logArea.append(rs3.getDouble("TOTAL_PAYE") + "$\n");
            }

        } catch (SQLException ex) {
            logArea.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AgenceLocationGUI().setVisible(true));
    }
}