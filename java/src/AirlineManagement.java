/*
 * Template JAVA User Interface
 * =============================
 *
 * Database Management Systems
 * Department of Computer Science &amp; Engineering
 * University of California - Riverside
 *
 * Target DBMS: 'Postgres'
 *
 */


import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * This class defines a simple embedded SQL utility class that is designed to
 * work with PostgreSQL JDBC drivers.
 *
 */
public class AirlineManagement {

   // reference to physical database connection.
   private Connection _connection = null;

   // handling the keyboard inputs through a BufferedReader
   // This variable can be global for convenience.
   static BufferedReader in = new BufferedReader(
                                new InputStreamReader(System.in));

   // ANSI color codes
   private static final String ANSI_RESET = "\u001B[0m";
   private static final String ANSI_GREEN = "\u001B[32m";
   private static final String ANSI_RED = "\u001B[31m";
   private static final String ANSI_BLUE = "\u001B[34m";
   private static final String ANSI_YELLOW = "\u001B[33m";
   private static final String ANSI_CYAN = "\u001B[36m";

   // Table formatting
   private static final String TABLE_BORDER = "+-----------------+";
   private static final String TABLE_FORMAT = "| %-15s |";

   // Help messages for menus
   private static final String[][] MANAGER_HELP = {
       {"1", "View weekly schedule for a specific flight"},
       {"2", "Check seat availability for a flight on a specific date"},
       {"3", "View on-time performance for a flight"},
       {"4", "List all flights for a given date"},
       {"5", "View passenger lists by reservation status"},
       {"6", "Look up traveler information by reservation"},
       {"7", "View plane details and maintenance history"},
       {"8", "View all repairs performed by a technician"},
       {"9", "View repair history for a plane within date range"},
       {"10", "View flight statistics for a date range"},
       {"h", "Show this help menu"},
       {"c", "Clear screen"},
       {"20", "Log out"}
   };

   /**
    * Creates a new instance of AirlineManagement
    *
    * @param hostname the MySQL or PostgreSQL server hostname
    * @param database the name of the database
    * @param username the user name used to login to the database
    * @param password the user login password
    * @throws java.sql.SQLException when failed to make a connection.
    */
   public AirlineManagement(String dbname, String dbport, String user, String passwd) throws SQLException {

      System.out.print("Connecting to database...");
      try{
         // constructs the connection URL
         String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
         System.out.println ("Connection URL: " + url + "\n");

         // obtain a physical connection
         this._connection = DriverManager.getConnection(url, user, passwd);
         System.out.println("Done");
      }catch (Exception e){
         System.err.println("Error - Unable to Connect to Database: " + e.getMessage() );
         System.out.println("Make sure you started postgres on this machine");
         System.exit(-1);
      }//end catch
   }//end AirlineManagement

   /**
    * Method to execute an update SQL statement.  Update SQL instructions
    * includes CREATE, INSERT, UPDATE, DELETE, and DROP.
    *
    * @param sql the input SQL string
    * @throws java.sql.SQLException when update failed
    */
   public void executeUpdate (String sql) throws SQLException {
      // creates a statement object
      Statement stmt = this._connection.createStatement ();

      // issues the update instruction
      stmt.executeUpdate (sql);

      // close the instruction
      stmt.close ();
   }//end executeUpdate

   /**
    * Method to execute an input query SQL instruction (i.e. SELECT).  This
    * method issues the query to the DBMS and outputs the results to
    * standard out.
    *
    * @param query the input query string
    * @return the number of rows returned
    * @throws java.sql.SQLException when failed to execute the query
    */
   public int executeQueryAndPrintResult (String query) throws SQLException {
      long startTime = System.nanoTime();
      Statement stmt = this._connection.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      ResultSetMetaData rsmd = rs.getMetaData();
      int numCol = rsmd.getColumnCount();
      int rowCount = 0;
      int pageSize = 20; 
      int currentRow = 0;

      boolean outputHeader = true;
      while (rs.next()) {
          if (outputHeader) {
              printTableHeader(rsmd, numCol);
              outputHeader = false;
          }

          printTableRow(rs, numCol);
          ++rowCount;
          ++currentRow;

          if (currentRow == pageSize) {
              System.out.print(ANSI_YELLOW + "\nPress Enter to continue, 'q' to quit: " + ANSI_RESET);
              try {
                  String input = in.readLine();
                  if (input != null && input.toLowerCase().equals("q")) {
                      break;
                  }
              } catch (IOException e) {
                  System.err.println(ANSI_RED + "Error reading input: " + e.getMessage() + ANSI_RESET);
                  break;
              }
              currentRow = 0;
              printTableHeader(rsmd, numCol);
          }
      }

      if (rowCount > 0) {
          StringBuilder border = new StringBuilder();
          for (int i = 0; i < numCol; i++) {
              border.append(TABLE_BORDER);
          }
          System.out.println(ANSI_CYAN + border.toString() + ANSI_RESET);
      }

      long endTime = System.nanoTime();
      double durationMs = (endTime - startTime) / 1_000_000.0;
      System.out.printf(ANSI_GREEN + "\nResults: %d rows • %.2f ms%n" + ANSI_RESET, rowCount, durationMs);

      stmt.close();
      return rowCount;
   }//end executeQuery

   /**
    * Method to execute an input query SQL instruction (i.e. SELECT).  This
    * method issues the query to the DBMS and returns the results as
    * a list of records. Each record in turn is a list of attribute values
    *
    * @param query the input query string
    * @return the query result as a list of records
    * @throws java.sql.SQLException when failed to execute the query
    */
   public List<List<String>> executeQueryAndReturnResult (String query) throws SQLException {
      // creates a statement object
      Statement stmt = this._connection.createStatement ();

      // issues the query instruction
      ResultSet rs = stmt.executeQuery (query);

      /*
       ** obtains the metadata object for the returned result set.  The metadata
       ** contains row and column info.
       */
      ResultSetMetaData rsmd = rs.getMetaData ();
      int numCol = rsmd.getColumnCount ();
      int rowCount = 0;

      // iterates through the result set and saves the data returned by the query.
      boolean outputHeader = false;
      List<List<String>> result  = new ArrayList<List<String>>();
      while (rs.next()){
        List<String> record = new ArrayList<String>();
		for (int i=1; i<=numCol; ++i)
			record.add(rs.getString (i));
        result.add(record);
      }//end while
      stmt.close ();
      return result;
   }//end executeQueryAndReturnResult

   /**
    * Method to execute an input query SQL instruction (i.e. SELECT).  This
    * method issues the query to the DBMS and returns the number of results
    *
    * @param query the input query string
    * @return the number of rows returned
    * @throws java.sql.SQLException when failed to execute the query
    */
   public int executeQuery (String query) throws SQLException {
       // creates a statement object
       Statement stmt = this._connection.createStatement ();

       // issues the query instruction
       ResultSet rs = stmt.executeQuery (query);

       int rowCount = 0;

       // iterates through the result set and count nuber of results.
       while (rs.next()){
          rowCount++;
       }//end while
       stmt.close ();
       return rowCount;
   }

   /**
    * Method to fetch the last value from sequence. This
    * method issues the query to the DBMS and returns the current
    * value of sequence used for autogenerated keys
    *
    * @param sequence name of the DB sequence
    * @return current value of a sequence
    * @throws java.sql.SQLException when failed to execute the query
    */
   public int getCurrSeqVal(String sequence) throws SQLException {
	Statement stmt = this._connection.createStatement ();

	ResultSet rs = stmt.executeQuery (String.format("Select currval('%s')", sequence));
	if (rs.next())
		return rs.getInt(1);
	return -1;
   }

   /**
    * Method to close the physical connection if it is open.
    */
   public void cleanup(){
      try{
         if (this._connection != null){
            this._connection.close ();
         }//end if
      }catch (SQLException e){
         // ignored.
      }//end try
   }//end cleanup

   /**
    * The main execution method
    *
    * @param args the command line arguments this inclues the <mysql|pgsql> <login file>
    */
   public static void main (String[] args) {
      if (args.length != 3) {
         System.err.println (
            "Usage: " +
            "java [-classpath <classpath>] " +
            AirlineManagement.class.getName () +
            " <dbname> <port> <user>");
         return;
      }//end if

      Greeting();
      AirlineManagement esql = null;
      try{
         // use postgres JDBC driver.
         Class.forName ("org.postgresql.Driver").newInstance ();
         // instantiate the AirlineManagement object and creates a physical
         // connection.
         String dbname = args[0];
         String dbport = args[1];
         String user = args[2];
         
         // get password
         System.out.print("Password for postgres user: ");
         String passwd = in.readLine();
         
         esql = new AirlineManagement (dbname, dbport, user, passwd);

         boolean keepon = true;
         while(keepon) {
            System.out.println("MAIN MENU");
            System.out.println("---------");
            System.out.println("1. Login");
            System.out.println("9. < EXIT");
            
            switch (readChoice()) {
                case 1: 
                    String role = LogIn(esql);
                    if (role != null) {
                        switch(role) {
                            case "manager":
                                showManagerMenu(esql);
                                break;
                            case "customer":
                                showCustomerMenu(esql);
                                break;
                            case "pilot":
                                showPilotMenu(esql);
                                break;
                            case "technician":
                                showTechnicianMenu(esql);
                                break;
                        }
                    }
                    break;
                case 9: 
                    keepon = false; 
                    break;
                default : 
                    System.out.println("Unrecognized choice!"); 
                    break;
            }
         }
      }catch(Exception e) {
         System.err.println (e.getMessage());
      }finally{
         // make sure to cleanup the created table and close the connection.
         try{
            if(esql != null) {
               System.out.print("Disconnecting from database...");
               esql.cleanup ();
               System.out.println("Done\n\nBye !");
            }//end if
         }catch (Exception e) {
            // ignored.
         }//end try
      }//end try
   }//end main

   public static void Greeting(){
      System.out.println(
         "\n\n*******************************************************\n" +
         "              User Interface      	               \n" +
         "*******************************************************\n");
   }//end Greeting

   /*
    * Reads the users choice given from the keyboard
    * @int
    **/
   public static int readChoice() {
      while (true) {
          System.out.print(ANSI_GREEN + "Please make your choice: " + ANSI_RESET);
          try {
              String input = in.readLine().trim().toLowerCase();
              
              if (input.equals("h") || input.equals("0")) {
                  return -1; // Help menu
              } else if (input.equals("c")) {
                  return -2; // Clear screen
              }
              
              int choice = Integer.parseInt(input);
              if (choice > 0) {
                  return choice;
              }
              System.out.println(ANSI_RED + "Please enter a positive number!" + ANSI_RESET);
          } catch (IOException e) {
              System.out.println(ANSI_RED + "Error reading input. Please try again." + ANSI_RESET);
          } catch (NumberFormatException e) {
              System.out.println(ANSI_RED + "Invalid input! Please enter a number." + ANSI_RESET);
          }
      }
   }//end readChoice

   /*
    * Creates a new user
    **/
   public static void CreateUser(AirlineManagement esql){
   }//end CreateUser


   /*
    * Check log in credentials for an existing user
    * @return User login or null is the user does not exist
    **/
   public static String LogIn(AirlineManagement esql) {
       try {
           System.out.print(ANSI_GREEN + "Username: " + ANSI_RESET);
           String username = in.readLine();
           System.out.print(ANSI_GREEN + "Password: " + ANSI_RESET);
           String password = in.readLine();

           String query = String.format(
               "SELECT role FROM Users WHERE username = '%s' AND password = '%s'",
               username, password
           );
           
           List<List<String>> result = esql.executeQueryAndReturnResult(query);
           if (result.isEmpty()) {
               System.out.println(ANSI_RED + "Invalid credentials!" + ANSI_RESET);
               return null;
           }

           String role = result.get(0).get(0);
           System.out.println(ANSI_GREEN + "Welcome " + username + "! You are logged in as: " + role + ANSI_RESET);
           return role;
       } catch (Exception e) {
           System.out.println(ANSI_RED + "Error during login: " + e.getMessage() + ANSI_RESET);
           return null;
       }
   }

/**
 * Utility class for input validation
 */
private static class Validator {
    private static final Pattern FLIGHT_NUMBER_PATTERN = Pattern.compile("^F[0-9]{3}$");
    private static final Pattern PLANE_ID_PATTERN = Pattern.compile("^PL[0-9]{3}$");
    private static final Pattern REPAIR_CODE_PATTERN = Pattern.compile("^RC[0-9]{3}$");
    private static final Pattern PILOT_ID_PATTERN = Pattern.compile("^P[0-9]{3}$");
    private static final Pattern TECH_ID_PATTERN = Pattern.compile("^T[0-9]{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^R[0-9]{4}$");
    
    public static boolean isValidDate(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public static boolean isValidTime(String time) {
        try {
            LocalTime.parse(time);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public static boolean isValidScheduleTimes(String departureTime, String arrivalTime) {
        try {
            LocalTime departure = LocalTime.parse(departureTime);
            LocalTime arrival = LocalTime.parse(arrivalTime);
            return arrival.isAfter(departure);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public static String generatePlaneId(int number) {
        return String.format("PL%03d", number);
    }
    
    public static String generateReservationId(int number) {
        return String.format("R%d", number);
    }
    
    public static String generateFlightNumber(int number) {
        return String.format("F%03d", number);
    }
    
    public static String generatePilotId(int number) {
        return String.format("P%03d", number);
    }
    
    public static String generateTechId(int number) {
        return String.format("T%03d", number);
    }
    
    public static boolean isValidFlightNumber(String flightNumber) {
        if (flightNumber == null) return false;
        return FLIGHT_NUMBER_PATTERN.matcher(flightNumber).matches();
    }
    
    public static boolean isValidPlaneId(String planeId) {
        if (planeId == null) return false;
        return PLANE_ID_PATTERN.matcher(planeId).matches();
    }

    public static boolean isValidRepairCode(String repairCode) {
        if (repairCode == null) return false;
        return REPAIR_CODE_PATTERN.matcher(repairCode).matches();
    }
    
    public static boolean isValidPilotId(String pilotId) {
        if (pilotId == null) return false;
        return PILOT_ID_PATTERN.matcher(pilotId).matches();
    }
    
    public static boolean isValidTechId(String techId) {
        if (techId == null) return false;
        return TECH_ID_PATTERN.matcher(techId).matches();
    }
    
    public static boolean isValidReservationId(String reservationId) {
        if (reservationId == null) return false;
        return RESERVATION_ID_PATTERN.matcher(reservationId).matches();
    }
    
    public static boolean isValidCustomerId(String customerId) {
        try {
            int id = Integer.parseInt(customerId);
            return id > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean areDatesValid(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return !start.isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public static String getValidationErrorMessage(String fieldName, String pattern) {
        return String.format("Invalid %s format! Should match pattern: %s", fieldName, pattern);
    }
}

/**
 * Utility class for database operations
 */
private static class DBChecker {
    public static boolean flightExists(AirlineManagement esql, String flightNumber) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Flight WHERE FlightNumber = '%s'",
            flightNumber
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
    
    public static boolean planeExists(AirlineManagement esql, String planeId) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Plane WHERE PlaneID = '%s'",
            planeId
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
    
    public static boolean customerExists(AirlineManagement esql, String customerId) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Customer WHERE CustomerID = '%s'",
            customerId
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
    
    public static boolean pilotExists(AirlineManagement esql, String pilotId) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Pilot WHERE PilotID = '%s'",
            pilotId
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
    
    public static boolean technicianExists(AirlineManagement esql, String techId) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Technician WHERE TechnicianID = '%s'",
            techId
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
    
    public static boolean reservationExists(AirlineManagement esql, String reservationId) throws SQLException {
        String query = String.format(
            "SELECT COUNT(*) FROM Reservation WHERE ReservationID = '%s'",
            reservationId
        );
        List<List<String>> result = esql.executeQueryAndReturnResult(query);
        return Integer.parseInt(result.get(0).get(0)) > 0;
    }
}

// Example of enhanced feature with validation
public static void feature1(AirlineManagement esql) {
    try {
        System.out.print("\nEnter Flight Number: ");
        String flightNum = in.readLine();
        
        // Validate flight number format
        if (!Validator.isValidFlightNumber(flightNum)) {
            System.out.println(ANSI_RED + "Invalid flight number format! Should be like 'F###'" + ANSI_RESET);
            return;
        }
        
        // Check if flight exists
        if (!DBChecker.flightExists(esql, flightNum)) {
            System.out.println(ANSI_RED + "Flight number does not exist!" + ANSI_RESET);
            return;
        }
        
        // Get schedule and validate times
        String query = "SELECT S.DayOfWeek, S.DepartureTime, S.ArrivalTime, " +
                      "F.DepartureCity, F.ArrivalCity " +
                      "FROM Schedule S, Flight F " +
                      "WHERE S.FlightNumber = F.FlightNumber " +
                      "AND S.FlightNumber = '" + flightNum + "' " +
                    //   "AND S.ArrivalTime > S.DepartureTime " +  Not necessary
                      "ORDER BY CASE DayOfWeek " +
                      "    WHEN 'Monday' THEN 1 " +
                      "    WHEN 'Tuesday' THEN 2 " +
                      "    WHEN 'Wednesday' THEN 3 " +
                      "    WHEN 'Thursday' THEN 4 " +
                      "    WHEN 'Friday' THEN 5 " +
                      "    WHEN 'Saturday' THEN 6 " +
                      "    WHEN 'Sunday' THEN 7 END";
        
        int rowCount = esql.executeQueryAndPrintResult(query);
        if (rowCount == 0) {
            System.out.println(ANSI_RED + "No valid schedule found for flight " + flightNum + ANSI_RESET);
        }
    } catch (SQLException e) {
        System.err.println(ANSI_RED + "Database error: " + e.getMessage() + ANSI_RESET);
    } catch (Exception e) {
        System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
    }
}

   public static void feature11(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Departure City: ");
           String depCity = in.readLine();
           System.out.print("Enter Arrival City: ");
           String arrCity = in.readLine();
           System.out.print("Enter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println("Invalid date format! Please use YYYY-MM-DD");
               return;
           }
           
           String query = "WITH FlightStats AS ( " +
                         "    SELECT FI.FlightNumber, " +
                         "           COUNT(*) as total_runs, " +
                         "           SUM(CASE WHEN FI.DepartedOnTime AND FI.ArrivedOnTime THEN 1 ELSE 0 END) as on_time, FI.NumOfStops " +
                         "    FROM FlightInstance FI " +
                         "    GROUP BY FI.FlightNumber, FI.NumOfStops " +
                         ") " +
                         "SELECT F.FlightNumber, F.DepartureCity, F.ArrivalCity, " +
                         "       S.DepartureTime, S.ArrivalTime, FS.NumOfStops, " +
                         "       ROUND(COALESCE(FS.on_time::FLOAT / NULLIF(FS.total_runs, 0) * 100, 0)::NUMERIC, 2) as on_time_percentage " +
                         "FROM Flight F " +
                         "JOIN Schedule S ON F.FlightNumber = S.FlightNumber " +
                         "LEFT JOIN FlightStats FS ON F.FlightNumber = FS.FlightNumber " +
                         "WHERE F.DepartureCity = '" + depCity + "' " +
                         "AND F.ArrivalCity = '" + arrCity + "' " +
                         "AND S.DayOfWeek = TRIM(TO_CHAR(DATE '" + date + "', 'Day')) " +
                         "ORDER BY S.DepartureTime";
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println("No flights found for the given criteria.");
           }
       } catch (Exception e) {
           System.err.println(e.getMessage());
       }
   }

   public static void feature2(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           System.out.print("Enter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println("Invalid date format! Please use YYYY-MM-DD");
               return;
           }
           
           String query = String.format(
               "SELECT FI.FlightNumber, FI.FlightDate, " +
               "FI.SeatsTotal as Total_Seats, " +
               "FI.SeatsSold as Sold_Seats, " +
               "(FI.SeatsTotal - FI.SeatsSold) as Available_Seats " +
               "FROM FlightInstance FI " +
               "WHERE FI.FlightNumber = '%s' " +
               "AND FI.FlightDate = '%s'",
               flightNum, date
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println("No flight found for the given criteria.");
           }
       } catch (Exception e) {
           System.err.println(e.getMessage());
       }
   }

   public static void feature3(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           
           // Validate flight number format
           if (!Validator.isValidFlightNumber(flightNum)) {
               System.out.println(ANSI_RED + "Invalid flight number format! Should be F### (e.g., F100)" + ANSI_RESET);
               return;
           }

           System.out.print("Enter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }

           // First check if flight exists
           String checkFlight = String.format(
               "SELECT COUNT(*) FROM Flight WHERE FlightNumber = '%s'",
               flightNum
           );
           List<List<String>> result = esql.executeQueryAndReturnResult(checkFlight);
           if (Integer.parseInt(result.get(0).get(0)) == 0) {
               System.out.println(ANSI_RED + "Flight " + flightNum + " not found!" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT F.FlightNumber, F.DepartureCity, F.ArrivalCity, " +
               "TO_CHAR(FI.FlightDate, 'YYYY-MM-DD') as FlightDate, " +
               "S.DepartureTime, S.ArrivalTime, " +
               "CASE WHEN FI.DepartedOnTime THEN 'Yes' ELSE 'No' END as DepartedOnTime, " +
               "CASE WHEN FI.ArrivedOnTime THEN 'Yes' ELSE 'No' END as ArrivedOnTime " +
               "FROM FlightInstance FI " +
               "JOIN Flight F ON FI.FlightNumber = F.FlightNumber " +
               "JOIN Schedule S ON F.FlightNumber = S.FlightNumber " +
               "AND S.DayOfWeek = TRIM(TO_CHAR(FI.FlightDate, 'Day')) " +
               "WHERE FI.FlightNumber = '%s' " +
               "AND FI.FlightDate = '%s'",
               flightNum, date
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No flight found for the given date." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature4(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT F.FlightNumber, F.DepartureCity, F.ArrivalCity, " +
               "TO_CHAR(FI.FlightDate, 'YYYY-MM-DD') as FlightDate, " +
               "S.DepartureTime, S.ArrivalTime, " +
               "FI.NumOfStops, " +
               "CASE WHEN FI.DepartedOnTime THEN 'Yes' ELSE 'No' END as DepartedOnTime, " +
               "CASE WHEN FI.ArrivedOnTime THEN 'Yes' ELSE 'No' END as ArrivedOnTime " +
               "FROM FlightInstance FI " +
               "JOIN Flight F ON FI.FlightNumber = F.FlightNumber " +
               "JOIN Schedule S ON F.FlightNumber = S.FlightNumber " +
               "AND S.DayOfWeek = TRIM(TO_CHAR(FI.FlightDate, 'Day')) " +
               "WHERE FI.FlightDate = '%s' " +
               "ORDER BY S.DepartureTime",
               date
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No flights found for the given date." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature5(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           
           // Validate flight number format
           if (!Validator.isValidFlightNumber(flightNum)) {
               System.out.println(ANSI_RED + "Invalid flight number format! Should be F### (e.g., F100)" + ANSI_RESET);
               return;
           }

           System.out.print("Enter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT R.Status as status, " +
               "COUNT(*) as passenger_count, " +
               "STRING_AGG(C.FirstName || ' ' || C.LastName, ', ' ORDER BY C.LastName, C.FirstName) as passengers " +
               "FROM FlightInstance FI " +
               "JOIN Reservation R ON FI.FlightInstanceID = R.FlightInstanceID " +
               "JOIN Customer C ON R.CustomerID = C.CustomerID " +
               "WHERE FI.FlightNumber = '%s' " +
               "AND FI.FlightDate = '%s' " +
               "GROUP BY R.Status " +
               "ORDER BY " +
               "CASE R.Status " +
               "  WHEN 'flown' THEN 1 " +
               "  WHEN 'reserved' THEN 2 " +
               "  WHEN 'waitlist' THEN 3 " +
               "  ELSE 4 END",
               flightNum, date
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No passengers found for the given flight." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature6(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Reservation ID: ");
           String reservationId = in.readLine();
           
           // Validate reservation ID format
           if (!Validator.isValidReservationId(reservationId)) {
               System.out.println(ANSI_RED + "Invalid reservation ID format! Should be R#### (e.g., R0001, R0123)" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT C.FirstName as firstname, C.LastName as lastname, " +
               "C.Gender as gender, TO_CHAR(C.DOB, 'YYYY-MM-DD') as dob, " +
               "C.Address as address, C.Phone as phone, C.Zip as zip, " +
               "F.FlightNumber as flightnumber, F.DepartureCity as departurecity, " +
               "F.ArrivalCity as arrivalcity, " +
               "TO_CHAR(FI.FlightDate, 'YYYY-MM-DD') as flightdate, " +
               "R.Status as status " +
               "FROM Reservation R " +
               "JOIN Customer C ON R.CustomerID = C.CustomerID " +
               "JOIN FlightInstance FI ON R.FlightInstanceID = FI.FlightInstanceID " +
               "JOIN Flight F ON FI.FlightNumber = F.FlightNumber " +
               "WHERE R.ReservationID = '%s'",
               reservationId
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "Reservation not found." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature7(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Plane ID: ");
           String planeId = in.readLine();
           
           // Validate plane ID format
           if (!Validator.isValidPlaneId(planeId)) {
               System.out.println(ANSI_RED + "Invalid plane ID format! Should be PL### (e.g., PL001)" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT P.PlaneID, P.Make, P.Model, P.Year, " +
               "EXTRACT(YEAR FROM CURRENT_DATE) - P.Year as Age, " +
               "P.LastRepairDate, " +
               "COALESCE(R.RepairCode, 'No repairs') as Last_Repair_Code, " +
               "COALESCE(T.Name, 'No technician') as Last_Repair_Technician " +
               "FROM Plane P " +
               "LEFT JOIN (SELECT PlaneID, RepairCode, TechnicianID, " +
               "          ROW_NUMBER() OVER (PARTITION BY PlaneID ORDER BY RepairDate DESC) as rn " +
               "          FROM Repair) R ON P.PlaneID = R.PlaneID AND R.rn = 1 " +
               "LEFT JOIN Technician T ON R.TechnicianID = T.TechnicianID " +
               "WHERE P.PlaneID = '%s'",
               planeId
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "Plane not found." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature8(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Technician ID: ");
           String techId = in.readLine();
           
           // Validate technician ID format
           if (!Validator.isValidTechId(techId)) {
               System.out.println(ANSI_RED + "Invalid technician ID format! Should be T### (e.g., T001)" + ANSI_RESET);
               return;
           }

           // First check if technician exists
           String checkTech = String.format(
               "SELECT Name FROM Technician WHERE TechnicianID = '%s'",
               techId
           );
           List<List<String>> result = esql.executeQueryAndReturnResult(checkTech);
           if (result.isEmpty()) {
               System.out.println(ANSI_RED + "Technician " + techId + " not found!" + ANSI_RESET);
               return;
           }
           
           String techName = result.get(0).get(0);
           System.out.println(ANSI_BLUE + "\nRepairs performed by " + techName + " (" + techId + "):" + ANSI_RESET);
           
           String query = String.format(
               "SELECT R.RepairID as repairid, " +
               "R.PlaneID as planeid, " +
               "P.Make as make, P.Model as model, " +
               "R.RepairCode as repaircode, " +
               "TO_CHAR(R.RepairDate, 'YYYY-MM-DD') as repairdate, " +
               "T.Name as technician " +
               "FROM Repair R " +
               "JOIN Plane P ON R.PlaneID = P.PlaneID " +
               "JOIN Technician T ON R.TechnicianID = T.TechnicianID " +
               "WHERE R.TechnicianID = '%s' " +
               "ORDER BY R.RepairDate DESC, R.RepairID",
               techId
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No repairs found for technician " + techId + "." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature9(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Plane ID: ");
           String planeId = in.readLine();
           
           // Validate plane ID format
           if (!Validator.isValidPlaneId(planeId)) {
               System.out.println(ANSI_RED + "Invalid plane ID format! Should be PL### (e.g., PL001)" + ANSI_RESET);
               return;
           }
           
           System.out.print("Enter Start Date (YYYY-MM-DD): ");
           String startDate = in.readLine();
           System.out.print("Enter End Date (YYYY-MM-DD): ");
           String endDate = in.readLine();
           
           // Validate date format and range
           try {
               java.time.LocalDate start = java.time.LocalDate.parse(startDate);
               java.time.LocalDate end = java.time.LocalDate.parse(endDate);
               if (start.isAfter(end)) {
                   System.out.println(ANSI_RED + "Start date must be before end date!" + ANSI_RESET);
                   return;
               }
           } catch (Exception e) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }
           
           // First check if plane exists
           String checkPlane = String.format(
               "SELECT COUNT(*) FROM Plane WHERE PlaneID = '%s'",
               planeId
           );
           List<List<String>> result = esql.executeQueryAndReturnResult(checkPlane);
           if (Integer.parseInt(result.get(0).get(0)) == 0) {
               System.out.println(ANSI_RED + "Plane " + planeId + " not found!" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "SELECT R.RepairID as repairid, " +
               "R.PlaneID as planeid, " +
               "P.Make as make, P.Model as model, " +
               "P.Year as year, " +
               "R.RepairCode as repaircode, " +
               "TO_CHAR(R.RepairDate, 'YYYY-MM-DD') as repairdate, " +
               "T.Name as technician " +
               "FROM Repair R " +
               "JOIN Plane P ON R.PlaneID = P.PlaneID " +
               "JOIN Technician T ON R.TechnicianID = T.TechnicianID " +
               "WHERE R.PlaneID = '%s' " +
               "AND R.RepairDate BETWEEN '%s' AND '%s' " +
               "ORDER BY R.RepairDate DESC, R.RepairID",
               planeId, startDate, endDate
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No repairs found for plane " + planeId + " in the given date range." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature10(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           
           // Validate flight number format
           if (!Validator.isValidFlightNumber(flightNum)) {
               System.out.println(ANSI_RED + "Invalid flight number format! Should be F### (e.g., F100)" + ANSI_RESET);
               return;
           }

           System.out.print("Enter Start Date (YYYY-MM-DD): ");
           String startDate = in.readLine();
           System.out.print("Enter End Date (YYYY-MM-DD): ");
           String endDate = in.readLine();
           
           // Validate date format and range
           try {
               java.time.LocalDate start = java.time.LocalDate.parse(startDate);
               java.time.LocalDate end = java.time.LocalDate.parse(endDate);
               if (start.isAfter(end)) {
                   System.out.println(ANSI_RED + "Start date must be before end date!" + ANSI_RESET);
                   return;
               }
           } catch (Exception e) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }

           // First check if flight exists
           String checkFlight = String.format(
               "SELECT COUNT(*) FROM Flight WHERE FlightNumber = '%s'",
               flightNum
           );
           List<List<String>> result = esql.executeQueryAndReturnResult(checkFlight);
           if (Integer.parseInt(result.get(0).get(0)) == 0) {
               System.out.println(ANSI_RED + "Flight " + flightNum + " not found!" + ANSI_RESET);
               return;
           }
           
           String query = String.format(
               "WITH FlightStats AS ( " +
               "    SELECT " +
               "        COUNT(*) as total_runs, " +
               "        SUM(SeatsSold) as total_tickets_sold, " +
               "        SUM(SeatsTotal - SeatsSold) as total_unsold, " +
               "        SUM(CASE WHEN DepartedOnTime AND ArrivedOnTime THEN 1 ELSE 0 END) as on_time_flights, " +
               "        AVG(CAST(TicketCost AS NUMERIC)) as avg_ticket_price " +
               "    FROM FlightInstance " +
               "    WHERE FlightNumber = '%s' " +
               "    AND FlightDate BETWEEN '%s' AND '%s' " +
               ") " +
               "SELECT " +
               "    '%s' as flightnumber, " +
               "    COALESCE(total_runs, 0) as total_flights, " +
               "    COALESCE(total_tickets_sold, 0) as tickets_sold, " +
               "    COALESCE(total_unsold, 0) as unsold_seats, " +
               "    COALESCE(ROUND(CAST(CAST(on_time_flights AS FLOAT) / NULLIF(total_runs, 0) * 100 AS NUMERIC), 2), 0.00) as on_time_percentage, " +
               "    COALESCE(ROUND(CAST(avg_ticket_price AS NUMERIC), 2), 0.00) as average_ticket_price " +
               "FROM FlightStats",
               flightNum, startDate, endDate, flightNum
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println(ANSI_YELLOW + "No flight instances found in the given date range." + ANSI_RESET);
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature12(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           System.out.print("Enter Date (YYYY-MM-DD): ");
           String date = in.readLine();
           
           // Validate date format
           try {
               java.time.LocalDate.parse(date);
           } catch (Exception e) {
               System.out.println("Invalid date format! Please use YYYY-MM-DD");
               return;
           }
           
           String query = String.format(
               "SELECT F.FlightNumber, F.DepartureCity, F.ArrivalCity, " +
               "FI.FlightDate, FI.TicketCost, " +
               "FI.SeatsTotal, FI.SeatsSold, " +
               "(FI.SeatsTotal - FI.SeatsSold) as SeatsAvailable " +
               "FROM FlightInstance FI " +
               "JOIN Flight F ON FI.FlightNumber = F.FlightNumber " +
               "WHERE FI.FlightNumber = '%s' " +
               "AND FI.FlightDate = '%s'",
               flightNum, date
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println("Flight not found for the given date.");
           }
       } catch (Exception e) {
           System.err.println(e.getMessage());
       }
   }

   public static void feature13(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           
           String query = String.format(
               "SELECT F.FlightNumber, " +
               "P.PlaneID, P.Make, P.Model, P.Year, " +
               "EXTRACT(YEAR FROM CURRENT_DATE) - P.Year as Age " +
               "FROM Flight F " +
               "JOIN Plane P ON F.PlaneID = P.PlaneID " +
               "WHERE F.FlightNumber = '%s'",
               flightNum
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println("Flight not found.");
           }
       } catch (Exception e) {
           System.err.println(e.getMessage());
       }
   }

   public static void feature14(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Flight Number: ");
           String flightNum = in.readLine();
           System.out.print("Enter Flight Date (YYYY-MM-DD): ");
           String flightDate = in.readLine();
           System.out.print("Enter Customer ID: ");
           String customerId = in.readLine();
           
           // Validate flight number format
           if (!Validator.isValidFlightNumber(flightNum)) {
               System.out.println(ANSI_RED + "Invalid flight number format! Should be like 'F###'" + ANSI_RESET);
               return;
           }
           
           // Validate date format
           if (!Validator.isValidDate(flightDate)) {
               System.out.println(ANSI_RED + "Invalid date format! Please use YYYY-MM-DD" + ANSI_RESET);
               return;
           }
           
           esql.executeUpdate("BEGIN TRANSACTION");
           
           try {
               // Check if flight instance exists and get seats info
               String checkQuery = String.format(
                   "SELECT FI.FlightInstanceID, FI.SeatsTotal, FI.SeatsSold " +
                   "FROM FlightInstance FI " +
                   "WHERE FI.FlightNumber = '%s' AND FI.FlightDate = '%s'",
                   flightNum, flightDate
               );
               
               List<List<String>> result = esql.executeQueryAndReturnResult(checkQuery);
               if (result.isEmpty()) {
                   throw new Exception("Flight instance not found!");
               }
               
               String flightInstanceId = result.get(0).get(0);
               int seatsTotal = Integer.parseInt(result.get(0).get(1));
               int seatsSold = Integer.parseInt(result.get(0).get(2));
               
               // Check if customer exists
               if (!DBChecker.customerExists(esql, customerId)) {
                   throw new Exception("Customer ID not found!");
               }
               
               // Generate next reservation ID
               String getMaxId = "SELECT COALESCE(MAX(CAST(SUBSTRING(ReservationID FROM 2) AS INTEGER)), 0) FROM Reservation";
               result = esql.executeQueryAndReturnResult(getMaxId);
               int nextNum = Integer.parseInt(result.get(0).get(0)) + 1;
               String newReservationId = String.format("R%03d", nextNum);
               
               String status;
               if (seatsSold < seatsTotal) {
                   status = "reserved";
                   // Update seats sold
                   String updateSeats = String.format(
                       "UPDATE FlightInstance " +
                       "SET SeatsSold = SeatsSold + 1 " +
                       "WHERE FlightInstanceID = '%s'",
                       flightInstanceId
                   );
                   esql.executeUpdate(updateSeats);
               } else {
                   status = "waitlist";
               }
               
               // Insert reservation
               String insertReservation = String.format(
                   "INSERT INTO Reservation " +
                   "(ReservationID, CustomerID, FlightInstanceID, Status) " +
                   "VALUES ('%s', '%s', '%s', '%s')",
                   newReservationId, customerId, flightInstanceId, status
               );
               esql.executeUpdate(insertReservation);
               
               // Commit transaction
               esql.executeUpdate("COMMIT");
               
               System.out.println(ANSI_GREEN + "Reservation created successfully!" + ANSI_RESET);
               System.out.println("Reservation ID: " + newReservationId);
               System.out.println("Status: " + status);
               
           } catch (Exception e) {
               // Rollback on error
               esql.executeUpdate("ROLLBACK");
               throw e;
           }
           
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error making reservation: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature15(AirlineManagement esql) {
       // same as feature9 - reuse the code
       feature9(esql);
   }

   public static void feature16(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Pilot ID: ");
           String pilotId = in.readLine();
           
           // Validate pilot ID format
           if (!Validator.isValidPilotId(pilotId)) {
               System.out.println(ANSI_RED + "Invalid pilot ID format! Should be P### (e.g., P001)" + ANSI_RESET);
               return;
           }

           String query = String.format(
               "SELECT MR.RequestID, MR.PlaneID, " +
               "P.Make, P.Model, " +
               "MR.RepairCode, MR.RequestDate, " +
               "PI.Name as Pilot " +
               "FROM MaintenanceRequest MR " +
               "JOIN Plane P ON MR.PlaneID = P.PlaneID " +
               "JOIN Pilot PI ON MR.PilotID = PI.PilotID " +
               "WHERE MR.PilotID = '%s' " +
               "ORDER BY MR.RequestDate DESC",
               pilotId
           );
           
           int rowCount = esql.executeQueryAndPrintResult(query);
           if (rowCount == 0) {
               System.out.println("No maintenance requests found for this pilot.");
           }
       } catch (Exception e) {
           System.err.println(e.getMessage());
       }
   }

   public static void feature17(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Plane ID: ");
           String planeId = in.readLine();
           
           // Validate plane ID format
           if (!Validator.isValidPlaneId(planeId)) {
               System.out.println(ANSI_RED + "Invalid plane ID format! Should be PL### (e.g., PL001)" + ANSI_RESET);
               return;
           }
           
           System.out.print("Enter Repair Code: ");
           String repairCode = in.readLine();

           // Validate Repair Code format
           if (!Validator.isValidRepairCode(repairCode)) {
               System.out.println(ANSI_RED + "Invalid Repair Code format! Should be RC### (e.g., RC001)" + ANSI_RESET);
               return;
           }

           System.out.print("Enter Technician ID: ");
           String techId = in.readLine();
           
           // Validate technician ID format
           if (!Validator.isValidTechId(techId)) {
               System.out.println(ANSI_RED + "Invalid technician ID format! Should be T### (e.g., T001)" + ANSI_RESET);
               return;
           }
           
           // Start transaction
           esql.executeUpdate("BEGIN TRANSACTION");
           
           try {
               // Verify plane exists
               String checkPlane = String.format(
                   "SELECT COUNT(*) FROM Plane WHERE PlaneID = '%s'",
                   planeId
               );
               List<List<String>> result = esql.executeQueryAndReturnResult(checkPlane);
               if (Integer.parseInt(result.get(0).get(0)) == 0) {
                   throw new Exception("Plane ID not found!");
               }
               
               // Verify technician exists
               String checkTech = String.format(
                   "SELECT COUNT(*) FROM Technician WHERE TechnicianID = '%s'",
                   techId
               );
               result = esql.executeQueryAndReturnResult(checkTech);
               if (Integer.parseInt(result.get(0).get(0)) == 0) {
                   throw new Exception("Technician ID not found!");
               }
               
               // Get next repair ID
               String getNextId = "SELECT COALESCE(MAX(RepairID) + 1, 1) FROM Repair";
               result = esql.executeQueryAndReturnResult(getNextId);
               int newRepairId = Integer.parseInt(result.get(0).get(0));
               
               // Insert repair record
               String insertRepair = String.format(
                   "INSERT INTO Repair " +
                   "(RepairID, PlaneID, RepairCode, RepairDate, TechnicianID) " +
                   "VALUES (%d, '%s', '%s', CURRENT_DATE, '%s')",
                   newRepairId, planeId, repairCode, techId
               );
               esql.executeUpdate(insertRepair);
               
               // Update plane's last repair date
               String updatePlane = String.format(
                   "UPDATE Plane " +
                   "SET LastRepairDate = CURRENT_DATE " +
                   "WHERE PlaneID = '%s'",
                   planeId
               );
               esql.executeUpdate(updatePlane);
               
               // Commit transaction
               esql.executeUpdate("COMMIT");
               
               System.out.println(ANSI_GREEN + "Repair record created successfully!" + ANSI_RESET);
               
           } catch (Exception e) {
               // Rollback on error
               esql.executeUpdate("ROLLBACK");
               throw e;
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error logging repair: " + e.getMessage() + ANSI_RESET);
       }
   }

   public static void feature18(AirlineManagement esql) {
       try {
           System.out.print("\nEnter Plane ID: ");
           String planeId = in.readLine();
           
           // Validate plane ID format
           if (!Validator.isValidPlaneId(planeId)) {
               System.out.println(ANSI_RED + "Invalid plane ID format! Should be PL### (e.g., PL001)" + ANSI_RESET);
               return;
           }
           
           System.out.print("Enter Repair Code: ");
           String repairCode = in.readLine();

           // Validate Repair Code format
           if (!Validator.isValidRepairCode(repairCode)) {
               System.out.println(ANSI_RED + "Invalid Repair Code format! Should be RC### (e.g., RC001)" + ANSI_RESET);
               return;
           }

           System.out.print("Enter Pilot ID: ");
           String pilotId = in.readLine();
           
           // Validate pilot ID format
           if (!Validator.isValidPilotId(pilotId)) {
               System.out.println(ANSI_RED + "Invalid pilot ID format! Should be P### (e.g., P001)" + ANSI_RESET);
               return;
           }
           
           // Start transaction
           esql.executeUpdate("BEGIN TRANSACTION");
           
           try {
               // Verify plane exists
               String checkPlane = String.format(
                   "SELECT COUNT(*) FROM Plane WHERE PlaneID = '%s'",
                   planeId
               );
               List<List<String>> result = esql.executeQueryAndReturnResult(checkPlane);
               if (Integer.parseInt(result.get(0).get(0)) == 0) {
                   throw new Exception("Plane ID not found!");
               }
               
               // Verify pilot exists
               String checkPilot = String.format(
                   "SELECT COUNT(*) FROM Pilot WHERE PilotID = '%s'",
                   pilotId
               );
               result = esql.executeQueryAndReturnResult(checkPilot);
               if (Integer.parseInt(result.get(0).get(0)) == 0) {
                   throw new Exception("Pilot ID not found!");
               }
               
               // Get next request ID
               String getNextId = "SELECT COALESCE(MAX(RequestID) + 1, 1) FROM MaintenanceRequest";
               result = esql.executeQueryAndReturnResult(getNextId);
               int newRequestId = Integer.parseInt(result.get(0).get(0));
               
               // Insert maintenance request
               String insertRequest = String.format(
                   "INSERT INTO MaintenanceRequest " +
                   "(RequestID, PlaneID, RepairCode, RequestDate, PilotID) " +
                   "VALUES (%d, '%s', '%s', CURRENT_DATE, '%s')",
                   newRequestId, planeId, repairCode, pilotId
               );
               esql.executeUpdate(insertRequest);
               
               // Commit transaction
               esql.executeUpdate("COMMIT");
               
               System.out.println(ANSI_GREEN + "Maintenance request submitted successfully!" + ANSI_RESET);
               
           } catch (Exception e) {
               // Rollback on error
               esql.executeUpdate("ROLLBACK");
               throw e;
           }
       } catch (Exception e) {
           System.err.println(ANSI_RED + "Error submitting maintenance request: " + e.getMessage() + ANSI_RESET);
       }
   }

   private static void showManagerMenu(AirlineManagement esql) {
       boolean keepon = true;
       while (keepon) {
           System.out.println(ANSI_BLUE + "\nMANAGER MENU" + ANSI_RESET);
           System.out.println("-----------");
           System.out.println("1. View Weekly Schedule for Flight");
           System.out.println("2. View Seats Available/Sold for Date");
           System.out.println("3. View On-time Info for Flight Date");
           System.out.println("4. View All Flights for Date");
           System.out.println("5. View Passenger Lists by Status");
           System.out.println("6. View Traveler Info by Reservation");
           System.out.println("7. View Plane Details and Last Repair");
           System.out.println("8. View All Repairs by Tech");
           System.out.println("9. View Repairs for Plane in Date Range");
           System.out.println("10. View Flight Stats for Date Range");
           System.out.println("h. Help");
           System.out.println("c. Clear Screen");
           System.out.println(".........................");
           System.out.println("20. Log Out");
           
           int choice = readChoice();
           switch (choice) {
               case -1: showHelp(MANAGER_HELP); break;
               case -2: clearScreen(); break;
               case 1: feature1(esql); break;
               case 2: feature2(esql); break;
               case 3: feature3(esql); break;
               case 4: feature4(esql); break;
               case 5: feature5(esql); break;
               case 6: feature6(esql); break;
               case 7: feature7(esql); break;
               case 8: feature8(esql); break;
               case 9: feature9(esql); break;
               case 10: feature10(esql); break;
               case 20: keepon = false; break;
               default: System.out.println("Invalid choice!"); break;
           }
       }
   }

   private static void showCustomerMenu(AirlineManagement esql) {
       boolean keepon = true;
       while (keepon) {
           System.out.println("\nCUSTOMER MENU");
           System.out.println("-------------");
           System.out.println("11. Search Flights by Cities and Date");
           System.out.println("12. View Ticket Cost");
           System.out.println("13. View Airplane Type");
           System.out.println("14. Make Reservation");
           System.out.println(".........................");
           System.out.println("20. Log Out");
           
           switch (readChoice()) {
               case 11: feature11(esql); break;
               case 12: feature12(esql); break;
               case 13: feature13(esql); break;
               case 14: feature14(esql); break;
               case 20: keepon = false; break;
               default: System.out.println("Invalid choice!"); break;
           }
       }
   }

   private static void showPilotMenu(AirlineManagement esql) {
       boolean keepon = true;
       while (keepon) {
           System.out.println("\nPILOT MENU");
           System.out.println("----------");
           System.out.println("18. Submit Maintenance Request");
           System.out.println(".........................");
           System.out.println("20. Log Out");
           
           switch (readChoice()) {
               case 18: feature18(esql); break;
               case 20: keepon = false; break;
               default: System.out.println("Invalid choice!"); break;
           }
       }
   }

   private static void showTechnicianMenu(AirlineManagement esql) {
       boolean keepon = true;
       while (keepon) {
           System.out.println("\nTECHNICIAN MENU");
           System.out.println("---------------");
           System.out.println("15. View Repairs for Plane");
           System.out.println("16. View Maintenance Requests by Pilot");
           System.out.println("17. Log New Repair");
           System.out.println(".........................");
           System.out.println("20. Log Out");
           
           switch (readChoice()) {
               case 15: feature15(esql); break;
               case 16: feature16(esql); break;
               case 17: feature17(esql); break;
               case 20: keepon = false; break;
               default: System.out.println("Invalid choice!"); break;
           }
       }
   }

   public static void clearScreen() {
       System.out.print("\033[H\033[2J");
       System.out.flush();
   }

   public static void showHelp(String[][] helpMenu) {
       System.out.println("\nAvailable Commands:");
       System.out.println("------------------");
       for (String[] help : helpMenu) {
           System.out.printf("%-4s: %s%n", help[0], help[1]);
       }
       System.out.println("\nPress Enter to continue...");
       try {
           in.readLine();
       } catch (Exception e) {}
   }

   // AuthService inner class for login handling
   private static class AuthService {
       public static String checkLogin(AirlineManagement esql, String username, String password) {
           try {
               MessageDigest digest = MessageDigest.getInstance("SHA-256");
               byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
               String hashHex = bytesToHex(hash);

               String query = String.format(
                   "SELECT role FROM users WHERE username = '%s' AND pass_hash = '%s'",
                   username, hashHex
               );
               List<List<String>> result = esql.executeQueryAndReturnResult(query);
               return result.isEmpty() ? null : result.get(0).get(0);
           } catch (Exception e) {
               System.err.println(ANSI_RED + "Error during login: " + e.getMessage() + ANSI_RESET);
               return null;
           }
       }

       private static String bytesToHex(byte[] hash) {
           StringBuilder hexString = new StringBuilder();
           for (byte b : hash) {
               String hex = Integer.toHexString(0xff & b);
               if (hex.length() == 1) hexString.append('0');
               hexString.append(hex);
           }
           return hexString.toString();
       }
   }

   public static void printTableHeader(ResultSetMetaData rsmd, int numCol) throws SQLException {
       StringBuilder border = new StringBuilder();
       for (int i = 0; i < numCol; i++) {
           border.append(TABLE_BORDER);
       }
       System.out.println(ANSI_CYAN + border.toString() + ANSI_RESET);
       
       for (int i = 1; i <= numCol; i++) {
           System.out.printf(ANSI_BLUE + TABLE_FORMAT + ANSI_RESET, rsmd.getColumnName(i));
       }
       System.out.println();
       
       System.out.println(ANSI_CYAN + border.toString() + ANSI_RESET);
   }
   
   public static void printTableRow(ResultSet rs, int numCol) throws SQLException {
       for (int i = 1; i <= numCol; i++) {
           String value = rs.getString(i);
           if (value == null) value = "NULL";
           System.out.printf(TABLE_FORMAT, value);
       }
       System.out.println();
   }

}//end AirlineManagement

