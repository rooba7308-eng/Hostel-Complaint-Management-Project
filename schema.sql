CREATE DATABASE IF NOT EXISTS hostel_db;
USE hostel_db;

CREATE TABLE users (
    user_id  INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role     ENUM('STUDENT','WARDEN','STAFF') NOT NULL
);

CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    name       VARCHAR(100) NOT NULL,
    room_no    VARCHAR(10) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE maintenance_staff (
    staff_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    name           VARCHAR(100) NOT NULL,
    specialization VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE complaints (
    complaint_id   INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    category       VARCHAR(30) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    priority       ENUM('LOW','MEDIUM','HIGH') NOT NULL,
    status         ENUM('PENDING','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED') NOT NULL DEFAULT 'PENDING',
    complaint_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id)
);

CREATE TABLE assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id  INT NOT NULL,
    staff_id      INT NOT NULL,
    assigned_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id),
    FOREIGN KEY (staff_id) REFERENCES maintenance_staff(staff_id)
);

CREATE TABLE escalations (
    escalation_id   INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id    INT NOT NULL,
    escalated_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason          VARCHAR(200),
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id)
);

-- Sample data (passwords are plain text for learning only)
INSERT INTO users (username, password, role) VALUES
 ('warden1','warden123','WARDEN'),
 ('student1','stud123','STUDENT'),
 ('student2','stud123','STUDENT'),
 ('staff1','staff123','STAFF'),
 ('staff2','staff123','STAFF');

INSERT INTO students (user_id, name, room_no) VALUES
 (2,'Arun Kumar','A101'),
 (3,'Priya Sharma','B205');

INSERT INTO maintenance_staff (user_id, name, specialization) VALUES
 (4,'Ravi','Plumbing'),
 (5,'Suresh','Electrical');
