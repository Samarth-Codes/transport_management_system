# Manual Testing Guide

## Step-by-Step Testing Instructions

### Prerequisites
- Application running on http://localhost:8080
- Swagger UI accessible at http://localhost:8080/swagger-ui/index.html

---

## Test Flow 1: Complete Booking Process

### Step 1: Create a Transporter

1. In Swagger UI, find **"Transporter Management"** section
2. Expand **POST `/api/transporters`**
3. Click **"Try it out"** button
4. Replace the example JSON with:
```json
{
  "name": "ABC Transport Co.",
  "availableTrucks": 10,
  "rating": 4.5,
  "truckType": "LARGE"
}
```
5. Click **"Execute"**
6. **Expected Result**: Status 201 Created
7. **Note the `id`** from response (e.g., `"id": 1`) - you'll need this!

**Response Example:**
```json
{
  "id": 1,
  "name": "ABC Transport Co.",
  "availableTrucks": 10,
  "rating": 4.5,
  "truckType": "LARGE"
}
```

---

### Step 2: Create a Load

1. Find **"Load Management"** section
2. Expand **POST `/api/loads`**
3. Click **"Try it out"**
4. Replace JSON with:
```json
{
  "origin": "Mumbai",
  "destination": "Delhi",
  "totalTrucks": 5
}
```
5. Click **"Execute"**
6. **Expected Result**: Status 201 Created
7. **Note the `id`** (e.g., `"id": 1`)

**Response Example:**
```json
{
  "id": 1,
  "origin": "Mumbai",
  "destination": "Delhi",
  "totalTrucks": 5,
  "remainingTrucks": 5,
  "status": "ACTIVE",
  "createdAt": "2025-12-04T22:21:39.8429048"
}
```

---

### Step 3: Create a Bid

1. Find **"Bid Management"** section
2. Expand **POST `/api/bids`**
3. Click **"Try it out"**
4. In **Parameters**, set `transporterId` to `1` (use the transporter ID from Step 1)
5. Replace Request body with:
```json
{
  "loadId": 1,
  "rate": 5000.0,
  "trucksOffered": 3
}
```
6. Click **"Execute"**
7. **Expected Result**: Status 201 Created
8. **Note the `id`** (e.g., `"id": 1`)

**Response Example:**
```json
{
  "id": 1,
  "rate": 5000.0,
  "trucksOffered": 3,
  "loadId": 1,
  "transporterId": 1,
  "transporterName": "ABC Transport Co.",
  "transporterRating": 4.5,
  "createdAt": "2025-12-04T22:21:40.1386837"
}
```

---

### Step 4: Get Bids by Transporter (NEW ENDPOINT)

1. In **"Bid Management"** section
2. Expand **GET `/api/bids/transporter/{transporterId}`**
3. Click **"Try it out"**
4. Set `transporterId` to `1`
5. Click **"Execute"**
6. **Expected Result**: Status 200 OK
7. You should see the bid you created in Step 3

---

### Step 5: Get Best Bids for a Load

1. In **"Bid Management"** section
2. Expand **GET `/api/bids/load/{loadId}/best`**
3. Click **"Try it out"**
4. Set `loadId` to `1`
5. Click **"Execute"**
6. **Expected Result**: Status 200 OK
7. Bids are sorted by score (lower = better)

**Response Example:**
```json
[
  {
    "id": 1,
    "rate": 5000.0,
    "trucksOffered": 3,
    "transporterId": 1,
    "transporterName": "ABC Transport Co.",
    "transporterRating": 4.5,
    "score": 5050.0,
    "createdAt": "2025-12-04T22:21:40.138684"
  }
]
```

---

### Step 6: Accept a Bid (Create Booking)

1. Find **"Booking Management"** section
2. Expand **POST `/api/bookings/accept/{bidId}`**
3. Click **"Try it out"**
4. Set `bidId` to `1` (use bid ID from Step 3)
5. Click **"Execute"**
6. **Expected Result**: Status 201 Created

**Response Example:**
```json
{
  "id": 1,
  "trucksBooked": 3,
  "totalAmount": 15000.0,
  "loadId": 1,
  "bidId": 1,
  "transporterId": 1,
  "transporterName": "ABC Transport Co.",
  "createdAt": "2025-12-04T22:21:40.5003262"
}
```

---

### Step 7: Verify Load Status Updated

1. In **"Load Management"** section
2. Expand **GET `/api/loads/{id}`**
3. Click **"Try it out"**
4. Set `id` to `1`
5. Click **"Execute"**
6. **Check**: `remainingTrucks` should be `2` (was 5, booked 3)
7. **Check**: `status` should still be `"ACTIVE"` (2 trucks remaining)

---

### Step 8: Create Another Bid and Accept (Test Load Status Change)

1. Create another transporter (Step 1) - ID will be 2
2. Create a bid for load 1 with 2 trucks (Step 3)
3. Accept this bid (Step 6)
4. Check load status again (Step 7)
5. **Expected**: `remainingTrucks` = `0`, `status` = `"BOOKED"`

---

## Test Flow 2: Error Cases

### Test 1: Insufficient Trucks

1. Create transporter with `availableTrucks: 2`
2. Try to create bid with `trucksOffered: 5`
3. **Expected**: Error 400 - "Transporter only has 2 available trucks"

### Test 2: Bid on Booked Load

1. Create a load and accept all bids (remainingTrucks = 0)
2. Try to create a new bid on this load
3. **Expected**: Error 400 - "Cannot bid on a load with status: BOOKED"

### Test 3: Accept Bid When Load Fully Booked

1. Create a bid on a load
2. Accept enough bids to fill the load (remainingTrucks = 0)
3. Try to accept the remaining bid
4. **Expected**: Error 400 - "Load is already fully booked"

---

## Test Flow 3: Get All Endpoints

### Get All Transporters
- **GET `/api/transporters`**
- Should return list of all transporters

### Get All Loads
- **GET `/api/loads`**
- Should return list of all loads

### Get All Bids for a Load
- **GET `/api/bids/load/{loadId}`**
- Should return all bids for that load

### Get All Bookings for a Load
- **GET `/api/bookings/load/{loadId}`**
- Should return all bookings for that load

### Get All Bookings for a Transporter
- **GET `/api/bookings/transporter/{transporterId}`**
- Should return all bookings for that transporter

---

## Quick Verification Checklist

- ✅ Create Transporter
- ✅ Create Load
- ✅ Create Bid
- ✅ Get Bids by Transporter (NEW)
- ✅ Get Best Bids
- ✅ Accept Bid
- ✅ Verify trucks reduced
- ✅ Verify load status changes to BOOKED when full
- ✅ Test error cases

---

## Tips for Testing

1. **Keep track of IDs**: Write down transporter IDs, load IDs, and bid IDs as you create them
2. **Check responses**: Always verify the response matches what you expect
3. **Test edge cases**: Try invalid data, negative numbers, etc.
4. **Use Swagger's "Try it out"**: It's the easiest way to test without writing code
5. **Check database**: You can also verify data in PostgreSQL if needed

---

## Alternative: Browser Testing

For simple GET requests, you can also use your browser:

- http://localhost:8080/api/loads
- http://localhost:8080/api/transporters
- http://localhost:8080/api/bids/load/1
- http://localhost:8080/api/bids/transporter/1

Browser will show JSON response directly!

