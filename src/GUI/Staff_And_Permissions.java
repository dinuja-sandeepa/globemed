/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import Model.MySQL;
import javax.swing.JOptionPane;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Staff_And_Permissions extends javax.swing.JFrame {

    private String userRole;

    public Staff_And_Permissions(String role) {
        this.userRole = role;
        initComponents();
        loadRoles();
        loadDepartments(); // Assuming departments are hardcoded or from DB
        loadStaffRecords(); // Load full table on init
        setupNameFilter();

    }

    //----------------------------------------------------------------------------
    // Interface for staff operations
    interface StaffOperation {

        void addStaff(String name, String username, String password, String department, String role) throws SecurityException;

        void updateStaff(int id, String name, String username, String password, String department, String role) throws SecurityException;

        void deleteStaff(int id) throws SecurityException;

        ResultSet getStaffRecords(String nameFilter) throws SecurityException;
    }

    class BasicStaffOperation implements StaffOperation {

        @Override
        public void addStaff(String name, String username, String password, String department, String role) throws SecurityException {
            try {
                String hashedPassword = hashPassword(password);
                String query = String.format(
                        "INSERT INTO staff (username, password, full_name, role_id, department_id) "
                        + "VALUES ('%s', '%s', '%s', (SELECT id FROM role WHERE role_name = '%s'), "
                        + "(SELECT id FROM department WHERE department_name = '%s'))",
                        username.replace("'", "''"), hashedPassword, name.replace("'", "''"),
                        role.replace("'", "''"), department.replace("'", "''")
                );
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add staff: " + e.getMessage());
            }
        }

        @Override
        public void updateStaff(int id, String name, String username, String password, String department, String role) throws SecurityException {
            try {
                String hashedPassword = hashPassword(password);
                String query = String.format(
                        "UPDATE staff SET username = '%s', password = '%s', full_name = '%s', "
                        + "role_id = (SELECT id FROM role WHERE role_name = '%s'), "
                        + "department_id = (SELECT id FROM department WHERE department_name = '%s') WHERE id = %d",
                        username.replace("'", "''"), hashedPassword, name.replace("'", "''"),
                        role.replace("'", "''"), department.replace("'", "''"), id
                );
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update staff: " + e.getMessage());
            }
        }

        @Override
        public void deleteStaff(int id) throws SecurityException {
            try {
                String query = "DELETE FROM staff WHERE id = " + id;
                MySQL.iud(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete staff: " + e.getMessage());
            }
        }

        @Override
        public ResultSet getStaffRecords(String nameFilter) throws SecurityException {
            try {
                String query = nameFilter.isEmpty()
                        ? "SELECT s.id, s.full_name, r.role_name, d.department_name, s.username, s.password "
                        + "FROM staff s JOIN role r ON s.role_id = r.id JOIN department d ON s.department_id = d.id"
                        : String.format(
                                "SELECT s.id, s.full_name, r.role_name, d.department_name, s.username, s.password "
                                + "FROM staff s JOIN role r ON s.role_id = r.id JOIN department d ON s.department_id = d.id "
                                + "WHERE s.full_name LIKE '%%%s%%'",
                                nameFilter.replace("'", "''")
                        );
                return MySQL.search(query);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to retrieve staff records: " + e.getMessage());
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Hashing failed: " + e.getMessage());
                return password; // Fallback to plain password
            }
        }
    }

    abstract class StaffOperationDecorator implements StaffOperation {

        protected StaffOperation decoratedOperation;

        public StaffOperationDecorator(StaffOperation operation) {
            this.decoratedOperation = operation;
        }

        @Override
        public void addStaff(String name, String username, String password, String department, String role) {
            decoratedOperation.addStaff(name, username, password, department, role);
        }

        @Override
        public void updateStaff(int id, String name, String username, String password, String department, String role) {
            decoratedOperation.updateStaff(id, name, username, password, department, role);
        }

        @Override
        public void deleteStaff(int id) {
            decoratedOperation.deleteStaff(id);
        }

        @Override
        public ResultSet getStaffRecords(String nameFilter) {
            return decoratedOperation.getStaffRecords(nameFilter);
        }
    }

    class AccessControlDecorator extends StaffOperationDecorator {

        private String role;

        public AccessControlDecorator(StaffOperation operation, String role) {
            super(operation);
            this.role = role;
        }

        @Override
        public void addStaff(String name, String username, String password, String department, String role) {
            if (!this.role.equals("Admin")) {
                throw new SecurityException("Only Admins can add staff");
            }
            super.addStaff(name, username, password, department, role);
        }

        @Override
        public void updateStaff(int id, String name, String username, String password, String department, String role) {
            if (!this.role.equals("Admin")) {
                throw new SecurityException("Only Admins can update staff");
            }
            super.updateStaff(id, name, username, password, department, role);
        }

        @Override
        public void deleteStaff(int id) {
            if (!this.role.equals("Admin")) {
                throw new SecurityException("Only Admins can delete staff");
            }
            super.deleteStaff(id);
        }

        @Override
        public ResultSet getStaffRecords(String nameFilter) {
            if (!this.role.equals("Admin")) {
                throw new SecurityException("Only Admins can view staff records");
            }
            return super.getStaffRecords(nameFilter);
        }
    }

    class LoggingDecorator extends StaffOperationDecorator {

        private int staffId;

        public LoggingDecorator(StaffOperation operation, int staffId) {
            super(operation);
            this.staffId = staffId;
        }

        @Override
        public void addStaff(String name, String username, String password, String department, String role) {
            super.addStaff(name, username, password, department, role);
            logAction("Added staff member " + name);
        }

        @Override
        public void updateStaff(int id, String name, String username, String password, String department, String role) {
            super.updateStaff(id, name, username, password, department, role);
            logAction("Updated staff member " + name);
        }

        @Override
        public void deleteStaff(int id) {
            super.deleteStaff(id);
            logAction("Deleted staff ID " + id);
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

    private void loadRoles() {
        try {
            ResultSet rs = MySQL.search("SELECT role_name FROM role");
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select Role");
            while (rs.next()) {
                model.addElement(rs.getString("role_name"));
            }
            jComboBox2.setModel(model);
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading roles: " + e.getMessage());
        }
    }

    private void loadDepartments() {
        try {
            ResultSet rs = MySQL.search("SELECT department_name FROM department");
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select Department");
            while (rs.next()) {
                model.addElement(rs.getString("department_name"));
            }
            jComboBox1.setModel(model);
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading departments: " + e.getMessage());
        }
    }

    private void loadStaffRecords() {
        loadStaffRecords("");
    }

    private void loadStaffRecords(String nameFilter) {
        try {
            StaffOperation operation = new AccessControlDecorator(new LoggingDecorator(new BasicStaffOperation(), getCurrentStaffId()), userRole);
            ResultSet rs = operation.getStaffRecords(nameFilter);
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getString("role_name"),
                    rs.getString("department_name"),
                    rs.getString("username"),
                    "********" // Mask password in UI
                });
            }
            rs.close();
        } catch (SQLException | SecurityException e) {
            JOptionPane.showMessageDialog(this, "Error loading staff records: " + e.getMessage());
        }
    }

    private void setupNameFilter() {
        jTextField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String name = jTextField1.getText().trim();
                loadStaffRecords(name);
            }
        });
    }

    private int getCurrentStaffId() {
        try {
            ResultSet rs = MySQL.search("SELECT id FROM staff WHERE username = '" + userRole + "'");
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get staff ID: " + e.getMessage());
        }
        return 1;
    }

    private int getSelectedStaffId(int row) {
        return (int) jTable1.getValueAt(row, 0);
    }

    private void clearForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jPasswordField1.setText("");
        jComboBox1.setSelectedIndex(0);
        jComboBox2.setSelectedIndex(0);
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
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPasswordField1 = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Staff And Permissions");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(25, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(23, 23, 23))
        );

        jPanel2.setBackground(new java.awt.Color(255, 242, 215));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Add New Staff Member");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Name :");

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField1KeyReleased(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Username :");

        jTextField2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Password :");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Department :");

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jComboBox2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setText("Role :");

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
        jButton3.setText("Remove");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Name", "Role", "Department", "Username", "Password"
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

        jPasswordField1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(77, 77, 77)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(18, 18, 18)
                                .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(35, 35, 35)
                                .addComponent(jTextField2))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGap(33, 33, 33)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 9, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel7))
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jTextField1)
                                            .addComponent(jComboBox2, 0, 246, Short.MAX_VALUE)))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(40, 40, 40)
                                        .addComponent(jPasswordField1)))))
                        .addGap(18, 18, 18)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 849, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(jButton1)
                .addGap(18, 18, 18)
                .addComponent(jButton2)
                .addGap(18, 18, 18)
                .addComponent(jButton3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 555, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(63, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String name = jTextField1.getText();
            String username = jTextField2.getText();
            String password = new String(jPasswordField1.getPassword());
            String department = (String) jComboBox1.getSelectedItem();
            String role = (String) jComboBox2.getSelectedItem();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || "Select Department".equals(department) || "Select Role".equals(role)) {
                JOptionPane.showMessageDialog(this, "Please fill all fields");
                return;
            }

            StaffOperation operation = new AccessControlDecorator(new LoggingDecorator(new BasicStaffOperation(), getCurrentStaffId()), userRole);
            operation.addStaff(name, username, password, department, role);
            JOptionPane.showMessageDialog(this, "Staff added successfully");
            loadStaffRecords();
            clearForm();
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding staff: " + e.getMessage());
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a staff record to update");
                return;
            }

            int id = getSelectedStaffId(selectedRow);
            String name = jTextField1.getText();
            String username = jTextField2.getText();
            String password = new String(jPasswordField1.getPassword());
            String department = (String) jComboBox1.getSelectedItem();
            String role = (String) jComboBox2.getSelectedItem();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || "Select Department".equals(department) || "Select Role".equals(role)) {
                JOptionPane.showMessageDialog(this, "Please fill all fields");
                return;
            }

            StaffOperation operation = new AccessControlDecorator(new LoggingDecorator(new BasicStaffOperation(), getCurrentStaffId()), userRole);
            operation.updateStaff(id, name, username, password, department, role);
            JOptionPane.showMessageDialog(this, "Staff updated successfully");
            loadStaffRecords();
            clearForm();
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating staff: " + e.getMessage());
            //System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a staff record to remove");
                return;
            }

            int id = getSelectedStaffId(selectedRow);
            StaffOperation operation = new AccessControlDecorator(new LoggingDecorator(new BasicStaffOperation(), getCurrentStaffId()), userRole);
            operation.deleteStaff(id);
            JOptionPane.showMessageDialog(this, "Staff removed successfully");
            loadStaffRecords();
            clearForm();
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error removing staff: " + e.getMessage());
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        if (evt.getClickCount() == 2) {
            int row = jTable1.getSelectedRow();
            if (row != -1) {
                jTextField1.setText(jTable1.getValueAt(row, 1).toString());
                jTextField2.setText(jTable1.getValueAt(row, 4).toString());
                jPasswordField1.setText("");
                jComboBox1.setSelectedItem(jTable1.getValueAt(row, 3).toString());
                jComboBox2.setSelectedItem(jTable1.getValueAt(row, 2).toString());
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTextField1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1KeyReleased

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
            java.util.logging.Logger.getLogger(Staff_And_Permissions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Staff_And_Permissions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Staff_And_Permissions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Staff_And_Permissions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        FlatMacLightLaf.setup();


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new Staff_And_Permissions("Admin").setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
