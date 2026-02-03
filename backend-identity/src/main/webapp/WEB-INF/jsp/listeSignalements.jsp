<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestion des Signalements | Admin</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Inter', sans-serif; }
    </style>
</head>
<body class="bg-slate-50 min-h-screen">

    <!-- Navigation -->
    <nav class="bg-white border-b border-slate-200 px-6 py-4">
        <div class="max-w-7xl mx-auto flex justify-between items-center">
            <div class="flex items-center gap-2">
                <div class="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-blue-200">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                    </svg>
                </div>
                <h1 class="text-xl font-bold text-slate-800 tracking-tight">Routier<span class="text-blue-600">Admin</span></h1>
            </div>
            <div class="flex gap-4">
                <button onclick="syncMobile()" class="flex items-center gap-2 px-4 py-2 bg-amber-50 text-amber-700 rounded-lg font-medium hover:bg-amber-100 transition-colors border border-amber-200">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Synchroniser Mobile
                </button>
                <a href="/signalements/nouveau" class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-shadow shadow-lg shadow-blue-200">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    Nouveau
                </a>
            </div>
        </div>
    </nav>

    <main class="max-w-7xl mx-auto px-6 py-8">
        <!-- Header Stats -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <p class="text-slate-500 text-sm font-medium mb-1">Total Signalements</p>
                <h3 id="statTotal" class="text-3xl font-bold text-slate-800">0</h3>
            </div>
            <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <p class="text-slate-500 text-sm font-medium mb-1">En attente</p>
                <h3 id="statNouveau" class="text-3xl font-bold text-blue-600">0</h3>
            </div>
            <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <p class="text-slate-500 text-sm font-medium mb-1">En cours</p>
                <h3 id="statEnCours" class="text-3xl font-bold text-amber-600">0</h3>
            </div>
        </div>

        <!-- Table Card -->
        <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="bg-slate-50 border-b border-slate-200">
                            <th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Date</th>
                            <th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Statut</th>
                            <th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Description</th>
                            <th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Budget / Surface</th>
                            <th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody id="signalementsTableBody" class="divide-y divide-slate-100">
                        <!-- Rempli par JS -->
                    </tbody>
                </table>
            </div>
        </div>
    </main>

    <!-- Photo Modal -->
    <div id="photoModal" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm hidden z-50 flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl max-w-2xl w-full overflow-hidden shadow-2xl">
            <div class="p-4 border-b border-slate-100 flex justify-between items-center">
                <h3 class="font-bold text-slate-800">Photo du signalement</h3>
                <button onclick="closeModal()" class="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>
            </div>
            <div class="p-4 bg-slate-50">
                <img id="modalImage" src="" alt="Signalement" class="w-full h-auto rounded-xl shadow-inner max-h-[70vh] object-contain">
            </div>
        </div>
    </div>

    <script>
        async function loadSignalements() {
            try {
                const response = await fetch('/api/signalements');
                const signalements = await response.json();
                const tableBody = document.getElementById('signalementsTableBody');
                tableBody.innerHTML = '';

                let total = signalements.length;
                let nouveau = 0;
                let enCours = 0;

                signalements.forEach(s => {
                    if (s.statut?.toLowerCase() === 'nouveau') nouveau++;
                    if (s.statut?.toLowerCase() === 'en cours' || s.statut?.toLowerCase() === 'en_cours') enCours++;

                    const date = s.dateSignalement ? new Date(s.dateSignalement) : null;
                    const dateStr = date ? date.toLocaleDateString('fr-FR') : 'N/A';
                    const timeStr = date ? date.toLocaleTimeString('fr-FR', {hour: '2-digit', minute:'2-digit'}) : '';
                    
                    const budget = s.budget ? new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MGA' }).format(s.budget) : '-';
                    const surface = s.surfaceM2 ? s.surfaceM2 + ' m²' : '-';

                    const row = `
                        <tr class="hover:bg-slate-50/80 transition-colors group">
                            <td class="px-6 py-4">
                                <div class="text-sm font-semibold text-slate-700">\${dateStr}</div>
                                <div class="text-xs text-slate-400 font-medium">\${timeStr}</div>
                            </td>
                            <td class="px-6 py-4">
                                <span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider \${getBadgeClass(s.statut)}">
                                    \${s.statut || 'Inconnu'}
                                </span>
                            </td>
                            <td class="px-6 py-4">
                                <div class="text-sm text-slate-600 max-w-xs truncate font-medium" title="\${s.description || '-'}">
                                    \${s.description || '<span class="text-slate-300 italic">Sans description</span>'}
                                </div>
                                <div class="text-[10px] text-slate-400 mt-0.5">\${s.utilisateur?.email || 'Anonyme'}</div>
                            </td>
                            <td class="px-6 py-4">
                                <div class="text-sm font-semibold text-slate-700">\${budget}</div>
                                <div class="text-xs text-slate-400 font-medium">\${surface}</div>
                            </td>
                            <td class="px-6 py-4 text-right">
                                <div class="flex justify-end gap-2">
                                    \${s.photoUrl ? `
                                        <button onclick="showPhoto('\${s.photoUrl}')" class="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors title="Voir Photo">
                                            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                            </svg>
                                        </button>
                                    ` : ''}
                                    <a href="/signalements/modifier/\${s.postgresId}" class="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors" title="Modifier">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                        </svg>
                                    </a>
                                    <button onclick="deleteSignalement('\${s.postgresId}')" class="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors" title="Supprimer">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                        </svg>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `;
                    tableBody.insertAdjacentHTML('beforeend', row);
                });

                document.getElementById('statTotal').innerText = total;
                document.getElementById('statNouveau').innerText = nouveau;
                document.getElementById('statEnCours').innerText = enCours;

            } catch (error) {
                console.error('Erreur:', error);
            }
        }

        function getBadgeClass(statut) {
            const s = (statut || '').toLowerCase();
            if (s === 'nouveau') return 'bg-blue-100 text-blue-700 border border-blue-200';
            if (s === 'en cours' || s === 'en_cours') return 'bg-amber-100 text-amber-700 border border-amber-200';
            if (s === 'terminé' || s === 'termine') return 'bg-emerald-100 text-emerald-700 border border-emerald-200';
            return 'bg-slate-100 text-slate-600 border border-slate-200';
        }

        function showPhoto(url) {
            document.getElementById('modalImage').src = url;
            document.getElementById('photoModal').classList.remove('hidden');
        }

        function closeModal() {
            document.getElementById('photoModal').classList.add('hidden');
        }

        async function syncMobile() {
            const btn = event.currentTarget;
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<svg class="animate-spin h-4 w-4 mr-2" viewBox="0 0 24 24">...</svg> Sync...';
            
            try {
                const response = await fetch('/api/signalements/sync', { method: 'POST' });
                const result = await response.json();
                alert(`Synchronisation terminée : \${result.signalements} nouveaux signalements.`);
                loadSignalements();
            } catch (error) {
                alert('Erreur lors de la synchronisation');
            } finally {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }

        async function deleteSignalement(id) {
            if (confirm('Supprimer ce signalement ?')) {
                try {
                    await fetch(`/api/signalements/\${id}`, { method: 'DELETE' });
                    loadSignalements();
                } catch (error) {
                    alert('Erreur lors de la suppression');
                }
            }
        }

        document.addEventListener('DOMContentLoaded', loadSignalements);
    </script>
</body>
</html>