#!/bin/bash

# Test script for Transport Management System API
# Make sure the application is running on http://localhost:8080

BASE_URL="http://localhost:8080/api"

echo "========================================="
echo "Testing Transport Management System API"
echo "========================================="
echo ""

# 1. Create a Transporter
echo "1. Creating a Transporter..."
TRANSPORTER_RESPONSE=$(curl -s -X POST "$BASE_URL/transporters" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fast Trucks Co.",
    "availableTrucks": 10,
    "rating": 4.5,
    "truckType": "LARGE"
  }')
echo "Response: $TRANSPORTER_RESPONSE"
TRANSPORTER_ID=$(echo $TRANSPORTER_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
echo "Transporter ID: $TRANSPORTER_ID"
echo ""

# 2. Create a Load
echo "2. Creating a Load..."
LOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/loads" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Mumbai",
    "destination": "Delhi",
    "totalTrucks": 5
  }')
echo "Response: $LOAD_RESPONSE"
LOAD_ID=$(echo $LOAD_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
echo "Load ID: $LOAD_ID"
echo ""

# 3. Create a Bid
echo "3. Creating a Bid..."
BID_RESPONSE=$(curl -s -X POST "$BASE_URL/bids?transporterId=$TRANSPORTER_ID" \
  -H "Content-Type: application/json" \
  -d "{
    \"loadId\": $LOAD_ID,
    \"rate\": 5000.0,
    \"trucksOffered\": 3
  }")
echo "Response: $BID_RESPONSE"
BID_ID=$(echo $BID_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
echo "Bid ID: $BID_ID"
echo ""

# 4. Get bids by transporter
echo "4. Getting bids by transporter ID $TRANSPORTER_ID..."
curl -s -X GET "$BASE_URL/bids/transporter/$TRANSPORTER_ID" | python -m json.tool
echo ""

# 5. Get best bids for load
echo "5. Getting best bids for load ID $LOAD_ID..."
curl -s -X GET "$BASE_URL/bids/load/$LOAD_ID/best" | python -m json.tool
echo ""

# 6. Accept bid (create booking)
echo "6. Accepting bid ID $BID_ID (creating booking)..."
BOOKING_RESPONSE=$(curl -s -X POST "$BASE_URL/bookings/accept/$BID_ID")
echo "Response: $BOOKING_RESPONSE"
echo ""

# 7. Get all loads
echo "7. Getting all loads..."
curl -s -X GET "$BASE_URL/loads" | python -m json.tool
echo ""

echo "========================================="
echo "Testing Complete!"
echo "========================================="

