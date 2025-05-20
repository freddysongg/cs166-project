# Airline Management System

### Enhanced CLI Experience
- Color-coded interface using ANSI escape codes for better readability
- Help menu (press 'h' or '0') showing available commands
- Clear screen functionality (press 'c')
- Input validation with re-prompting for invalid dates/numbers
- Pagination for large result sets (40 rows per page)
- Status footer showing row count and query execution time

### Schema Improvements
- Secure user authentication with SHA-256 password hashing
- Action logging for all important operations
- Timestamp tracking for reservations
- Additional indexes for improved query performance

### Demo Users
- Manager: username=admin, password=admin123
- Customer: username=customer1, password=pass123
- Pilot: username=pilot1, password=pass123
- Technician: username=tech1, password=pass123

* Go to the project directory that has the following folders/files: 
       data  java  README.txt  sql

* To create the database and load data run the following script: 
    source sql/scripts/create_db.sh

* To run the java program use the following script: 
    source java/scripts/compile.sh  

