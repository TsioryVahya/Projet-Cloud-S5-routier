# Script de test des notifications FCM
# Ce script permet de tester manuellement l'envoi de notifications

Write-Host "🧪 Test des notifications FCM" -ForegroundColor Cyan
Write-Host ""

# Configuration
$projectId = "projet-cloud-s5-routier"
$firestoreUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

Write-Host "📋 Options de test disponibles:" -ForegroundColor Yellow
Write-Host "1. Créer une notification de test dans Firestore"
Write-Host "2. Lister les notifications d'un utilisateur"
Write-Host "3. Vérifier la configuration FCM"
Write-Host ""

$choice = Read-Host "Choisissez une option (1-3)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "📝 Création d'une notification de test..." -ForegroundColor Cyan
        
        $userId = Read-Host "ID utilisateur Firebase (UID)"
        $signalementId = Read-Host "ID du signalement"
        $status = Read-Host "Nouveau statut (ex: en cours, validé, résolu)"
        
        Write-Host ""
        Write-Host "✅ Pour créer cette notification, utilisez la console Firebase ou le backend." -ForegroundColor Green
        Write-Host ""
        Write-Host "Exemple de document à créer dans la collection 'notifications':" -ForegroundColor Yellow
        Write-Host @"
{
  "userId": "$userId",
  "signalementId": "$signalementId",
  "titre": "Changement de statut",
  "message": "Votre signalement est maintenant '$status'",
  "type": "status_change",
  "oldStatus": "nouveau",
  "newStatus": "$status",
  "dateCreation": [Timestamp actuel],
  "lu": false
}
"@
    }
    
    "2" {
        Write-Host ""
        Write-Host "📋 Pour lister les notifications d'un utilisateur:" -ForegroundColor Yellow
        Write-Host "1. Ouvrez la console Firebase"
        Write-Host "2. Allez dans Firestore Database"
        Write-Host "3. Ouvrez la collection 'notifications'"
        Write-Host "4. Filtrez par userId"
    }
    
    "3" {
        Write-Host ""
        Write-Host "🔍 Vérification de la configuration FCM..." -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "✓ Vérifications à effectuer:" -ForegroundColor Green
        Write-Host ""
        Write-Host "1. Backend (Java):" -ForegroundColor Yellow
        Write-Host "   - FcmNotificationService.java existe"
        Write-Host "   - SignalementService.java intègre les notifications"
        Write-Host "   - serviceAccountKey.json est présent"
        Write-Host ""
        
        Write-Host "2. Frontend (Mobile):" -ForegroundColor Yellow
        Write-Host "   - notificationService.ts existe"
        Write-Host "   - NotificationsView.vue existe"
        Write-Host "   - Route /tabs/notifications ajoutée"
        Write-Host "   - Badge de notification dans TabsPage.vue"
        Write-Host ""
        
        Write-Host "3. Firebase Console:" -ForegroundColor Yellow
        Write-Host "   - Clé VAPID générée dans Cloud Messaging"
        Write-Host "   - Règles Firestore configurées pour 'notifications'"
        Write-Host "   - Collection 'users' avec champ 'fcmToken'"
        Write-Host ""
        
        Write-Host "4. Tests:" -ForegroundColor Yellow
        Write-Host "   - Permissions de notification accordées sur le device"
        Write-Host "   - FCM token sauvegardé dans Firestore/users"
        Write-Host "   - Service worker enregistré (firebase-messaging-sw.js)"
        Write-Host ""
    }
    
    default {
        Write-Host "❌ Option invalide" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "📚 Documentation complète: mobile-app/FCM_NOTIFICATIONS.md" -ForegroundColor Cyan
Write-Host ""
