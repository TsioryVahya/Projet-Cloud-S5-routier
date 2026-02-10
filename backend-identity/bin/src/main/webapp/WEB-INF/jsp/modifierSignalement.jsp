<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Modifier le Signalement</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <style>
        body { padding: 20px; background-color: #f8f9fa; }
        .form-container { max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
    </style>
</head>
<body>

<div class="container">
    <div class="form-container">
        <h2 class="mb-4 text-center">Modifier le Signalement</h2>
        
        <form id="modifierForm">
            <input type="hidden" id="signalementId" value="${signalementId}">

            <div class="mb-3">
                <label class="form-label">ID Firebase</label>
                <input type="text" class="form-control" id="idFirebase" readonly>
            </div>

            <div class="mb-3">
                <label class="form-label">Email Utilisateur</label>
                <input type="text" class="form-control" id="userEmail" readonly>
            </div>

            <div class="row">
                <div class="col-md-6 mb-3">
                    <label for="latitude" class="form-label">Latitude</label>
                    <input type="number" step="any" class="form-control" id="latitude" name="latitude" required>
                </div>
                <div class="col-md-6 mb-3">
                    <label for="longitude" class="form-label">Longitude</label>
                    <input type="number" step="any" class="form-control" id="longitude" name="longitude" required>
                </div>
            </div>

            <div class="mb-3">
                <label for="statutId" class="form-label">Statut</label>
                <select class="form-select" id="statutId" name="statutId" required>
                    <!-- Rempli par JS -->
                </select>
            </div>

            <hr>
            <h5 class="mb-3">Détails du Signalement</h5>

            <div class="mb-3">
                <label for="description" class="form-label">Description</label>
                <textarea class="form-control" id="description" name="description" rows="3" required></textarea>
            </div>

            <div class="row">
                <div class="col-md-6 mb-3">
                    <label for="surfaceM2" class="form-label">Surface (m²)</label>
                    <input type="number" step="any" class="form-control" id="surfaceM2" name="surfaceM2">
                </div>
                <div class="col-md-6 mb-3">
                    <label for="budget" class="form-label">Budget estimé</label>
                    <input type="number" step="0.01" class="form-control" id="budget" name="budget">
                </div>
            </div>

            <div class="mb-3">
                <label for="entrepriseConcerne" class="form-label">Entreprise concernée</label>
                <input type="text" class="form-control" id="entrepriseConcerne" name="entrepriseConcerne">
            </div>

            <div class="mb-3">
                <label for="photoUrl" class="form-label">URL de la photo</label>
                <input type="text" class="form-control" id="photoUrl" name="photoUrl">
            </div>

            <div class="d-grid gap-2 mt-4">
                <button type="submit" class="btn btn-primary">Enregistrer les modifications</button>
                <a href="/signalements" class="btn btn-outline-secondary">Annuler</a>
            </div>
        </form>
        <div id="message" class="mt-3"></div>
    </div>
</div>

<script>
    const signalementId = document.getElementById('signalementId').value;

    async function loadData() {
        try {
            // Charger les statuts
            const statutsResponse = await fetch('/api/statuts-signalement');
            const statuts = await statutsResponse.json();
            const statutSelect = document.getElementById('statutId');
            statuts.forEach(s => {
                const option = new Option(s.nom, s.id);
                statutSelect.add(option);
            });

            // Charger les données du signalement
            const response = await fetch(`/api/signalements/\${signalementId}`);
            const s = await response.json();

            document.getElementById('idFirebase').value = s.idFirebase || 'N/A';
            document.getElementById('userEmail').value = s.utilisateur ? s.utilisateur.email : 'N/A';
            document.getElementById('latitude').value = s.latitude;
            document.getElementById('longitude').value = s.longitude;
            document.getElementById('statutId').value = s.statut ? s.statut.id : '';
            
            // Les détails sont inclus dans l'objet Signalement si le service est bien fait
            // Sinon il faudra peut-être une autre API
            if (s.details) {
                document.getElementById('description').value = s.details.description || '';
                document.getElementById('surfaceM2').value = s.details.surfaceM2 || '';
                document.getElementById('budget').value = s.details.budget || '';
                document.getElementById('entrepriseConcerne').value = s.details.entrepriseConcerne || '';
                document.getElementById('photoUrl').value = s.details.photoUrl || '';
            }
        } catch (error) {
            console.error('Erreur lors du chargement des données:', error);
        }
    }

    document.getElementById('modifierForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        // Conversion des types
        data.latitude = parseFloat(data.latitude);
        data.longitude = parseFloat(data.longitude);
        data.statutId = parseInt(data.statutId);
        if (data.surfaceM2) data.surfaceM2 = parseFloat(data.surfaceM2);
        if (data.budget) data.budget = parseFloat(data.budget);

        const messageDiv = document.getElementById('message');

        try {
            const response = await fetch(`/api/signalements/\${signalementId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                messageDiv.innerHTML = '<div class="alert alert-success">Modifications enregistrées ! Redirection...</div>';
                setTimeout(() => window.location.href = '/signalements', 1500);
            } else {
                const error = await response.text();
                messageDiv.innerHTML = `<div class="alert alert-danger">Erreur : \${error}</div>`;
            }
        } catch (error) {
            messageDiv.innerHTML = `<div class="alert alert-danger">Erreur réseau : \${error.message}</div>`;
        }
    });

    document.addEventListener('DOMContentLoaded', loadData);
</script>

</body>
</html>
