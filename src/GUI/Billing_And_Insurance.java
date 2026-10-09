/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import Model.MySQL;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

// Handler interface for Chain of Responsibility
interface BillingHandler {
    void setNext(BillingHandler next);
    boolean handle(BillingRequest request);
}

// Billing request data structure
class BillingRequest {

    String patientId;
    String patientName;
    String serviceType;
    double serviceCost;
    boolean useInsurance;
    String paymentMethod;

    public BillingRequest(String patientId, String serviceType, double serviceCost, boolean useInsurance, String paymentMethod) {
        this.patientId = patientId;
        this.serviceType = serviceType;
        this.serviceCost = serviceCost;
        this.useInsurance = useInsurance;
        this.paymentMethod = paymentMethod;
    }
}

// Concrete handler: Validates patient ID and fetches name
class PatientValidator implements BillingHandler {

    private BillingHandler next;

    @Override
    public void setNext(BillingHandler next) {
        this.next = next;
    }

    @Override
    public boolean handle(BillingRequest request) {
        try {
            ResultSet rs = MySQL.search("SELECT name FROM patient WHERE id = " + request.patientId);
            if (rs.next()) {
                request.patientName = rs.getString("name");
                return next != null ? next.handle(request) : true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid Patient ID", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error validating patient: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}

// Concrete handler: Validates service type and cost
class ServiceValidator implements BillingHandler {

    private BillingHandler next;

    @Override
    public void setNext(BillingHandler next) {
        this.next = next;
    }

    @Override
    public boolean handle(BillingRequest request) {
        try {
            ResultSet rs = MySQL.search("SELECT default_amount FROM service_type WHERE service_name = '" + request.serviceType.replace("'", "''") + "'");
            if (rs.next()) {
                request.serviceCost = rs.getDouble("default_amount");
                return next != null ? next.handle(request) : true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid Service Type", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error validating service: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}

// Concrete handler: Saves billing to database
class BillingSaver implements BillingHandler {

    private BillingHandler next;

    @Override
    public void setNext(BillingHandler next) {
        this.next = next;
    }

    @Override
    public boolean handle(BillingRequest request) {
        try {
            // Determine claim status based on insurance usage
            int claimStatusId = request.useInsurance ? 2 : 1; // 2 = Pending, 1 = Completed

            // Get payment method ID
            ResultSet rs = MySQL.search("SELECT id FROM payment_method WHERE method = '" + request.paymentMethod.replace("'", "''") + "'");
            int paymentMethodId = rs.next() ? rs.getInt("id") : -1;
            if (paymentMethodId == -1) {
                throw new SQLException("Invalid payment method");
            }

            // Insert into billing
            String billingQuery = String.format(
                    "INSERT INTO billing (patient_id, total_amount, claim_status_id, payment_method_id) VALUES (%s, %.2f, %d, %d)",
                    request.patientId, request.serviceCost, claimStatusId, paymentMethodId
            );
            MySQL.iud(billingQuery);

            // Get last billing_id
            rs = MySQL.search("SELECT LAST_INSERT_ID() as id");
            int billingId = rs.next() ? rs.getInt("id") : -1;
            if (billingId == -1) {
                throw new SQLException("Failed to get billing ID");
            }

            // Get service_type_id
            rs = MySQL.search("SELECT id FROM service_type WHERE service_name = '" + request.serviceType.replace("'", "''") + "'");
            int serviceTypeId = rs.next() ? rs.getInt("id") : -1;
            if (serviceTypeId == -1) {
                throw new SQLException("Invalid service type");
            }

            // Insert into billing_item
            String itemQuery = String.format(
                    "INSERT INTO billing_item (billing_id, service_type_id, description, amount) VALUES (%d, %d, '%s', %.2f)",
                    billingId, serviceTypeId, ("Service: " + request.serviceType).replace("'", "''"), request.serviceCost
            );
            MySQL.iud(itemQuery);

            // Insert into transaction
            String transactionQuery = String.format(
                    "INSERT INTO transaction (billing_id, patient_id, service_type_id, amount, payment_method_id) VALUES (%d, %s, %d, %.2f, %d)",
                    billingId, request.patientId, serviceTypeId, request.serviceCost, paymentMethodId
            );
            MySQL.iud(transactionQuery);

            JOptionPane.showMessageDialog(null, "Billing saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saving billing: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}

public class Billing_And_Insurance extends javax.swing.JFrame {

    private Map<String, Double> serviceCosts = new HashMap<>();

    /**
     * Creates new form Billing_And_Insurance
     */
    public Billing_And_Insurance() {
        initComponents();
        loadServiceTypes();
        loadPaymentMethods();

    }

    private void loadPaymentMethods() {
        try {
            ResultSet rs = MySQL.search("SELECT * FROM payment_method");
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select Payment Method");
            while (rs.next()) {
                String paymentMethod = rs.getString("method");
                if (!paymentMethod.equalsIgnoreCase("Insurance")) {
                    model.addElement(paymentMethod);
                }
            }
            jComboBox2.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadServiceTypes() {
        try {
            ResultSet rs = MySQL.search("SELECT service_name, default_amount FROM service_type");
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select Service");
            while (rs.next()) {
                String serviceName = rs.getString("service_name");
                model.addElement(serviceName);
                serviceCosts.put(serviceName, rs.getDouble("default_amount"));
            }
            jComboBox1.setModel(model);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading services: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBillingTable(String patientId) {
    try {
        String query = "SELECT b.patient_id, b.created_at AS date, st.service_name AS service, b.total_amount AS billed_amount, pm.method AS method, cs.status AS status "
                + "FROM billing b "
                + "JOIN billing_item bi ON b.id = bi.billing_id "
                + "JOIN service_type st ON bi.service_type_id = st.id "
                + "JOIN claim_status cs ON b.claim_status_id = cs.id "
                + "JOIN payment_method pm ON b.payment_method_id = pm.id "
                + "WHERE b.patient_id = " + patientId;
        ResultSet rs = MySQL.search(query);
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("patient_id"),
                rs.getString("date"),
                rs.getString("service"),
                rs.getDouble("billed_amount"),
                rs.getString("method"),
                rs.getString("status")
            });
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading billing data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

    private void clearTable() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
    }

    private void fetchPatientName(String patientId) {
        try {
            ResultSet rs = MySQL.search("SELECT name FROM patient WHERE id = " + patientId);
            if (rs.next()) {
                jTextField2.setText(rs.getString("name"));
            } else {
                jTextField2.setText("");
                clearTable();
            }
        } catch (SQLException e) {
            jTextField2.setText("");
            clearTable();
        }
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
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        jCheckBox1 = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jComboBox2 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1280, 720));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Billing & Insurance");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 242, 215));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Patient ID :");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Patient Name :");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Service Type :");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Service Cost :");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Use Insurance :");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setText("Payment Method :");

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setText("Submit Claim");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton2.setText("Clear From");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField1KeyReleased(evt);
            }
        });

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jCheckBox1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jCheckBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBox1ItemStateChanged(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Pateint ID", "Date", "Service", "Billed Amount", "Payment Method", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jTextField2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jTextField3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jComboBox2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addGap(57, 57, 57)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jCheckBox1)
                                    .addComponent(jComboBox1, 0, 247, Short.MAX_VALUE)
                                    .addComponent(jTextField2)
                                    .addComponent(jTextField3)
                                    .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 13, Short.MAX_VALUE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 743, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jCheckBox1))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(36, 36, 36)
                        .addComponent(jButton1)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 561, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(47, Short.MAX_VALUE))
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
            String patientId = jTextField1.getText().trim();
            String serviceType = (String) jComboBox1.getSelectedItem();
            double serviceCost = Double.parseDouble(jTextField3.getText());
            boolean useInsurance = jCheckBox1.isSelected();
            String paymentMethod = useInsurance ? "Insurance" : (String) jComboBox2.getSelectedItem();

            if (patientId.isEmpty() || "Select Service".equals(serviceType) || serviceCost <= 0) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!useInsurance && "Select Payment Method".equals(paymentMethod)) {
                JOptionPane.showMessageDialog(this, "Please select a payment method", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BillingRequest request = new BillingRequest(patientId, serviceType, serviceCost, useInsurance, paymentMethod);

            PatientValidator patientValidator = new PatientValidator();
            ServiceValidator serviceValidator = new ServiceValidator();
            BillingSaver billingSaver = new BillingSaver();

            patientValidator.setNext(serviceValidator);
            serviceValidator.setNext(billingSaver);

            if (patientValidator.handle(request)) {
                loadBillingTable(patientId);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid service cost", "Error", JOptionPane.ERROR_MESSAGE);
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
         jTextField1.setText("");
        jTextField2.setText("");
        jComboBox1.setSelectedIndex(0);
        jTextField3.setText("");
        jCheckBox1.setSelected(false);
        jComboBox2.setSelectedIndex(0);
        jComboBox2.setEnabled(false);
        clearTable();
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jCheckBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox1ItemStateChanged

       jComboBox2.setEnabled(evt.getStateChange() == ItemEvent.DESELECTED);
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            jComboBox2.setSelectedIndex(0);
        }

        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox1ItemStateChanged

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed

       String service = (String) jComboBox1.getSelectedItem();
        if (service != null) {
            Double cost = serviceCosts.get(service);
            jTextField3.setText(cost != null ? String.valueOf(cost) : "");
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jTextField1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyReleased

         String patientId = jTextField1.getText().trim();
        if (!patientId.isEmpty()) {
            fetchPatientName(patientId);
            loadBillingTable(patientId);
        } else {
            jTextField2.setText("");
            clearTable();
        }

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
            java.util.logging.Logger.getLogger(Billing_And_Insurance.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Billing_And_Insurance.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Billing_And_Insurance.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Billing_And_Insurance.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        FlatMacLightLaf.setup();


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               // new Billing_And_Insurance().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables
}
