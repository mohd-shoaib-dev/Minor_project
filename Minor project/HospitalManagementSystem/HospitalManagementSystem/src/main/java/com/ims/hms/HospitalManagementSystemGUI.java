package com.ims.hms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * College-level Hospital Management System focused on OPD patients, doctors,
 * appointments, login and role-based permissions.
 */
public class HospitalManagementSystemGUI extends JFrame {
    private final String username;
    private final String role;
    private final JTable table = new JTable();
    private final JLabel status = new JLabel("  Choose an action to get started.");

    private HospitalManagementSystemGUI(String username, String role) {
        this.username = username;
        this.role = role;
        setTitle("Hospital Management System | " + username + " (" + role + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 570);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 248, 252));
        buildUi();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginResult login = login();
            if (login != null) new HospitalManagementSystemGUI(login.username, login.role).setVisible(true);
            else System.exit(0);
        });
    }

    private void buildUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        top.setBackground(new Color(226, 237, 248));
        JButton add = button("Add Patient");
        JButton patients = button("View Patients");
        JButton doctors = button("View Doctors");
        JButton book = button("Book Appointment");
        JButton history = button("Patient History");
        JButton remove = button("Remove Patient");
        JButton logout = button("Logout");
        top.add(add); top.add(patients); top.add(doctors); top.add(book);
        top.add(history); top.add(remove); top.add(logout);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(status, BorderLayout.SOUTH);

        boolean canWrite = role.equals("admin") || role.equals("reception");
        add.setEnabled(canWrite);
        book.setEnabled(canWrite);
        remove.setEnabled(role.equals("admin"));

        add.addActionListener(e -> addPatient());
        patients.addActionListener(e -> loadPatients());
        doctors.addActionListener(e -> loadDoctors());
        book.addActionListener(e -> bookAppointment());
        history.addActionListener(e -> patientHistory());
        remove.addActionListener(e -> removePatient());
        logout.addActionListener(e -> {
            dispose();
            LoginResult next = login();
            if (next != null) new HospitalManagementSystemGUI(next.username, next.role).setVisible(true);
            else System.exit(0);
        });
        loadPatients();
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }

    private static LoginResult login() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            JTextField user = new JTextField();
            JPasswordField pass = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
            panel.add(new JLabel("Username")); panel.add(user);
            panel.add(new JLabel("Password")); panel.add(pass);
            int result = JOptionPane.showConfirmDialog(null, panel,
                    "Hospital Management System — Login (" + attempt + "/3)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return null;
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT role FROM users WHERE username=? AND password_hash=SHA2(?,256)")) {
                ps.setString(1, user.getText().trim());
                ps.setString(2, new String(pass.getPassword()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new LoginResult(user.getText().trim(), rs.getString("role"));
                }
                JOptionPane.showMessageDialog(null, "Invalid username or password.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Database connection failed. Check MySQL and database setup.\n" + ex.getMessage());
                return null;
            }
        }
        JOptionPane.showMessageDialog(null, "Three unsuccessful attempts. Application will close.");
        return null;
    }

    private void addPatient() {
        JTextField name = new JTextField();
        JTextField age = new JTextField();
        JComboBox<String> gender = new JComboBox<>(new String[]{"Female", "Male", "Other", "Prefer not to say"});
        JPanel p = form(new String[]{"Patient name", "Age", "Gender"}, name, age, gender);
        if (JOptionPane.showConfirmDialog(this, p, "Register Patient", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            String n = name.getText().trim();
            int a = Integer.parseInt(age.getText().trim());
            if (n.isEmpty() || a < 0 || a > 120) throw new IllegalArgumentException();
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO patients(name,age,gender) VALUES(?,?,?)")) {
                ps.setString(1, n); ps.setInt(2, a); ps.setString(3, (String) gender.getSelectedItem());
                ps.executeUpdate();
            }
            message("Patient registered successfully."); loadPatients();
        } catch (IllegalArgumentException ex) { message("Enter a name and an age from 0 to 120."); }
        catch (SQLException ex) { error("Could not register patient", ex); }
    }

    private void loadPatients() {
        showQuery("SELECT id AS 'Patient ID', name AS 'Name', age AS 'Age', gender AS 'Gender' FROM patients ORDER BY id",
                "Patient records loaded.");
    }

    private void loadDoctors() {
        showQuery("SELECT id AS 'Doctor ID', name AS 'Doctor', specialization AS 'Specialization' FROM doctors ORDER BY id",
                "Doctor list loaded.");
    }

    private void bookAppointment() {
        JTextField patientId = new JTextField();
        JTextField doctorId = new JTextField();
        JTextField date = new JTextField(LocalDate.now().toString());
        JPanel p = form(new String[]{"Patient ID", "Doctor ID", "Date (YYYY-MM-DD)"}, patientId, doctorId, date);
        if (JOptionPane.showConfirmDialog(this, p, "Book Appointment", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            int pid = Integer.parseInt(patientId.getText().trim());
            int did = Integer.parseInt(doctorId.getText().trim());
            LocalDate.parse(date.getText().trim());
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    if (!exists(c, "patients", pid) || !exists(c, "doctors", did)) {
                        c.rollback(); message("Patient ID or Doctor ID does not exist."); return;
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO appointments(patient_id,doctor_id,appointment_date) VALUES(?,?,?)")) {
                        ps.setInt(1, pid); ps.setInt(2, did); ps.setDate(3, Date.valueOf(date.getText().trim()));
                        ps.executeUpdate();
                    }
                    c.commit();
                    message("Appointment booked successfully."); loadAppointments();
                } catch (SQLException ex) {
                    c.rollback();
                    if (ex.getErrorCode() == 1062) message("This doctor already has an appointment on that date.");
                    else error("Could not book appointment", ex);
                } finally { c.setAutoCommit(true); }
            }
        } catch (NumberFormatException | DateTimeParseException ex) {
            message("Enter valid numeric IDs and a date in YYYY-MM-DD format.");
        } catch (SQLException ex) { error("Database operation failed", ex); }
    }

    private boolean exists(Connection c, String tableName, int id) throws SQLException {
        // Table names are selected internally from fixed values, never user input.
        String sql = "SELECT 1 FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void loadAppointments() {
        showQuery("SELECT a.id AS 'Appointment ID', p.name AS 'Patient', d.name AS 'Doctor', " +
                "d.specialization AS 'Specialization', a.appointment_date AS 'Date' " +
                "FROM appointments a JOIN patients p ON p.id=a.patient_id " +
                "JOIN doctors d ON d.id=a.doctor_id ORDER BY a.appointment_date",
                "Appointment schedule loaded.");
    }

    private void patientHistory() {
        String value = JOptionPane.showInputDialog(this, "Enter Patient ID:");
        if (value == null) return;
        try {
            int id = Integer.parseInt(value.trim());
            String sql = "SELECT a.id AS 'Appointment ID', p.name AS 'Patient', d.name AS 'Doctor', " +
                    "d.specialization AS 'Specialization', a.appointment_date AS 'Date' " +
                    "FROM appointments a JOIN patients p ON p.id=a.patient_id " +
                    "JOIN doctors d ON d.id=a.doctor_id WHERE p.id=? ORDER BY a.appointment_date";
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) { fillTable(rs); }
            }
            status.setText("  Appointment history for patient ID " + id);
        } catch (NumberFormatException ex) { message("Patient ID must be a number."); }
        catch (SQLException ex) { error("Could not load patient history", ex); }
    }

    private void removePatient() {
        String value = JOptionPane.showInputDialog(this, "Enter Patient ID to remove:");
        if (value == null) return;
        try {
            int id = Integer.parseInt(value.trim());
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete this patient and all linked appointments? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try (PreparedStatement a = c.prepareStatement("DELETE FROM appointments WHERE patient_id=?");
                     PreparedStatement p = c.prepareStatement("DELETE FROM patients WHERE id=?")) {
                    a.setInt(1, id); a.executeUpdate();
                    p.setInt(1, id);
                    int rows = p.executeUpdate();
                    c.commit();
                    message(rows > 0 ? "Patient and linked appointments removed." : "Patient ID not found.");
                } catch (SQLException ex) { c.rollback(); throw ex; }
                finally { c.setAutoCommit(true); }
            }
            loadPatients();
        } catch (NumberFormatException ex) { message("Patient ID must be a number."); }
        catch (SQLException ex) { error("Could not remove patient", ex); }
    }

    private JPanel form(String[] labels, JComponent... fields) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) { p.add(new JLabel(labels[i])); p.add(fields[i]); }
        return p;
    }

    private void showQuery(String sql, String success) {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            fillTable(rs); status.setText("  " + success);
        } catch (SQLException ex) { error("Could not retrieve records", ex); }
    }

    private void fillTable(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        String[] columns = new String[md.getColumnCount()];
        for (int i = 0; i < columns.length; i++) columns[i] = md.getColumnLabel(i + 1);
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        while (rs.next()) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) row[i] = rs.getObject(i + 1);
            model.addRow(row);
        }
        table.setModel(model);
    }

    private void message(String text) { JOptionPane.showMessageDialog(this, text); status.setText("  " + text); }
    private void error(String text, Exception ex) {
        JOptionPane.showMessageDialog(this, text + ". Check database setup and logs.\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private record LoginResult(String username, String role) {}
}
