# Script de Remplacement de la Clé VAPID

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Remplacement Clé VAPID Firebase" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Clé actuelle
$oldKey = "BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI"

Write-Host "🔍 Recherche de la clé actuelle..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Clé actuelle (première partie):" -ForegroundColor White
Write-Host "  $($oldKey.Substring(0, 20))..." -ForegroundColor Gray
Write-Host ""

# Rechercher dans les fichiers
$files = Get-ChildItem -Path ".\src" -Recurse -Include "*.vue", "*.ts", "*.js" | 
    Where-Object { (Get-Content $_.FullName -Raw) -match [regex]::Escape($oldKey) }

if ($files.Count -eq 0) {
    Write-Host "✅ Aucun fichier trouvé avec l'ancienne clé" -ForegroundColor Green
    Write-Host "   (Peut-être déjà mise à jour)" -ForegroundColor Gray
} else {
    Write-Host "📁 Fichiers contenant la clé VAPID:" -ForegroundColor Yellow
    Write-Host ""
    foreach ($file in $files) {
        $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
        Write-Host "  - $relativePath" -ForegroundColor White
        
        # Compter les occurrences
        $content = Get-Content $file.FullName -Raw
        $matches = [regex]::Matches($content, [regex]::Escape($oldKey))
        Write-Host "    ($($matches.Count) occurrence(s))" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Étapes pour Obtenir la Nouvelle Clé" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Ouvrir Firebase Console:" -ForegroundColor White
Write-Host "   https://console.firebase.google.com" -ForegroundColor Blue
Write-Host ""

Write-Host "2. Sélectionner le projet:" -ForegroundColor White
Write-Host "   projet-cloud-s5-routier" -ForegroundColor Yellow
Write-Host ""

Write-Host "3. Paramètres (⚙️) > Cloud Messaging" -ForegroundColor White
Write-Host ""

Write-Host "4. Section 'Certificats de clés Web push'" -ForegroundColor White
Write-Host "   - Si clé existe: COPIER la clé" -ForegroundColor Gray
Write-Host "   - Si pas de clé: Cliquer sur 'Generate key pair'" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Remplacement Automatique" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Avez-vous la nouvelle clé VAPID depuis Firebase Console? (O/N): " -ForegroundColor Yellow -NoNewline
$hasKey = Read-Host

if ($hasKey -eq 'O' -or $hasKey -eq 'o') {
    Write-Host ""
    Write-Host "Collez la nouvelle clé VAPID (puis Entrée):" -ForegroundColor Yellow
    $newKey = Read-Host
    
    if ($newKey -and $newKey.Length -gt 50) {
        Write-Host ""
        Write-Host "🔄 Remplacement en cours..." -ForegroundColor Cyan
        
        $replaced = 0
        foreach ($file in $files) {
            $content = Get-Content $file.FullName -Raw
            $newContent = $content -replace [regex]::Escape($oldKey), $newKey
            
            if ($content -ne $newContent) {
                Set-Content -Path $file.FullName -Value $newContent -NoNewline
                $replaced++
                $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
                Write-Host "  ✅ $relativePath" -ForegroundColor Green
            }
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "  Remplacement Terminé" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Fichiers modifiés: $replaced" -ForegroundColor Green
        Write-Host ""
        Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
        Write-Host "  1. Redémarrer le serveur (Ctrl+C puis npm run dev)" -ForegroundColor White
        Write-Host "  2. Nettoyer le navigateur (désinscrire les SW)" -ForegroundColor White
        Write-Host "  3. Recharger 2 fois (F5 x 2)" -ForegroundColor White
        Write-Host "  4. Se reconnecter" -ForegroundColor White
        Write-Host ""
        
    } else {
        Write-Host ""
        Write-Host "❌ Clé invalide (trop courte ou vide)" -ForegroundColor Red
        Write-Host "   La clé VAPID doit faire environ 88 caractères" -ForegroundColor Gray
    }
} else {
    Write-Host ""
    Write-Host "📖 Consultez le guide complet:" -ForegroundColor Cyan
    Write-Host "   .\VAPID_KEY_FIX.md" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Activer l'API Cloud Messaging" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "N'oubliez pas d'activer l'API sur:" -ForegroundColor White
Write-Host "  https://console.cloud.google.com" -ForegroundColor Blue
Write-Host ""
Write-Host "APIs & Services > Library > Firebase Cloud Messaging API" -ForegroundColor Gray
Write-Host ""
