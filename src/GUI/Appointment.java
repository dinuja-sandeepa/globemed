package GUI;

import Model.MySQL;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import javax.swing.JButton;

public class Appointment extends javax.swing.JFrame {

    private AppointmentMediator mediator;

    public Appointment() {
        mediator = new AppointmentMediator(this);
        initComponents();
        loadDoctors();
        loadTimeSlots();
        loadAppointments();
    }

    /**
     * Interface for UI components to interact with the mediator.
     */
    interface AppointmentColleague {

        void updateUI();

        void clearForm();
    }

    /**
     * Mediator to handle interactions between UI components and business logic.
     */
    static class AppointmentMediator {

        private Appointment appointmentForm;
        private AppointmentManager appointmentManager;

        public AppointmentMediator(Appointment form) {
            this.appointmentForm = form;
            this.appointmentManager = new AppointmentManager();
        }

        // Handle scheduling a new appointment
        public void scheduleAppointment(String doctorId, String timeSlotId, String patientId, String date) {
            try {
                appointmentManager.scheduleAppointment(doctorId, timeSlotId, patientId, date);
                appointmentForm.loadAppointments();
                appointmentForm.clearForm();
                JOptionPane.showMessageDialog(appointmentForm, "Appointment scheduled successfully");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(appointmentForm, "Error scheduling appointment: Another appontment already sheduled for this time. ");
            }
        }

        // Handle updating an appointment
        public void updateAppointment(String appointmentId, String doctorId, String timeSlotId, String patientId, String date, String status) {
            try {
                appointmentManager.updateAppointment(appointmentId, doctorId, timeSlotId, patientId, date, status);
                appointmentForm.loadAppointments();
                appointmentForm.clearForm();
                JOptionPane.showMessageDialog(appointmentForm, "Appointment updated successfully");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(appointmentForm, "Error updating appointment: " + e.getMessage());
            }
        }

        // Handle removing an appointment
        public void removeAppointment(String appointmentId) {
            try {
                appointmentManager.removeAppointment(appointmentId);
                appointmentForm.loadAppointments();
                appointmentForm.clearForm();
                JOptionPane.showMessageDialog(appointmentForm, "Appointment removed successfully");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(appointmentForm, "Error removing appointment: " + e.getMessage());
            }
        }

        // Load data into UI components
        public void loadDoctors() {
            appointmentForm.loadDoctors();
        }

        public void loadTimeSlots() {
            appointmentForm.loadTimeSlots();
        }

        public void loadAppointments() {
            appointmentForm.loadAppointments();
        }

        // Populate form with selected appointment details
        public void populateForm(String appointmentId) {
            try {
                ResultSet rs = appointmentManager.getAppointment(appointmentId);
                if (rs.next()) {
                    appointmentForm.jComboBox1.setSelectedItem(rs.getString("doctor_id") + ": " + rs.getString("full_name"));
                    appointmentForm.jComboBox2.setSelectedItem(rs.getString("slot_time"));
                    appointmentForm.jDateChooser1.setDate(rs.getDate("appointment_date"));
                    appointmentForm.jTextField1.setText(rs.getString("patient_id"));
                    appointmentForm.jComboBox3.setSelectedItem(rs.getString("status"));
                }
                rs.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(appointmentForm, "Error loading appointment details: " + e.getMessage());
            }
        }
    }

    /**
     * Class to handle database operations for appointments.
     */
    static class AppointmentManager {

    public void scheduleAppointment(String doctorId, String timeSlotId, String patientName, String date) throws SQLException {
        String query = String.format(
            "INSERT INTO appointment (patient_name, staff_id, appointment_date, time_slot_id, status) " +
            "VALUES ('%s', %s, '%s', %s, 'Scheduled')",
            patientName, doctorId, date, timeSlotId
        );
        MySQL.iud(query);
    }

    public void updateAppointment(String appointmentId, String doctorId, String timeSlotId, String patientName, String date, String status) throws SQLException {
        String query = String.format(
            "UPDATE appointment SET staff_id = %s, time_slot_id = %s, patient_name = '%s', appointment_date = '%s', status = '%s' WHERE id = %s",
            doctorId, timeSlotId, patientName, date, status, appointmentId
        );
        MySQL.iud(query);
    }

    public void removeAppointment(String appointmentId) throws SQLException {
        String query = String.format("DELETE FROM appointment WHERE id = %s", appointmentId);
        MySQL.iud(query);
    }

    public ResultSet getAppointments() throws SQLException {
        return MySQL.search(
            "SELECT appointment.id, appointment.appointment_date, time_slot.slot_time, appointment.patient_name, staff.full_name, appointment.status " +
            "FROM appointment " +
            "JOIN time_slot ON appointment.time_slot_id = time_slot.id " +
            "JOIN staff ON appointment.staff_id = staff.id"
        );
    }

    public ResultSet getAppointment(String appointmentId) throws SQLException {
        return MySQL.search(
            "SELECT appointment.id, appointment.appointment_date, time_slot.slot_time, appointment.patient_name, appointment.staff_id, staff.full_name, appointment.status " +
            "FROM appointment " +
            "JOIN time_slot ON appointment.time_slot_id = time_slot.id " +
            "JOIN staff ON appointment.staff_id = staff.id " +
            "WHERE appointment.id = " + appointmentId
        );
    }

    public ResultSet getDoctors() throws SQLException {
        return MySQL.search("SELECT id, full_name FROM staff WHERE role_id = 1");
    }

    public ResultSet getTimeSlots() throws SQLException {
        return MySQL.search("SELECT id, slot_time FROM time_slot");
    }
}


    /**
     * Load doctors into jComboBox1.
     */
    private void loadDoctors() {
        try {
            jComboBox1.removeAllItems();
            ResultSet rs = mediator.appointmentManager.getDoctors();
            while (rs.next()) {
                jComboBox1.addItem(rs.getString("id") + ": " + rs.getString("full_name"));
            }
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading doctors: " + e.getMessage());
        }
    }

    /**
     * Load time slots into jComboBox2.
     */
    private void loadTimeSlots() {
        try {
            jComboBox2.removeAllItems();
            ResultSet rs = mediator.appointmentManager.getTimeSlots();
            while (rs.next()) {
                jComboBox2.addItem(rs.getString("slot_time"));
            }
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading time slots: " + e.getMessage());
        }
    }

    /**
     * Load appointments into jTable1.
     */
    private void loadAppointments() {
        try {
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            ResultSet rs = mediator.appointmentManager.getAppointments();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id"), // appointment ID (hidden later)
                    rs.getString("appointment_date"),
                    rs.getString("slot_time"),
                    rs.getString("patient_name"),
                    rs.getString("full_name"),
                    rs.getString("status")
                });
            }
            rs.close();

            // Hide the ID column
            jTable1.getColumnModel().getColumn(0).setMinWidth(0);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(0);
            jTable1.getColumnModel().getColumn(0).setWidth(0);

            jButton1.setEnabled(true);
            jButton2.setEnabled(false);
            jButton3.setEnabled(false);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading appointments: " + e.getMessage());
        }
    }

    private String getTimeSlotId(String timeSlot) throws SQLException {
        ResultSet rs = MySQL.search("SELECT id FROM time_slot WHERE slot_time = '" + timeSlot + "'");
        if (rs.next()) {
            String id = rs.getString("id");
            rs.close();
            return id;
        }
        rs.close();
        throw new SQLException("Time slot not found");
    }

    /**
     * Clear form fields.
     */
    private void clearForm() {
        jComboBox1.setSelectedIndex(-1);
        jComboBox2.setSelectedIndex(-1);
        jDateChooser1.setDate(null);
        jTextField1.setText("");
        jComboBox3.setSelectedIndex(-1);
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
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 242, 215));
        jPanel1.setMinimumSize(new java.awt.Dimension(1280, 720));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Doctor Name :");

        jComboBox1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Date", "Time Slot", "Patient", "Doctor", "Status"
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

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Time Slot :");

        jComboBox2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Date :");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Patient Name :");

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Status :");

        jComboBox3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Scheduled", "Completed", "Cancelled" }));

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setText("Shedule");
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField1)
                            .addComponent(jDateChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(67, 67, 67)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 801, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 630, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(37, 37, 37)
                        .addComponent(jButton1)
                        .addGap(47, 47, 47)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(26, 26, 26)
                        .addComponent(jButton2)
                        .addGap(28, 28, 28)
                        .addComponent(jButton3)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Appointment");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel1)
                .addContainerGap(23, Short.MAX_VALUE))
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 653, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String doctor = (String) jComboBox1.getSelectedItem();
            String timeSlot = (String) jComboBox2.getSelectedItem();
            String patientName  = jTextField1.getText();
            java.util.Date date = jDateChooser1.getDate();

            if (doctor == null || timeSlot == null || patientName .isEmpty() || date == null) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields");
                return;
            }

            String doctorId = doctor.split(":")[0].trim();
            String timeSlotId = getTimeSlotId(timeSlot);
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

            mediator.scheduleAppointment(doctorId, timeSlotId, patientName, formattedDate);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + e.getMessage());
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an appointment to update");
                return;
            }

            String appointmentId = jTable1.getValueAt(selectedRow, 0).toString();
            String doctor = (String) jComboBox1.getSelectedItem();
            String timeSlot = (String) jComboBox2.getSelectedItem();
            String patientId = jTextField1.getText();
            java.util.Date date = jDateChooser1.getDate();
            String status = (String) jComboBox3.getSelectedItem();

            if (doctor == null || timeSlot == null || patientId.isEmpty() || date == null || status == null) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields");
                return;
            }

            String doctorId = doctor.split(":")[0].trim();
            String timeSlotId = getTimeSlotId(timeSlot);
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

            mediator.updateAppointment(appointmentId, doctorId, timeSlotId, patientId, formattedDate, status);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating appointment: " + e.getMessage());
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an appointment to remove");
                return;
            }

            String appointmentId = jTable1.getValueAt(selectedRow, 0).toString();
            mediator.removeAppointment(appointmentId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error removing appointment: " + e.getMessage());
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        if (evt.getClickCount() == 2) {
            int row = jTable1.getSelectedRow();
            if (row != -1) {
                String appointmentId = jTable1.getValueAt(row, 0).toString();
                try {
                    ResultSet rs = mediator.appointmentManager.getAppointment(appointmentId);
                    if (rs.next()) {
                        // Set doctor combo box
                        String doctorItem = rs.getString("staff_id") + ": " + rs.getString("full_name");
                        jComboBox1.setSelectedItem(doctorItem);

                        // Set time slot combo box
                        String timeSlot = rs.getString("slot_time");
                        jComboBox2.setSelectedItem(timeSlot);

                        // Set date chooser
                        Date sqlDate = rs.getDate("appointment_date");
                        if (sqlDate != null) {
                            jDateChooser1.setDate(new java.util.Date(sqlDate.getTime()));
                        } else {
                            jDateChooser1.setDate(null);
                        }

                        // Set patient ID
                        jTextField1.setText(rs.getString("patient_name"));

                        // Set status
                        String status = rs.getString("status");
                        jComboBox3.setSelectedItem(status);

                        jButton1.setEnabled(false);
                        jButton2.setEnabled(true);
                        jButton3.setEnabled(true);
                    }
                    rs.close();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error loading appointment details: " + e.getMessage());
                }
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jTable1MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            // Apply FlatLaf
            FlatMacLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new Appointment().setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
