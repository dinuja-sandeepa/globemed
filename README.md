# GlobeMed - Healthcare Management System 🏥

## 🎯 Project Overview

GlobeMed is a desktop-based Hospital Management System developed to simplify hospital administration and day-to-day operations. Built with Java Swing and MySQL, the application provides centralized management of patient records, appointments, billing, insurance, medical reports, and staff permissions through role-based access control.

This project demonstrates practical skills in Java desktop application development, database integration using JDBC, SQL database design, and role-based application architecture.


## ✨ Features

- **🔐 Role-Based Access Control (RBAC):** Secure login with customized dashboard views for different staff roles:
  - **Admin:** Full access to all modules, including Staff & Permissions.
  - **Doctor:** Access to Patient Records, Appointments, and Reports.
  - **Nurse:** Access to Patient Records and certain operational tabs.
  - **Pharmacist:** Access to Billing, Insurance, and Patient Records.
- **📁 Patient Records Management:** Add, view, update, and delete patient details, medical history, and treatment plans.
- **🗓️ Appointment Scheduling:** Easily book, track, and manage patient appointments with doctors.
- **💳 Billing & Insurance:** Generate invoices, manage payments, and handle patient insurance details.
- **📊 Reporting Module:** Generate operational reports and view hospital analytics.
- **👨‍⚕️ Staff & Permissions:** (Admin only) Manage hospital staff details and assign system roles.

## 🛠️ Technologies Used

- **Language:** Java (JDK 8 or higher)
- **UI Framework:** Java Swing
- **Look and Feel:** [FlatLaf](https://www.formdev.com/flatlaf/) (Flat Mac Light theme)
- **Database:** MySQL
- **Database Connectivity:** JDBC
- **IDE:** Apache NetBeans

## 🚀 Getting Started

### Prerequisites

To run this project locally, you will need:
- Java Development Kit (JDK) installed on your system.
- A local database server for MySQL (like XAMPP, WAMP, or MySQL Workbench).
- Apache NetBeans IDE (or any other standard Java IDE).

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/dinuja-sandeepa/globemed.git
   ```

2. **Database Setup:**
   - Open your MySQL server environment.
   - Create a new database for the project (e.g., `globemed`).
   - Make sure your database tables (for patients, appointments, billing, users/staff) are created. *(Note: If you have an exported `.sql` dump file, import it here).*
   - Update the database connection credentials (URL, username, password) in the `src/Model/MySQL.java` file.

3. **Open the Project:**
   - Open NetBeans IDE.
   - Go to `File` -> `Open Project...` and navigate to the cloned `GlobeMed` directory.

4. **Dependencies:**
   - Ensure the **MySQL JDBC Driver** is added to the project libraries.
   - Ensure the **FlatLaf** library is added to your project's classpath to render the UI properly.

5. **Run the Application:**
   - Build the project and run the application starting from the `Login.java` file inside the `src/GUI/` package.

## 🤝 Contributing

Contributions are welcome! If you have suggestions to improve the application, feel free to fork the repo and create a pull request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is open-source. Feel free to use and modify it as needed.
