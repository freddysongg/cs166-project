/* Replace the location to where you saved the data files*/

\copy Plane FROM 'data/Plane.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Flight FROM 'data/Flight.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Schedule FROM 'data/Schedule.csv' WITH DELIMITER ',' CSV HEADER; 

\copy FlightInstance FROM 'data/FlightInstance.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Customer FROM 'data/Customer.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Reservation FROM 'data/Reservation.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Technician FROM 'data/Technician.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Repair FROM 'data/Repair.csv' WITH DELIMITER ',' CSV HEADER; 

\copy Pilot FROM 'data/Pilot.csv' WITH DELIMITER ',' CSV HEADER; 

\copy MaintenanceRequest FROM 'data/MaintenanceRequest.csv' WITH DELIMITER ',' CSV HEADER; 

