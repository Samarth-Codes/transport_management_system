# PowerShell script to test Transport Management System API
# Make sure the application is running on http://localhost:8080

$baseUrl = "http://localhost:8080/api"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Testing Transport Management System API" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Create a Transporter
Write-Host "1. Creating a Transporter..." -ForegroundColor Yellow
$transporterBody = @{
    name = "Fast Trucks Co."
    availableTrucks = 10
    rating = 4.5
    truckType = "LARGE"
} | ConvertTo-Json

$transporterResponse = Invoke-RestMethod -Uri "$baseUrl/transporters" -Method Post -Body $transporterBody -ContentType "application/json"
Write-Host "Transporter Created:" -ForegroundColor Green
$transporterResponse | ConvertTo-Json
$transporterId = $transporterResponse.id
Write-Host "Transporter ID: $transporterId" -ForegroundColor Green
Write-Host ""

# 2. Create a Load
Write-Host "2. Creating a Load..." -ForegroundColor Yellow
$loadBody = @{
    origin = "Mumbai"
    destination = "Delhi"
    totalTrucks = 5
} | ConvertTo-Json

$loadResponse = Invoke-RestMethod -Uri "$baseUrl/loads" -Method Post -Body $loadBody -ContentType "application/json"
Write-Host "Load Created:" -ForegroundColor Green
$loadResponse | ConvertTo-Json
$loadId = $loadResponse.id
Write-Host "Load ID: $loadId" -ForegroundColor Green
Write-Host ""

# 3. Create a Bid
Write-Host "3. Creating a Bid..." -ForegroundColor Yellow
$bidBody = @{
    loadId = $loadId
    rate = 5000.0
    trucksOffered = 3
} | ConvertTo-Json

$bidResponse = Invoke-RestMethod -Uri "$baseUrl/bids?transporterId=$transporterId" -Method Post -Body $bidBody -ContentType "application/json"
Write-Host "Bid Created:" -ForegroundColor Green
$bidResponse | ConvertTo-Json
$bidId = $bidResponse.id
Write-Host "Bid ID: $bidId" -ForegroundColor Green
Write-Host ""

# 4. Get bids by transporter
Write-Host "4. Getting bids by transporter ID $transporterId..." -ForegroundColor Yellow
$transporterBids = Invoke-RestMethod -Uri "$baseUrl/bids/transporter/$transporterId" -Method Get
Write-Host "Bids by Transporter:" -ForegroundColor Green
$transporterBids | ConvertTo-Json
Write-Host ""

# 5. Get best bids for load
Write-Host "5. Getting best bids for load ID $loadId..." -ForegroundColor Yellow
$bestBids = Invoke-RestMethod -Uri "$baseUrl/bids/load/$loadId/best" -Method Get
Write-Host "Best Bids:" -ForegroundColor Green
$bestBids | ConvertTo-Json
Write-Host ""

# 6. Accept bid (create booking)
Write-Host "6. Accepting bid ID $bidId (creating booking)..." -ForegroundColor Yellow
$bookingResponse = Invoke-RestMethod -Uri "$baseUrl/bookings/accept/$bidId" -Method Post
Write-Host "Booking Created:" -ForegroundColor Green
$bookingResponse | ConvertTo-Json
Write-Host ""

# 7. Get all loads
Write-Host "7. Getting all loads..." -ForegroundColor Yellow
$loads = Invoke-RestMethod -Uri "$baseUrl/loads" -Method Get
Write-Host "All Loads:" -ForegroundColor Green
$loads | ConvertTo-Json
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

