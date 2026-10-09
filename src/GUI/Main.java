/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javax.swing.JOptionPane;
import Model.MySQL;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Dinuja
 */
public class Main extends javax.swing.JFrame {

    private String userRole;

    /**
     * Creates new form Main
     */
    public Main(String role) {
        this.userRole = role;
        initComponents();
        loadRoleBasedWindow(role);
        loadPatientRecords();
    }

    private void loadRoleBasedWindow(String role) {
        // Role-based tab access
        switch (role) {
            case "Doctor":
                jTabbedPane2.setEnabledAt(3, false); // Reports
                jTabbedPane2.setEnabledAt(4, false); // Staff & Security
                break;
            case "Nurse":
                jTabbedPane2.setEnabledAt(1, false); // Appointments
                jTabbedPane2.setEnabledAt(2, false); // Billing
                jTabbedPane2.setEnabledAt(3, false); // Reports
                jTabbedPane2.setEnabledAt(4, false); // Staff
                break;
            case "Pharmacist":
                jTabbedPane2.setEnabledAt(1, false); // Appointments
                jTabbedPane2.setEnabledAt(3, false); // Reports
                jTabbedPane2.setEnabledAt(4, false); // Staff
                break;
            case "Admin":
                // Admin sees all tabs
                break;
            default:
                JOptionPane.showMessageDialog(this, "Invalid role: " + role);
                this.dispose();
                new Login().setVisible(true);
                break;
        }
    }

    //----------------------------------------------------------------------------
    // Interface for patient record operations
    interface PatientRecord {

        void addRecord(String name, int age, String gender, String medicalHistory, String treatmentPlan) throws SecurityException;

        void updateRecord(int id, String name, int age, String gender, String medicalHistory, String treatmentPlan) throws SecurityException;

        void deleteRecord(int id) throws SecurityException;

        ResultSet getRecords() throws SecurityException;
    }

// Concrete class for basic patient record operations
    class BasicPatientRecord implements PatientRecord {

        @Override
        public void addRecord(String name, int age, String gender, String medicalHistory, String treatmentPlan) throws SecurityException {
            try {
                String query = String.format("INSERT INTO patient (name, age, gender, medical_history, treatment_plan) VALUES ('%s', %d, '%s', '%s', '%s')",
                        name.replace("'", "''"), age, gender, medicalHistory.replace("'", "''"), treatmentPlan.replace("'", "''"));
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add patient record: " + e.getMessage());
            }
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String medicalHistory, String treatmentPlan) throws SecurityException {
            try {
                String query = String.format("UPDATE patient SET name = '%s', age = %d, gender = '%s', medical_history = '%s', treatment_plan = '%s' WHERE id = %d",
                        name.replace("'", "''"), age, gender, medicalHistory.replace("'", "''"), treatmentPlan.replace("'", "''"), id);
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update patient record: " + e.getMessage());
            }
        }

        @Override
        public void deleteRecord(int id) throws SecurityException {
            try {
                String query = "DELETE FROM patient WHERE id = " + id;
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete patient record: " + e.getMessage());
            }
        }

        @Override
        public ResultSet getRecords() throws SecurityException {
            try {
                return MySQL.search("SELECT id, name, age, gender, medical_history, treatment_plan FROM patient");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to retrieve patient records: " + e.getMessage());
            }
        }
    }

// Abstract decorator class
    abstract class PatientRecordDecorator implements PatientRecord {

        protected PatientRecord decoratedRecord;

        public PatientRecordDecorator(PatientRecord record) {
            this.decoratedRecord = record;
        }

        @Override
        public void addRecord(String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            decoratedRecord.addRecord(name, age, gender, medicalHistory, treatmentPlan);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            decoratedRecord.updateRecord(id, name, age, gender, medicalHistory, treatmentPlan);
        }

        @Override
        public void deleteRecord(int id) {
            decoratedRecord.deleteRecord(id);
        }

        @Override
        public ResultSet getRecords() {
            return decoratedRecord.getRecords();
        }
    }

// Decorator for role-based access control
    class AccessControlDecorator extends PatientRecordDecorator {

        private String role;

        public AccessControlDecorator(PatientRecord record, String role) {
            super(record);
            this.role = role;
        }

        @Override
        public void addRecord(String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            if (!role.equals("Doctor") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors and Admins can add patient records");
            }
            super.addRecord(name, age, gender, medicalHistory, treatmentPlan);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            if (!role.equals("Doctor") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors and Admins can update patient records");
            }
            super.updateRecord(id, name, age, gender, medicalHistory, treatmentPlan);
        }

        @Override
        public void deleteRecord(int id) {
            if (!role.equals("Admin")) {
                throw new SecurityException("Only Admins can delete patient records");
            }
            super.deleteRecord(id);
        }

        @Override
        public ResultSet getRecords() {
            if (!role.equals("Doctor") && !role.equals("Nurse") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors, Nurses, and Admins can view patient records");
            }
            return super.getRecords();
        }
    }

// Decorator for logging activities
    class LoggingDecorator extends PatientRecordDecorator {

        private int staffId;

        public LoggingDecorator(PatientRecord record, int staffId) {
            super(record);
            this.staffId = staffId;
        }

        @Override
        public void addRecord(String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            super.addRecord(name, age, gender, medicalHistory, treatmentPlan);
            logAction("Added patient record for " + name);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String medicalHistory, String treatmentPlan) {
            super.updateRecord(id, name, age, gender, medicalHistory, treatmentPlan);
            logAction("Updated patient record for " + name);
        }

        @Override
        public void deleteRecord(int id) {
            super.deleteRecord(id);
            logAction("Deleted patient record ID " + id);
        }

        private void logAction(String action) {
            try {
                String query = String.format("INSERT INTO activity_log (staff_id, action) VALUES (%d, '%s')", staffId, action.replace("'", "''"));
                MySQL.iud(query);
            } catch (SQLException e) {
                System.err.println("Failed to log action: " + e.getMessage());
            }
        }
    }

// Method to load patient records into jTable1
    private void loadPatientRecords() {
        try {
            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            ResultSet rs = record.getRecords();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0); // Clear existing rows
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("age"),
                    rs.getString("gender"),
                    rs.getString("medical_history"),
                    rs.getString("treatment_plan")
                });
            }
            rs.close();
        } catch (SQLException | SecurityException e) {
            JOptionPane.showMessageDialog(this, "Error loading patient records: " + e.getMessage());
        }
    }

// Helper method to get staff ID (placeholder, assumes username stored role)
    private int getStaffId() {
        try {
            ResultSet rs = MySQL.search("SELECT id FROM staff WHERE username = '" + userRole + "'"); // Replace with actual username
            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                return id;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Failed to get staff ID: " + e.getMessage());
        }
        return 1; // Fallback ID
    }

    // Helper method to get patient ID from selected row (assumes ID is retrieved via query)
    private int getSelectedPatientId(int row) {
        try {
            String name = (String) jTable1.getValueAt(row, 0);
            ResultSet rs = MySQL.search("SELECT id FROM patient WHERE name = '" + name.replace("'", "''") + "'");
            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                return id;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Failed to get patient ID: " + e.getMessage());
        }
        return -1; // Fallback ID
    }

// Helper method to clear form fields
    private void clearForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jComboBox1.setSelectedIndex(0);
        jTextArea1.setText("");
        jTextArea2.setText("");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel2.setText("Welcome User");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(323, 323, 323)
                .addComponent(jLabel2)
                .addContainerGap(357, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(197, 197, 197)
                .addComponent(jLabel2)
                .addContainerGap(310, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Dashboard", jPanel1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 844, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 539, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Appointments", jPanel4);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 844, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 539, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Billing & Insurance", jPanel5);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 844, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 539, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Reports", jPanel6);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 844, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 539, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Staff & Security", jPanel7);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Name :");

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Age :");

        jTextField2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Gender :");

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select Gender", "Male", "Female" }));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Medical History :");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("Treatment Plan :");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane3.setViewportView(jTextArea2);

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton1.setText("Add");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setText("Update");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton3.setText("Delete");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel5))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(36, 36, 36))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Age", "Gender", "Medical History", "Treatment Plan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, true, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Patient Records", jPanel2);

        jMenu1.setText("File");

        jMenuItem1.setText("Logout");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText(" Exit");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Modules");

        jMenuItem3.setText("Patient Records");
        jMenu2.add(jMenuItem3);

        jMenuItem4.setText("Appointments");
        jMenu2.add(jMenuItem4);

        jMenuItem5.setText("Billing");
        jMenu2.add(jMenuItem5);

        jMenuItem6.setText("Reports");
        jMenu2.add(jMenuItem6);

        jMenuItem7.setText("Staff");
        jMenu2.add(jMenuItem7);

        jMenuBar1.add(jMenu2);

        jMenu3.setText(" Help");

        jMenuItem8.setText("About");
        jMenu3.add(jMenuItem8);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    //Update Button
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a patient record to update");
                return;
            }

            int id = getSelectedPatientId(selectedRow); // Assume ID is stored in table model or retrieved
            String name = jTextField1.getText();
            int age = Integer.parseInt(jTextField2.getText());
            String gender = (String) jComboBox1.getSelectedItem();
            String medicalHistory = jTextArea1.getText();
            String treatmentPlan = jTextArea2.getText();

            if (name.isEmpty() || gender.equals("Select Gender") || medicalHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields");
                return;
            }

            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.updateRecord(id, name, age, gender, medicalHistory, treatmentPlan);
            JOptionPane.showMessageDialog(this, "Patient record updated successfully");
            loadPatientRecords(); // Refresh table
            clearForm(); // Clear input fields
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid age format");
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating patient record: " + e.getMessage());
        }

        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed

        this.dispose();
        new Login().setVisible(true);
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed

        System.exit(0);
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    //Add Button
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String name = jTextField1.getText();
            int age = Integer.parseInt(jTextField2.getText());
            String gender = (String) jComboBox1.getSelectedItem();
            String medicalHistory = jTextArea1.getText();
            String treatmentPlan = jTextArea2.getText();

            if (name.isEmpty() || gender.equals("Select Gender") || medicalHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields");
                return;
            }

            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.addRecord(name, age, gender, medicalHistory, treatmentPlan);
            JOptionPane.showMessageDialog(this, "Patient record added successfully");
            loadPatientRecords(); // Refresh table
            clearForm(); // Clear input fields
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid age format");
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding patient record: " + e.getMessage());
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    //Delete
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a patient record to delete");
                return;
            }

            int id = getSelectedPatientId(selectedRow); // Assume ID is stored in table model or retrieved
            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.deleteRecord(id);
            JOptionPane.showMessageDialog(this, "Patient record deleted successfully");
            loadPatientRecords(); // Refresh table
            clearForm(); // Clear input fields
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting patient record: " + e.getMessage());
        }

// TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        if (evt.getClickCount() == 2) { // Double-click detected
            int row = jTable1.getSelectedRow();
            if (row != -1) {
                // Populate form fields with table row data
                jTextField1.setText(jTable1.getValueAt(row, 0).toString());
                jTextField2.setText(jTable1.getValueAt(row, 1).toString());
                jComboBox1.setSelectedItem(jTable1.getValueAt(row, 2).toString());
                jTextArea1.setText(jTable1.getValueAt(row, 3).toString());
                jTextArea2.setText(jTable1.getValueAt(row, 4).toString());
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jTable1MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        FlatMacLightLaf.setup();

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // new Main().setVisible(true);

            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
