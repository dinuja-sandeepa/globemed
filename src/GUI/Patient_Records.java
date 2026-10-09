package GUI;

import javax.swing.JOptionPane;
import Model.MySQL;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Patient_Records extends javax.swing.JFrame {

    private String userRole;
    private String name;
    private int selectedPatientId = -1; // Track selected patient ID

    public Patient_Records(String role, String name) {
        this.userRole = role;
        this.name = name;
        initComponents();
        loadPatientRecords(); // Load all records by default
        setupNameFilter();   // Setup name filter listener
    }

    // Interface for patient record operations
    interface PatientRecord {

        void addRecord(String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) throws SecurityException;

        void updateRecord(int id, String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) throws SecurityException;

        void deleteRecord(int id) throws SecurityException;

        ResultSet getRecords(String nameFilter) throws SecurityException;

        ResultSet getPatientByName(String name) throws SecurityException;
    }

    // Concrete class for basic patient record operations
    class BasicPatientRecord implements PatientRecord {

        public void addRecord(String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) throws SecurityException {
            try {
                String query = String.format(
                        "INSERT INTO patient (name, age, gender, contact, medical_history, treatment_plan) VALUES ('%s', %d, '%s', '%s', '%s', '%s')",
                        name.replace("'", "''"), age, gender, contact.replace("'", "''"), medicalHistory.replace("'", "''"), treatmentPlan.replace("'", "''")
                );
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add patient record: " + e.getMessage());
            }
        }

        public void updateRecord(int id, String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) throws SecurityException {
            try {
                String query = String.format(
                        "UPDATE patient SET name = '%s', age = %d, gender = '%s', contact = '%s', medical_history = '%s', treatment_plan = '%s' WHERE id = %d",
                        name.replace("'", "''"), age, gender, contact.replace("'", "''"), medicalHistory.replace("'", "''"), treatmentPlan.replace("'", "''"), id
                );
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update patient record: " + e.getMessage());
            }
        }

        @Override
        public void deleteRecord(int id) throws SecurityException {
            try {
                // Delete dependent records first to avoid foreign key constraint violations
                MySQL.iud("DELETE FROM billing WHERE patient_id = " + id);
                MySQL.iud("DELETE FROM report WHERE patient_id = " + id);
                MySQL.iud("DELETE FROM transaction WHERE patient_id = " + id);
                // Note: Appointment table does not directly reference patient_id, so no deletion needed
                MySQL.iud("DELETE FROM patient WHERE id = " + id);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete patient record: " + e.getMessage());
            }
        }

        @Override
        public ResultSet getRecords(String nameFilter) throws SecurityException {
            try {
                String query = nameFilter.isEmpty()
                        ? "SELECT id, name, age, gender, contact, medical_history, treatment_plan FROM patient"
                        : String.format(
                                "SELECT id, name, age, gender, contact, medical_history, treatment_plan FROM patient WHERE name LIKE '%%%s%%'",
                                nameFilter.replace("'", "''")
                        );
                return MySQL.search(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to retrieve patient records: " + e.getMessage());
            }
        }

        @Override
        public ResultSet getPatientByName(String name) throws SecurityException {
            try {
                String query = String.format(
                        "SELECT id, name, age, gender, contact, medical_history, treatment_plan "
                        + "FROM patient WHERE name = '%s'",
                        name.replace("'", "''")
                );
                return MySQL.search(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to retrieve patient by name: " + e.getMessage());
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
        public void addRecord(String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            decoratedRecord.addRecord(name, age, gender, contact, medicalHistory, treatmentPlan);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            decoratedRecord.updateRecord(id, name, age, gender, contact, medicalHistory, treatmentPlan);
        }

        @Override
        public void deleteRecord(int id) {
            decoratedRecord.deleteRecord(id);
        }

        @Override
        public ResultSet getRecords(String nameFilter) {
            return decoratedRecord.getRecords(nameFilter);
        }

        @Override
        public ResultSet getPatientByName(String name) {
            return decoratedRecord.getPatientByName(name);
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
        public void addRecord(String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            if (!role.equals("Doctor") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors and Admins can add patient records");
            }
            super.addRecord(name, age, gender, contact, medicalHistory, treatmentPlan);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            if (!role.equals("Doctor") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors and Admins can update patient records");
            }
            super.updateRecord(id, name, age, gender, contact, medicalHistory, treatmentPlan);
        }

        @Override
        public void deleteRecord(int id) {
            if (!role.equals("Admin")) {
                throw new SecurityException("Only Admins can delete patient records");
            }
            super.deleteRecord(id);
        }

        @Override
        public ResultSet getRecords(String nameFilter) {
            if (!role.equals("Doctor") && !role.equals("Nurse") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors, Nurses, and Admins can view patient records");
            }
            return super.getRecords(nameFilter);
        }

        @Override
        public ResultSet getPatientByName(String name) {
            if (!role.equals("Doctor") && !role.equals("Nurse") && !role.equals("Admin")) {
                throw new SecurityException("Only Doctors, Nurses, and Admins can view patient records");
            }
            return super.getPatientByName(name);
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
        public void addRecord(String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            super.addRecord(name, age, gender, contact, medicalHistory, treatmentPlan);
            logAction("Added patient record for " + name);
        }

        @Override
        public void updateRecord(int id, String name, int age, String gender, String contact, String medicalHistory, String treatmentPlan) {
            super.updateRecord(id, name, age, gender, contact, medicalHistory, treatmentPlan);
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

    private void loadPatientRecords() {
        loadPatientRecords("");
    }

    private void loadPatientRecords(String nameFilter) {
        try {
            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            ResultSet rs = record.getRecords(nameFilter);
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0); // Clear existing rows
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
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

    private void setupNameFilter() {
        jTextField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String name = jTextField1.getText().trim();
                loadPatientRecords(name);
            }
        });
    }

    private int getStaffId() {
        try {
            ResultSet rs = MySQL.search("SELECT id FROM staff WHERE username = '" + name.replace("'", "''") + "'");
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

    private int getSelectedPatientId(int row) {
        return (int) jTable1.getValueAt(row, 0); // Patient ID is now in the first column
    }

    private void clearForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextArea1.setText("");
        jTextArea2.setText("");
        jComboBox1.setSelectedIndex(0); // Reset to "Select Gender"
        selectedPatientId = -1; // Reset selected patient ID
        jTable1.clearSelection(); // Clear table selection
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
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
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1280, 870));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel2.setText("Patient Records");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(21, 21, 21))
        );

        jPanel4.setBackground(new java.awt.Color(255, 242, 215));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Name :");

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Age :");

        jTextField2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Gender :");

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select Gender", "Male", "Female" }));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Medical History :");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Treatment Plan :");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane3.setViewportView(jTextArea2);

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setText("Add");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton2.setText("Update");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton3.setText("Delete");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Patient ID", "Name", "Age", "Gender", "Medical History", "Treatment Plan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
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

        jLabel7.setFont(new java.awt.Font("Sitka Small", 1, 18)); // NOI18N
        jLabel7.setText("Personal Details");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setText("Contact :");

        jTextField3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel1))
                                .addComponent(jLabel4))
                            .addGap(19, 19, 19)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(jSeparator1)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addGap(86, 86, 86)
                            .addComponent(jLabel7))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel8)
                            .addGap(18, 18, 18)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 335, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel6))
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
                .addGap(10, 10, 10))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 98, Short.MAX_VALUE)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jButton2)
                            .addComponent(jButton3))
                        .addGap(14, 14, 14))
                    .addComponent(jScrollPane1))
                .addGap(23, 23, 23))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String name = jTextField1.getText().trim();
            String ageText = jTextField2.getText().trim();
            String gender = (String) jComboBox1.getSelectedItem();
            String contact = jTextField3.getText().trim();
            String medicalHistory = jTextArea1.getText().trim();
            String treatmentPlan = jTextArea2.getText().trim();

            if (name.isEmpty() || gender.equals("Select Gender") || medicalHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields (Name, Gender, Medical History)");
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageText);
                if (age <= 0) {
                    JOptionPane.showMessageDialog(this, "Age must be a positive number");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid age format");
                return;
            }

            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.addRecord(name, age, gender, contact, medicalHistory, treatmentPlan);
            JOptionPane.showMessageDialog(this, "Patient record added successfully");
            loadPatientRecords();
            clearForm();
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding patient record: " + e.getMessage());
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        try {
            if (selectedPatientId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a patient record to update");
                return;
            }

            String name = jTextField1.getText().trim();
            String ageText = jTextField2.getText().trim();
            String gender = (String) jComboBox1.getSelectedItem();
            String contact = jTextField3.getText().trim();
            String medicalHistory = jTextArea1.getText().trim();
            String treatmentPlan = jTextArea2.getText().trim();

            if (name.isEmpty() || gender.equals("Select Gender") || medicalHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields (Name, Gender, Medical History)");
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageText);
                if (age <= 0) {
                    JOptionPane.showMessageDialog(this, "Age must be a positive number");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid age format");
                return;
            }

            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.updateRecord(selectedPatientId, name, age, gender, contact, medicalHistory, treatmentPlan);
            JOptionPane.showMessageDialog(this, "Patient record updated successfully");
            loadPatientRecords();
            clearForm();
            selectedPatientId = -1; // Reset after update
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating patient record: " + e.getMessage());
        }

    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a patient record to delete");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this patient record? This will also delete related billing, reports, and transactions.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            int id = getSelectedPatientId(selectedRow);
            PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
            record.deleteRecord(id);
            JOptionPane.showMessageDialog(this, "Patient record deleted successfully");
            loadPatientRecords();
            clearForm();
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting patient record: " + e.getMessage());
        }

    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        if (evt.getClickCount() == 2) {
            int row = jTable1.getSelectedRow();
            if (row != -1) {
                try {
                    selectedPatientId = getSelectedPatientId(row); // Store the selected patient ID
                    jTable1.setRowSelectionInterval(row, row); // Ensure row remains selected
                    PatientRecord record = new AccessControlDecorator(new LoggingDecorator(new BasicPatientRecord(), getStaffId()), userRole);
                    ResultSet rs = MySQL.search("SELECT id, name, age, gender, contact, medical_history, treatment_plan FROM patient WHERE id = " + selectedPatientId);
                    if (rs.next()) {
                        jTextField1.setText(rs.getString("name"));
                        jTextField2.setText(String.valueOf(rs.getInt("age")));
                        jComboBox1.setSelectedItem(rs.getString("gender"));
                        jTextField3.setText(rs.getString("contact") != null ? rs.getString("contact") : "");
                        jTextArea1.setText(rs.getString("medical_history") != null ? rs.getString("medical_history") : "");
                        jTextArea2.setText(rs.getString("treatment_plan") != null ? rs.getString("treatment_plan") : "");
                    }
                    rs.close();
                } catch (SQLException | SecurityException e) {
                    JOptionPane.showMessageDialog(this, "Error loading patient details: " + e.getMessage());
                }
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

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
            java.util.logging.Logger.getLogger(Patient_Records.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Patient_Records.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Patient_Records.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Patient_Records.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        FlatMacLightLaf.setup();


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Patient_Records("Admin", "Admin User").setVisible(true);
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
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables
}
