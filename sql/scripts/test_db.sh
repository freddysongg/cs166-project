#!/bin/bash

# Get the current directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'
BOLD='\033[1m'

# Database info
DB_NAME=$USER"_project_phase_3_DB"
PSQL="$DIR/../../postgresql/bin/psql"

if [ ! -f "$PSQL" ]; then
    PSQL=$(which psql)
fi

echo -e "${BLUE}${BOLD}=== Running Airline Management System Tests ===${NC}\n"

echo -e "${YELLOW}Running tests...${NC}"
$PSQL -h localhost -p $PGPORT $DB_NAME << EOF
\set QUIET 1
\pset format aligned
\pset border 2
\pset null '[NULL]'
\x off

-- Set up custom formats for better output
\pset linestyle unicode
\pset title '╔═══════════════════════════════════════════════════════════════╗'

-- Run the tests
\i '$DIR/../src/run_tests.sql'
EOF

echo -e "\n${GREEN}${BOLD}Tests completed!${NC}\n" 