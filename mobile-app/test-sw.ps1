# Test rapide Service Worker

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Service Worker Firebase" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier que le fichier existe
$swFile = ".\public\firebase-messaging-sw.js"
if (Test-Path $swFile) {
    Write-Host "✅ Service Worker trouvé: $swFile" -ForegroundColor Green
} else {
    Write-Host "❌ ERREUR: Service Worker introuvable!" -ForegroundColor Red
    Write-Host "   Le fichier doit être dans:" -ForegroundColor Yellow
    Write-Host "   mobile-app\public\firebase-messaging-sw.js" -ForegroundColor Gray
    exit 1
}

Write-Host ""
Write-Host "🔧 Instructions de Test:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Démarrer le serveur (si pas déjà fait):" -ForegroundColor White
Write-Host "   npm run dev" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Ouvrir http://localhost:5173 dans Chrome" -ForegroundColor White
Write-Host ""
Write-Host "3. Ouvrir la Console (F12)" -ForegroundColor White
Write-Host ""
Write-Host "4. IMPORTANT: Recharger la page (F5) 2 fois" -ForegroundColor Red
Write-Host "   - 1ère fois: Enregistre le SW" -ForegroundColor Gray
Write-Host "   - 2ème fois: Le SW prend le contrôle" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Se connecter et vérifier les logs" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Vérifications dans la Console" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Copier/coller dans la console du navigateur:" -ForegroundColor Yellow
Write-Host ""

$jsCode = @"
// Vérification rapide
console.log('SW supporté:', 'serviceWorker' in navigator);
console.log('SW controller:', navigator.serviceWorker.controller);
navigator.serviceWorker.getRegistrations().then(regs => {
  console.log('Nombre de SW:', regs.length);
  regs.forEach(r => console.log('  -', r.scope, r.active?.state));
});
"@

Write-Host $jsCode -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Résultats Attendus" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Après le 1er rechargement (F5):" -ForegroundColor Yellow
Write-Host "  [main.ts] Service Worker Firebase enregistré" -ForegroundColor Gray
Write-Host "  [main.ts] ⚠️ Service Worker n'a pas encore le contrôle" -ForegroundColor Gray
Write-Host ""

Write-Host "Après le 2ème rechargement (F5):" -ForegroundColor Yellow
Write-Host "  [main.ts] ✅ Service Worker contrôle la page" -ForegroundColor Gray
Write-Host "  [config.ts] Service Worker en contrôle, initialisation du messaging" -ForegroundColor Gray
Write-Host ""

Write-Host "Lors de la connexion:" -ForegroundColor Yellow
Write-Host "  [config.ts] Firebase Messaging initialisé avec succès" -ForegroundColor Gray
Write-Host "  Début requestFcmToken..." -ForegroundColor Gray
Write-Host "  Permission notification: granted" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Si Problème Persiste" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Voir la documentation complète:" -ForegroundColor White
Write-Host "  .\SW_NOT_SUPPORTED_FIX.md" -ForegroundColor Yellow
Write-Host ""

Write-Host "Ou exécuter le diagnostic complet:" -ForegroundColor White
Write-Host "  .\diagnose-fcm.ps1" -ForegroundColor Yellow
Write-Host ""

# Proposer de démarrer le serveur
Write-Host "Voulez-vous démarrer le serveur maintenant? (O/N): " -ForegroundColor Cyan -NoNewline
$response = Read-Host

if ($response -eq 'O' -or $response -eq 'o') {
    Write-Host ""
    Write-Host "🚀 Démarrage du serveur de développement..." -ForegroundColor Green
    Write-Host ""
    Write-Host "N'oubliez pas:" -ForegroundColor Yellow
    Write-Host "  1. Ouvrir http://localhost:5173" -ForegroundColor Gray
    Write-Host "  2. Recharger (F5) 2 fois" -ForegroundColor Gray
    Write-Host "  3. Se connecter" -ForegroundColor Gray
    Write-Host ""
    npm run dev
}
