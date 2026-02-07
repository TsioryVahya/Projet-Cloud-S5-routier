# Script pour synchroniser les changements avec Android
Write-Host "🔄 Synchronisation du code avec Android..." -ForegroundColor Cyan

# Étape 1: Build du projet
Write-Host "`n📦 Build du projet Vue..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors du build" -ForegroundColor Red
    exit 1
}

# Étape 2: Sync avec Capacitor
Write-Host "`n🔄 Synchronisation avec Capacitor..." -ForegroundColor Yellow
npx cap sync android

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la synchronisation" -ForegroundColor Red
    exit 1
}

# Étape 3: Copy assets (optionnel mais recommandé)
Write-Host "`n📋 Copie des assets..." -ForegroundColor Yellow
npx cap copy android

Write-Host "`n✅ Synchronisation terminée!" -ForegroundColor Green
Write-Host "👉 Vous pouvez maintenant ouvrir le projet dans Android Studio" -ForegroundColor Cyan
Write-Host "   Commande: npx cap open android" -ForegroundColor Gray
