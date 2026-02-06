# Script de Diagnostic FCM - Push Service Error

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Diagnostic FCM - Push Service Error" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📋 Vérification des fichiers de configuration..." -ForegroundColor Yellow
Write-Host ""

# 1. Vérifier config.ts
$configFile = ".\src\firebase\config.ts"
if (Test-Path $configFile) {
    $content = Get-Content $configFile -Raw
    
    Write-Host "✅ Configuration Firebase trouvée" -ForegroundColor Green
    
    # Extraire projectId
    if ($content -match 'projectId:\s*"([^"]+)"') {
        Write-Host "   Project ID: $($matches[1])" -ForegroundColor Gray
    }
    
    # Extraire messagingSenderId
    if ($content -match 'messagingSenderId:\s*"([^"]+)"') {
        Write-Host "   Messaging Sender ID: $($matches[1])" -ForegroundColor Gray
    }
    
    # Extraire appId
    if ($content -match 'appId:\s*"([^"]+)"') {
        Write-Host "   App ID: $($matches[1])" -ForegroundColor Gray
    }
} else {
    Write-Host "❌ Fichier de configuration introuvable" -ForegroundColor Red
}

Write-Host ""
Write-Host "🔑 Vérification de la clé VAPID..." -ForegroundColor Yellow
Write-Host ""

# 2. Chercher les clés VAPID dans tous les fichiers
$vapidFiles = @(
    ".\src\views\MapView.vue",
    ".\src\App.vue"
)

$vapidKeys = @{}

foreach ($file in $vapidFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        if ($content -match "vapidKey:\s*'([^']+)'") {
            $key = $matches[1]
            if (-not $vapidKeys.ContainsKey($key)) {
                $vapidKeys[$key] = @()
            }
            $vapidKeys[$key] += $file
        }
    }
}

if ($vapidKeys.Count -eq 0) {
    Write-Host "⚠️  Aucune clé VAPID trouvée" -ForegroundColor Yellow
} elseif ($vapidKeys.Count -eq 1) {
    $key = $vapidKeys.Keys[0]
    Write-Host "✅ Clé VAPID unique trouvée:" -ForegroundColor Green
    Write-Host "   $key" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   Utilisée dans:" -ForegroundColor Gray
    foreach ($file in $vapidKeys[$key]) {
        Write-Host "   - $file" -ForegroundColor DarkGray
    }
} else {
    Write-Host "⚠️  Plusieurs clés VAPID différentes trouvées!" -ForegroundColor Red
    foreach ($key in $vapidKeys.Keys) {
        Write-Host "   Clé: $($key.Substring(0, 20))..." -ForegroundColor Yellow
        foreach ($file in $vapidKeys[$key]) {
            Write-Host "   - $file" -ForegroundColor DarkGray
        }
    }
}

Write-Host ""
Write-Host "🔧 Service Worker..." -ForegroundColor Yellow
Write-Host ""

$swFile = ".\public\firebase-messaging-sw.js"
if (Test-Path $swFile) {
    Write-Host "✅ Service Worker trouvé: $swFile" -ForegroundColor Green
    
    $content = Get-Content $swFile -Raw
    
    # Vérifier la version Firebase
    if ($content -match 'firebasejs/(\d+\.\d+\.\d+)/') {
        Write-Host "   Version Firebase: $($matches[1])" -ForegroundColor Gray
    }
    
    # Vérifier la configuration
    if ($content -match 'projectId:\s*"([^"]+)"') {
        Write-Host "   Project ID SW: $($matches[1])" -ForegroundColor Gray
    }
} else {
    Write-Host "❌ Service Worker introuvable" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Diagnostic - Push Service Error" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Cette erreur peut avoir plusieurs causes:" -ForegroundColor White
Write-Host ""
Write-Host "1. 🔑 Clé VAPID incorrecte" -ForegroundColor Yellow
Write-Host "   → Vérifier sur Firebase Console:" -ForegroundColor Gray
Write-Host "     https://console.firebase.google.com" -ForegroundColor DarkGray
Write-Host "     Projet > Paramètres > Cloud Messaging > Certificats de clés Web push" -ForegroundColor DarkGray
Write-Host ""

Write-Host "2. 🚫 Cloud Messaging API non activée" -ForegroundColor Yellow
Write-Host "   → Activer sur Google Cloud Console:" -ForegroundColor Gray
Write-Host "     https://console.cloud.google.com" -ForegroundColor DarkGray
Write-Host "     APIs & Services > Library > Firebase Cloud Messaging API" -ForegroundColor DarkGray
Write-Host ""

Write-Host "3. 🌐 Problème du navigateur" -ForegroundColor Yellow
Write-Host "   → Essayer dans un autre navigateur (Chrome recommandé)" -ForegroundColor Gray
Write-Host "   → Vider le cache et désinscrire les Service Workers" -ForegroundColor Gray
Write-Host ""

Write-Host "4. ⚙️  Mode Dégradé" -ForegroundColor Green
Write-Host "   → L'application fonctionne SANS notifications push" -ForegroundColor Gray
Write-Host "   → Les utilisateurs peuvent:" -ForegroundColor Gray
Write-Host "     • Se connecter normalement" -ForegroundColor DarkGray
Write-Host "     • Créer des signalements" -ForegroundColor DarkGray
Write-Host "     • Voir la carte" -ForegroundColor DarkGray
Write-Host "     • Utiliser toutes les fonctionnalités" -ForegroundColor DarkGray
Write-Host "   → Seulement les notifications en temps réel sont désactivées" -ForegroundColor DarkGray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Actions Recommandées" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Vérifier la clé VAPID sur Firebase Console" -ForegroundColor White
Write-Host "2. Activer Cloud Messaging API sur Google Cloud Console" -ForegroundColor White
Write-Host "3. Tester dans Chrome en mode navigation privée" -ForegroundColor White
Write-Host "4. Si le problème persiste, utiliser l'application en mode dégradé" -ForegroundColor White
Write-Host ""

Write-Host "📖 Documentation complète:" -ForegroundColor Cyan
Write-Host "   .\PUSH_SERVICE_ERROR_FIX.md" -ForegroundColor Yellow
Write-Host ""

# Option pour ouvrir la documentation
Write-Host "Voulez-vous ouvrir la documentation complète? (O/N): " -ForegroundColor Cyan -NoNewline
$response = Read-Host

if ($response -eq 'O' -or $response -eq 'o') {
    if (Test-Path ".\PUSH_SERVICE_ERROR_FIX.md") {
        Start-Process ".\PUSH_SERVICE_ERROR_FIX.md"
    } else {
        Write-Host "Documentation introuvable" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Voulez-vous démarrer le serveur de développement? (O/N): " -ForegroundColor Cyan -NoNewline
$response = Read-Host

if ($response -eq 'O' -or $response -eq 'o') {
    Write-Host ""
    Write-Host "Démarrage du serveur..." -ForegroundColor Green
    npm run dev
}
