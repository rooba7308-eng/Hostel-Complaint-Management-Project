import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static final int ESCALATION_DAYS = 2;
    static final String[] CATEGORIES = {"Plumbing", "Electrical", "Cleaning", "Furniture", "Internet", "Security", "Other"};

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found. Add mysql-connector-j jar to lib folder.");
            return;
        }
        while (true) {
            System.out.println("\n=== HOSTEL COMPLAINT MANAGEMENT SYSTEM ===");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            if (c.equals("1")) login();
            else if (c.equals("2")) { System.out.println("Goodbye!"); break; }
            else System.out.println("Invalid choice.");
        }
    }

    // ---------------- LOGIN ----------------
    static void login() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        String sql = "SELECT user_id, role FROM users WHERE username=? AND password=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Invalid username or password."); return; }
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                System.out.println("Login successful. Role: " + role);
                switch (role) {
                    case "STUDENT": studentMenu(con, userId); break;
                    case "WARDEN":  wardenMenu(con); break;
                    case "STAFF":   staffMenu(con, userId); break;
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    static int getProfileId(Connection con, String table, String idCol, int userId) throws SQLException {
        String sql = "SELECT " + idCol + " FROM " + table + " WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // ---------------- STUDENT ----------------
    static void studentMenu(Connection con, int userId) throws SQLException {
        int studentId = getProfileId(con, "students", "student_id", userId);
        if (studentId == -1) { System.out.println("Student profile not found."); return; }
        while (true) {
            System.out.println("\n--- STUDENT MENU ---");
            System.out.println("1. Submit complaint");
            System.out.println("2. View my complaints");
            System.out.println("3. Logout");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            if (c.equals("1")) submitComplaint(con, studentId);
            else if (c.equals("2")) viewStudentComplaints(con, studentId);
            else if (c.equals("3")) return;
            else System.out.println("Invalid choice.");
        }
    }

    static void submitComplaint(Connection con, int studentId) throws SQLException {
        System.out.println("Categories:");
        for (int i = 0; i < CATEGORIES.length; i++) System.out.println((i + 1) + ". " + CATEGORIES[i]);
        System.out.print("Select category number: ");
        int idx;
        try { idx = Integer.parseInt(sc.nextLine().trim()); } catch (NumberFormatException e) { idx = 0; }
        if (idx < 1 || idx > CATEGORIES.length) { System.out.println("Invalid category."); return; }

        System.out.print("Description: ");
        String desc = sc.nextLine().trim();
        if (desc.isEmpty()) { System.out.println("Description cannot be empty."); return; }

        System.out.print("Priority (LOW / MEDIUM / HIGH): ");
        String pr = sc.nextLine().trim().toUpperCase();
        if (!pr.equals("LOW") && !pr.equals("MEDIUM") && !pr.equals("HIGH")) {
            System.out.println("Invalid priority."); return;
        }

        String sql = "INSERT INTO complaints (student_id, category, description, priority, status) VALUES (?,?,?,?, 'PENDING')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setString(2, CATEGORIES[idx - 1]);
            ps.setString(3, desc);
            ps.setString(4, pr);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) System.out.println("Complaint submitted. Your Complaint ID: " + k.getInt(1));
            }
        }
    }

    static void viewStudentComplaints(Connection con, int studentId) throws SQLException {
        String sql = "SELECT complaint_id, category, priority, status, complaint_date, description " +
                     "FROM complaints WHERE student_id=? ORDER BY complaint_date DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%n%-5s %-11s %-8s %-12s %-19s %s%n", "ID", "Category", "Priority", "Status", "Date", "Description");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-5d %-11s %-8s %-12s %-19s %s%n",
                        rs.getInt("complaint_id"), rs.getString("category"), rs.getString("priority"),
                        rs.getString("status"), rs.getTimestamp("complaint_date").toString().substring(0, 19),
                        rs.getString("description"));
                }
                if (!any) System.out.println("No complaints found.");
            }
        }
    }

    // ---------------- WARDEN ----------------
    static void wardenMenu(Connection con) throws SQLException {
        while (true) {
            System.out.println("\n--- WARDEN MENU ---");
            System.out.println("1. View all complaints");
            System.out.println("2. View pending / unresolved complaints");
            System.out.println("3. View maintenance staff");
            System.out.println("4. Assign complaint to staff");
            System.out.println("5. Update complaint status");
            System.out.println("6. Run escalation check (" + ESCALATION_DAYS + " days)");
            System.out.println("7. View escalations");
            System.out.println("8. Logout");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": viewAllComplaints(con, false); break;
                case "2": viewAllComplaints(con, true); break;
                case "3": viewStaff(con); break;
                case "4": assignComplaint(con); break;
                case "5": wardenUpdateStatus(con); break;
                case "6": runEscalation(con); break;
                case "7": viewEscalations(con); break;
                case "8": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void viewAllComplaints(Connection con, boolean unresolvedOnly) throws SQLException {
        String sql = "SELECT c.complaint_id, s.name, s.room_no, c.category, c.priority, c.status, c.complaint_date, c.description " +
                     "FROM complaints c JOIN students s ON c.student_id = s.student_id " +
                     (unresolvedOnly ? "WHERE c.status <> 'RESOLVED' " : "") +
                     "ORDER BY FIELD(c.priority,'HIGH','MEDIUM','LOW'), c.complaint_date";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%n%-5s %-14s %-6s %-11s %-8s %-12s %-19s %s%n",
                "ID", "Student", "Room", "Category", "Priority", "Status", "Date", "Description");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-14s %-6s %-11s %-8s %-12s %-19s %s%n",
                    rs.getInt("complaint_id"), rs.getString("name"), rs.getString("room_no"),
                    rs.getString("category"), rs.getString("priority"), rs.getString("status"),
                    rs.getTimestamp("complaint_date").toString().substring(0, 19), rs.getString("description"));
            }
            if (!any) System.out.println("No complaints found.");
        }
    }

    static void viewStaff(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT staff_id, name, specialization FROM maintenance_staff")) {
            System.out.printf("%n%-8s %-15s %s%n", "StaffID", "Name", "Specialization");
            while (rs.next())
                System.out.printf("%-8d %-15s %s%n", rs.getInt(1), rs.getString(2), rs.getString(3));
        }
    }

    static void assignComplaint(Connection con) throws SQLException {
        System.out.print("Complaint ID: ");
        int cid = readInt();
        System.out.print("Staff ID: ");
        int sid = readInt();

        if (!exists(con, "SELECT 1 FROM complaints WHERE complaint_id=? AND status='PENDING'", cid)) {
            System.out.println("Complaint not found or not in PENDING status."); return;
        }
        if (!exists(con, "SELECT 1 FROM maintenance_staff WHERE staff_id=?", sid)) {
            System.out.println("Staff not found."); return;
        }
        con.setAutoCommit(false);
        try (PreparedStatement a = con.prepareStatement("INSERT INTO assignments (complaint_id, staff_id) VALUES (?,?)");
             PreparedStatement u = con.prepareStatement("UPDATE complaints SET status='ASSIGNED' WHERE complaint_id=?")) {
            a.setInt(1, cid); a.setInt(2, sid); a.executeUpdate();
            u.setInt(1, cid); u.executeUpdate();
            con.commit();
            System.out.println("Complaint " + cid + " assigned to staff " + sid + ". Status: ASSIGNED");
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Assignment failed: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void wardenUpdateStatus(Connection con) throws SQLException {
        System.out.print("Complaint ID: ");
        int cid = readInt();
        System.out.print("New status (PENDING / ASSIGNED / IN_PROGRESS / RESOLVED / ESCALATED): ");
        String st = sc.nextLine().trim().toUpperCase();
        if (!isValidStatus(st)) { System.out.println("Invalid status."); return; }
        updateStatus(con, cid, st);
    }

    static void runEscalation(Connection con) throws SQLException {
        String find = "SELECT complaint_id FROM complaints " +
                      "WHERE status IN ('PENDING','ASSIGNED','IN_PROGRESS') " +
                      "AND complaint_date < (NOW() - INTERVAL ? DAY)";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(find)) {
            ps.setInt(1, ESCALATION_DAYS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        if (ids.isEmpty()) { System.out.println("No delayed complaints found."); return; }

        con.setAutoCommit(false);
        try (PreparedStatement ins = con.prepareStatement("INSERT INTO escalations (complaint_id, reason) VALUES (?,?)");
             PreparedStatement upd = con.prepareStatement("UPDATE complaints SET status='ESCALATED' WHERE complaint_id=?")) {
            for (int id : ids) {
                ins.setInt(1, id);
                ins.setString(2, "Unresolved for more than " + ESCALATION_DAYS + " days");
                ins.executeUpdate();
                upd.setInt(1, id);
                upd.executeUpdate();
            }
            con.commit();
            System.out.println(ids.size() + " complaint(s) escalated: " + ids);
        } catch (SQLException e) {
            con.rollback();
            System.out.println("Escalation failed: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void viewEscalations(Connection con) throws SQLException {
        String sql = "SELECT e.escalation_id, e.complaint_id, c.category, c.priority, e.escalated_date, e.reason " +
                     "FROM escalations e JOIN complaints c ON e.complaint_id = c.complaint_id ORDER BY e.escalated_date DESC";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%n%-6s %-10s %-11s %-8s %-19s %s%n", "EscID", "Complaint", "Category", "Priority", "Escalated On", "Reason");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-6d %-10d %-11s %-8s %-19s %s%n",
                    rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                    rs.getTimestamp(5).toString().substring(0, 19), rs.getString(6));
            }
            if (!any) System.out.println("No escalations yet.");
        }
    }

    // ---------------- STAFF ----------------
    static void staffMenu(Connection con, int userId) throws SQLException {
        int staffId = getProfileId(con, "maintenance_staff", "staff_id", userId);
        if (staffId == -1) { System.out.println("Staff profile not found."); return; }
        while (true) {
            System.out.println("\n--- STAFF MENU ---");
            System.out.println("1. View my assigned complaints");
            System.out.println("2. Update work progress");
            System.out.println("3. Logout");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            if (c.equals("1")) viewAssigned(con, staffId);
            else if (c.equals("2")) staffUpdate(con, staffId);
            else if (c.equals("3")) return;
            else System.out.println("Invalid choice.");
        }
    }

    static void viewAssigned(Connection con, int staffId) throws SQLException {
        String sql = "SELECT c.complaint_id, s.name, s.room_no, c.category, c.priority, c.status, c.description " +
                     "FROM assignments a JOIN complaints c ON a.complaint_id = c.complaint_id " +
                     "JOIN students s ON c.student_id = s.student_id WHERE a.staff_id=? " +
                     "ORDER BY FIELD(c.priority,'HIGH','MEDIUM','LOW')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%n%-5s %-14s %-6s %-11s %-8s %-12s %s%n", "ID", "Student", "Room", "Category", "Priority", "Status", "Description");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-5d %-14s %-6s %-11s %-8s %-12s %s%n",
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7));
                }
                if (!any) System.out.println("No complaints assigned to you.");
            }
        }
    }

    static void staffUpdate(Connection con, int staffId) throws SQLException {
        System.out.print("Complaint ID: ");
        int cid = readInt();
        String check = "SELECT 1 FROM assignments WHERE complaint_id=? AND staff_id=?";
        try (PreparedStatement ps = con.prepareStatement(check)) {
            ps.setInt(1, cid); ps.setInt(2, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("This complaint is not assigned to you."); return; }
            }
        }
        System.out.print("New status (IN_PROGRESS / RESOLVED): ");
        String st = sc.nextLine().trim().toUpperCase();
        if (!st.equals("IN_PROGRESS") && !st.equals("RESOLVED")) { System.out.println("Invalid status."); return; }
        updateStatus(con, cid, st);
    }

    // ---------------- HELPERS ----------------
    static void updateStatus(Connection con, int cid, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE complaints SET status=? WHERE complaint_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, cid);
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Status updated to " + status : "Complaint not found.");
        }
    }

    static boolean exists(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static boolean isValidStatus(String s) {
        return s.equals("PENDING") || s.equals("ASSIGNED") || s.equals("IN_PROGRESS")
            || s.equals("RESOLVED") || s.equals("ESCALATED");
    }

    static int readInt() {
        try { return Integer.parseInt(sc.nextLine().trim()); } catch (NumberFormatException e) { return -1; }
    }
}
