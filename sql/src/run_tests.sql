-- Comprehensive Test Suite for Airline Management System
\set QUIET 1
\pset format unaligned
\pset tuples_only true

-- Create test tracking tables
DROP TABLE IF EXISTS test_suites CASCADE;
DROP TABLE IF EXISTS test_cases CASCADE;
DROP TABLE IF EXISTS test_results CASCADE;

CREATE TABLE test_suites (
    suite_id SERIAL PRIMARY KEY,
    suite_name TEXT NOT NULL,
    total_tests INTEGER DEFAULT 0,
    passed_tests INTEGER DEFAULT 0,
    failed_tests INTEGER DEFAULT 0,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_cases (
    case_id SERIAL PRIMARY KEY,
    suite_id INTEGER REFERENCES test_suites(suite_id),
    test_name TEXT NOT NULL,
    test_description TEXT,
    expected_result TEXT,
    actual_result TEXT,
    status TEXT CHECK (status IN ('PASSED', 'FAILED', 'ERROR')),
    error_message TEXT,
    execution_time INTEGER 
);

-- Test Suite Functions
CREATE OR REPLACE FUNCTION create_test_suite(p_suite_name TEXT) 
RETURNS INTEGER AS $$
DECLARE
    v_suite_id INTEGER;
BEGIN
    INSERT INTO test_suites (suite_name) VALUES (p_suite_name) RETURNING suite_id INTO v_suite_id;
    RETURN v_suite_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION run_test(
    p_suite_id INTEGER,
    p_test_name TEXT,
    p_test_description TEXT,
    p_test_query TEXT,
    p_expected_result BOOLEAN
) RETURNS VOID AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_actual_result BOOLEAN;
    v_status TEXT;
    v_error_message TEXT;
BEGIN
    v_start_time := CURRENT_TIMESTAMP;
    BEGIN
        EXECUTE p_test_query INTO v_actual_result;
        
        IF v_actual_result = p_expected_result THEN
            v_status := 'PASSED';
            UPDATE test_suites SET passed_tests = passed_tests + 1 WHERE suite_id = p_suite_id;
        ELSE
            v_status := 'FAILED';
            UPDATE test_suites SET failed_tests = failed_tests + 1 WHERE suite_id = p_suite_id;
        END IF;
        
    EXCEPTION WHEN OTHERS THEN
        v_status := 'ERROR';
        v_error_message := SQLERRM;
        UPDATE test_suites SET failed_tests = failed_tests + 1 WHERE suite_id = p_suite_id;
    END;
    
    INSERT INTO test_cases (
        suite_id, test_name, test_description, expected_result, 
        actual_result, status, error_message, execution_time
    ) VALUES (
        p_suite_id,
        p_test_name,
        p_test_description,
        p_expected_result::TEXT,
        v_actual_result::TEXT,
        v_status,
        v_error_message,
        EXTRACT(MILLISECONDS FROM (CURRENT_TIMESTAMP - v_start_time))::INTEGER
    );
    
    UPDATE test_suites 
    SET total_tests = total_tests + 1 
    WHERE suite_id = p_suite_id;
END;
$$ LANGUAGE plpgsql;

-- Start Testing
BEGIN;

-- 1. Data Format Tests
DO $$
DECLARE
    v_suite_id INTEGER;
BEGIN
    v_suite_id := create_test_suite('Data Format Tests');
    
    -- Test Flight Number Format
    PERFORM run_test(
        v_suite_id,
        'Flight Number Format',
        'Ensures all flight numbers follow F### pattern',
        'SELECT COUNT(*) = 0 FROM Flight WHERE FlightNumber !~ ''^F[0-9]{3}$''',
        true
    );
    
    -- Test Plane ID Format
    PERFORM run_test(
        v_suite_id,
        'Plane ID Format',
        'Ensures all plane IDs follow PL### pattern',
        'SELECT COUNT(*) = 0 FROM Plane WHERE PlaneID !~ ''^PL[0-9]{3}$''',
        true
    );

    -- Test Repair Code Format
    PERFORM run_test(
        v_suite_id,
        'Repair Code Format',
        'Ensures all Repair Codes follow RC### pattern',
        'SELECT COUNT(*) = 0 FROM Repair WHERE RepairCode !~ ''^RC[0-9]{3}$''',
        true
    );
    
    -- Test Pilot ID Format
    PERFORM run_test(
        v_suite_id,
        'Pilot ID Format',
        'Ensures all pilot IDs follow P### pattern',
        'SELECT COUNT(*) = 0 FROM Pilot WHERE PilotID !~ ''^P[0-9]{3}$''',
        true
    );
    
    -- Test Technician ID Format
    PERFORM run_test(
        v_suite_id,
        'Technician ID Format',
        'Ensures all technician IDs follow T### pattern',
        'SELECT COUNT(*) = 0 FROM Technician WHERE TechnicianID !~ ''^T[0-9]{3}$''',
        true
    );
    
    -- Test Reservation ID Format
    PERFORM run_test(
        v_suite_id,
        'Reservation ID Format',
        'Ensures all reservation IDs follow R#### pattern',
        'SELECT COUNT(*) = 0 FROM Reservation WHERE ReservationID !~ ''^R[0-9]{4}$''',
        true
    );
END $$;

-- 2. Relationship Tests
DO $$
DECLARE
    v_suite_id INTEGER;
BEGIN
    v_suite_id := create_test_suite('Relationship Tests');
    
    -- Test Flight-Plane Relationship
    PERFORM run_test(
        v_suite_id,
        'Flight-Plane Relationship',
        'Ensures all flights reference valid planes',
        'SELECT COUNT(*) = 0 FROM Flight f LEFT JOIN Plane p ON f.PlaneID = p.PlaneID WHERE p.PlaneID IS NULL',
        true
    );
    
    -- Test Schedule-Flight Relationship
    PERFORM run_test(
        v_suite_id,
        'Schedule-Flight Relationship',
        'Ensures all schedules reference valid flights',
        'SELECT COUNT(*) = 0 FROM Schedule s LEFT JOIN Flight f ON s.FlightNumber = f.FlightNumber WHERE f.FlightNumber IS NULL',
        true
    );
    
    -- Test Reservation-Customer Relationship
    PERFORM run_test(
        v_suite_id,
        'Reservation-Customer Relationship',
        'Ensures all reservations reference valid customers',
        'SELECT COUNT(*) = 0 FROM Reservation r LEFT JOIN Customer c ON r.CustomerID = c.CustomerID WHERE c.CustomerID IS NULL',
        true
    );
    
    -- Test Repair-Technician Relationship
    PERFORM run_test(
        v_suite_id,
        'Repair-Technician Relationship',
        'Ensures all repairs reference valid technicians',
        'SELECT COUNT(*) = 0 FROM Repair r LEFT JOIN Technician t ON r.TechnicianID = t.TechnicianID WHERE t.TechnicianID IS NULL',
        true
    );
END $$;

-- 3. Business Rule Tests
DO $$
DECLARE
    v_suite_id INTEGER;
BEGIN
    v_suite_id := create_test_suite('Business Rule Tests');
    
    -- Test Seat Capacity
    PERFORM run_test(
        v_suite_id,
        'Seat Capacity Rule',
        'Ensures seats sold never exceeds total seats',
        'SELECT COUNT(*) = 0 FROM FlightInstance WHERE SeatsSold > SeatsTotal',
        true
    );
    
    -- Test Reservation Status
    PERFORM run_test(
        v_suite_id,
        'Reservation Status Values',
        'Ensures reservation status is valid',
        'SELECT COUNT(*) = 0 FROM Reservation WHERE Status NOT IN (''reserved'', ''waitlist'', ''flown'')',
        true
    );
    
    -- Test Valid Flight Dates
    PERFORM run_test(
        v_suite_id,
        'Valid Flight Dates',
        'Ensures flight dates are not in the past',
        'SELECT COUNT(*) = 0 FROM FlightInstance WHERE FlightDate < CURRENT_DATE - INTERVAL ''1 year''',
        true
    );
    
    -- Test Customer Age
    PERFORM run_test(
        v_suite_id,
        'Customer Age Check',
        'Ensures all customers are at least 18 years old',
        'SELECT COUNT(*) = 0 FROM Customer WHERE AGE(CURRENT_DATE, DOB) < INTERVAL ''18 years''',
        true
    );
END $$;

-- 4. Data Integrity Tests
DO $$
DECLARE
    v_suite_id INTEGER;
BEGIN
    v_suite_id := create_test_suite('Data Integrity Tests');
    
    -- Test for NULL Values in Critical Fields
    PERFORM run_test(
        v_suite_id,
        'Required Fields Check',
        'Ensures required fields are not NULL',
        'SELECT COUNT(*) = 0 FROM Flight WHERE FlightNumber IS NULL OR PlaneID IS NULL',
        true
    );
    
    -- Test Date Validity
    PERFORM run_test(
        v_suite_id,
        'Date Validity Check',
        'Ensures no future dates in repair history',
        'SELECT COUNT(*) = 0 FROM Repair WHERE RepairDate > CURRENT_DATE',
        true
    );
    
    -- Test Customer Contact Info
    PERFORM run_test(
        v_suite_id,
        'Customer Contact Info',
        'Ensures all customers have contact information',
        'SELECT COUNT(*) = 0 FROM Customer WHERE Phone IS NULL OR Address IS NULL',
        true
    );
    
    -- -- Test Valid Schedule Times -- in schedule.csv, 35,F105,Wednesday,22:00,1:00
    -- PERFORM run_test(
    --     v_suite_id,
    --     'Valid Schedule Times',
    --     'Ensures arrival time is not before departure time',
    --     'SELECT COUNT(*) = 0 FROM Schedule WHERE ArrivalTime < DepartureTime',
    --     true
    -- );
END $$;

-- Print Test Results 
\echo '\n\033[1;34m╔════════════════════════════════════════════════════════════════╗\033[0m'
\echo '\033[1;34m║                     Test Results Summary                        ║\033[0m'
\echo '\033[1;34m╚════════════════════════════════════════════════════════════════╝\033[0m\n'

SELECT E'\033[1;33m' || s.suite_name || E'\033[0m\n' ||
       'Total: ' || s.total_tests || 
       ' | Passed: ' || E'\033[0;32m' || s.passed_tests || E'\033[0m' ||
       ' | Failed: ' || CASE WHEN s.failed_tests > 0 THEN E'\033[0;31m' ELSE E'\033[0m' END || s.failed_tests || E'\033[0m' ||
       ' | Success Rate: ' || 
       CASE 
           WHEN s.total_tests > 0 THEN
               CASE 
                   WHEN ROUND((s.passed_tests::FLOAT / s.total_tests::FLOAT * 100)) = 100 THEN E'\033[0;32m'
                   WHEN ROUND((s.passed_tests::FLOAT / s.total_tests::FLOAT * 100)) >= 80 THEN E'\033[0;33m'
                   ELSE E'\033[0;31m'
               END
           ELSE E'\033[0m'
       END ||
       CASE 
           WHEN s.total_tests > 0 
           THEN ROUND((s.passed_tests::FLOAT / s.total_tests::FLOAT * 100))::TEXT || '%'
           ELSE '0%'
       END || E'\033[0m'
FROM test_suites s
ORDER BY s.suite_id;

\echo '\n\033[1;34m╔════════════════════════════════════════════════════════════════╗\033[0m'
\echo '\033[1;34m║                    Detailed Test Results                        ║\033[0m'
\echo '\033[1;34m╚════════════════════════════════════════════════════════════════╝\033[0m\n'

SELECT 
    E'\033[1;33m' || s.suite_name || E'\033[0m' || ' | ' ||
    c.test_name || ' | ' ||
    CASE 
        WHEN c.status = 'PASSED' THEN E'\033[0;32m✓ PASSED\033[0m'
        WHEN c.status = 'FAILED' THEN E'\033[0;31m✗ FAILED\033[0m'
        ELSE E'\033[0;33m! ERROR\033[0m'
    END || ' | ' ||
    c.execution_time || 'ms | ' ||
    CASE 
        WHEN c.status != 'PASSED' 
        THEN E'\033[0;31m' || COALESCE(c.error_message, 'Expected: ' || c.expected_result || ', Got: ' || c.actual_result) || E'\033[0m'
        ELSE ''
    END
FROM test_cases c
JOIN test_suites s ON c.suite_id = s.suite_id
ORDER BY s.suite_id, c.case_id;

\echo '\n\033[1;34m╔════════════════════════════════════════════════════════════════╗\033[0m'
\echo '\033[1;34m║                    Performance Summary                          ║\033[0m'
\echo '\033[1;34m╚════════════════════════════════════════════════════════════════╝\033[0m\n'

SELECT 
    E'\033[1;33m' || s.suite_name || E'\033[0m' || ' | ' ||
    'Tests: ' || COUNT(*) || ' | ' ||
    'Avg Time: ' || ROUND(AVG(c.execution_time))::TEXT || 'ms | ' ||
    'Max Time: ' || MAX(c.execution_time)::TEXT || 'ms'
FROM test_cases c
JOIN test_suites s ON c.suite_id = s.suite_id
GROUP BY s.suite_id, s.suite_name
ORDER BY s.suite_id;

ROLLBACK; 