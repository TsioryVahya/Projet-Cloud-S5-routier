# Test de création de signalement
$body = @{
    latitude = 48.8566
    longitude = 2.3522
    description = "Test synchronisation Firebase"
    email = "test@example.com"
    surfaceM2 = 100.5
    budget = 5000
    entrepriseNom = "Test Entreprise"
} | ConvertTo-Json

Write-Host "📤 Envoi de la requête..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/signalements" `
        -Method Post `
        -Body $body `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    Write-Host "✅ Signalement créé avec succès !" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "❌ Erreur lors de la création : $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
    }
}

Write-Host "`n📋 Vérification des logs Docker..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
docker compose logs --tail=30 backend | Select-String -Pattern "Création|Firestore|Synchronisation|ÉCHEC|✅|❌"
