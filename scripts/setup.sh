#!/bin/bash

# Setup script for Cloud File Manager
# This script helps set up the development environment

set -e

echo "🚀 Setting up Cloud File Manager..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi
echo "✅ Java found: $(java -version 2>&1 | head -n 1)"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6+."
    exit 1
fi
echo "✅ Maven found: $(mvn -version | head -n 1)"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+."
    exit 1
fi
echo "✅ Node.js found: $(node -v)"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "⚠️  PostgreSQL client not found. Make sure PostgreSQL is installed and running."
else
    echo "✅ PostgreSQL client found"
fi

# Setup Backend
echo -e "\n${BLUE}Setting up backend...${NC}"
cd backend
if [ ! -f "pom.xml" ]; then
    echo "❌ pom.xml not found in backend directory"
    exit 1
fi
echo "Installing backend dependencies..."
mvn clean install -DskipTests
echo -e "${GREEN}✅ Backend setup complete${NC}"
cd ..

# Setup Frontend (React)
echo -e "\n${BLUE}Setting up frontend (React)...${NC}"
cd frontend
if [ ! -f "package.json" ]; then
    echo "❌ package.json not found in frontend directory"
    exit 1
fi
echo "Installing frontend dependencies..."
npm install
echo -e "${GREEN}✅ Frontend setup complete${NC}"
cd ..

# Create environment file for React
if [ ! -f "frontend/.env" ]; then
    echo "Creating .env file for React..."
    echo "VITE_API_BASE_URL=http://localhost:8080" > frontend/.env
    echo -e "${GREEN}✅ Created frontend/.env${NC}"
fi

# Create FakeS3 directory
mkdir -p ~/.filemanager/s3
echo -e "${GREEN}✅ Created FakeS3 directory: ~/.filemanager/s3${NC}"

echo -e "\n${GREEN}✨ Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Update backend/src/main/resources/application.yml with your database credentials"
echo "2. Set JWT_SECRET_KEY environment variable for production"
echo ""
echo "Then EITHER run the whole stack in Docker:"
echo "  docker-compose up -d"
echo ""
echo "OR run services manually against just the dockerized database (do not run both"
echo "at once — the manual backend and the docker-compose backend both bind port 8080):"
echo "  3. Start database only: docker-compose up -d db"
echo "  4. Start backend: cd backend && mvn spring-boot:run"
echo "  5. Start frontend: cd frontend && npm run dev"
