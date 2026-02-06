# Script de Test FCM - Vérification Rapide

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test FCM - Vérification Service Worker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Vérifier que le fichier Service Worker existe
$swPath = ".\public\firebase-messaging-sw.js"
if (Test-Path $swPath) {
    Write-Host "✅ Service Worker trouvé: $swPath" -ForegroundColor Green
} else {
    Write-Host "❌ Service Worker INTROUVABLE: $swPath" -ForegroundColor Red
    exit 1
}

# 2. Vérifier la configuration Firebase
$configPath = ".\src\firebase\config.ts"
if (Test-Path $configPath) {
    Write-Host "✅ Configuration Firebase trouvée: $configPath" -ForegroundColor Green
} else {
    Write-Host "❌ Configuration Firebase INTROUVABLE: $configPath" -ForegroundColor Red
    exit 1
}

# 3. Vérifier main.ts
$mainPath = ".\src\main.ts"
if (Test-Path $mainPath) {
    $content = Get-Content $mainPath -Raw
    if ($content -match "serviceWorker.register") {
        Write-Host "✅ Enregistrement du SW dans main.ts détecté" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Enregistrement du SW dans main.ts NON trouvé" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ main.ts INTROUVABLE: $mainPath" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instructions de Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Démarrer le serveur de développement:" -ForegroundColor White
Write-Host "   npm run dev" -ForegroundColor Yellow
Write-Host ""
Write-Host "2. Ouvrir le navigateur (Chrome recommandé):" -ForegroundColor White
Write-Host "   http://localhost:5173" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Ouvrir la Console (F12) et vérifier:" -ForegroundColor White
Write-Host "   - Logs du Service Worker" -ForegroundColor Gray
Write-Host "   - Onglet Application > Service Workers" -ForegroundColor Gray
Write-Host ""
Write-Host "4. IMPORTANT: Recharger la page (F5) après le premier chargement" -ForegroundColor Red
Write-Host "   (Nécessaire pour que le SW prenne le contrôle)" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Se connecter et vérifier le token FCM dans les logs" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Page de Test Disponible" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "http://localhost:5173/test-fcm.html" -ForegroundColor Yellow
Write-Host ""
Write-Host "Cette page permet de tester le Service Worker" -ForegroundColor Gray
Write-Host "et FCM indépendamment de l'application." -ForegroundColor Gray
Write-Host ""

# Option pour démarrer automatiquement
Write-Host "Voulez-vous démarrer le serveur maintenant? (O/N): " -ForegroundColor Cyan -NoNewline
$response = Read-Host

if ($response -eq 'O' -or $response -eq 'o') {
    Write-Host ""
    Write-Host "Démarrage du serveur de développement..." -ForegroundColor Green
    npm run dev
}
