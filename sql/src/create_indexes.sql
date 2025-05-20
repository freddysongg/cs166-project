-- FlightInstance(FlightNumber, FlightDate) - speeds #2, 3, 4, 10, 11
CREATE INDEX idx_flightinstance_flight_date ON FlightInstance(FlightNumber, FlightDate);

-- Reservation(FlightInstanceID, Status) - speeds #5
CREATE INDEX idx_reservation_status ON Reservation(FlightInstanceID, Status);

-- Repair(PlaneID, RepairDate) - speeds #8, 9, 15
CREATE INDEX idx_repair_plane_date ON Repair(PlaneID, RepairDate);

-- MaintenanceRequest(PilotID) - speeds #16
CREATE INDEX idx_maintenance_pilot ON MaintenanceRequest(PilotID);

-- Flight(DepartureCity, ArrivalCity) - composite for search flights (#11)
CREATE INDEX idx_flight_cities ON Flight(DepartureCity, ArrivalCity);
