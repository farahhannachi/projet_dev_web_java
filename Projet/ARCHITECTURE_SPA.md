🎉 ARCHITECTURE SPA COMPLÈTE - SINGLE PAGE APPLICATION JAVAFX

================================================================================
                          NOUVELLE ARCHITECTURE
================================================================================

✅ AVANT (Modal/Stage classique):
  - Cliquer "Ajouter" → Nouvelle fenêtre (Stage) s'ouvre
  - Modale indépendante du Dashboard
  - Navigation complexe avec gestion multiple de fenêtres
  
✅ APRÈS (SPA - Single Page Application):
  - Cliquer "Ajouter" → Formulaire s'affiche dans le même espace
  - Sidebar reste fixe et visible
  - Changement dynamique du content pane uniquement
  - Navigation fluide sans nouvelles fenêtres

================================================================================
                         COMPOSANTS CRÉÉS
================================================================================

1. NavigationService.java (NEW)
   - Service singleton de navigation interne
   - Gère les changements de vue dynamiquement
   - SANS création de nouvelles fenêtres

2. DepotFormController.java (NEW)
   - Contrôleur pour le formulaire d'ajout/modification
   - Affiche le formulaire dans le content pane
   - Référence au contrôleur parent (DepotController)

3. DepotForm.fxml (NEW)
   - Vue du formulaire (pas de modale, juste un VBox)
   - Affichée dynamiquement dans le StackPane
   - Remplace la vue "liste" quand on clique "Ajouter"

================================================================================
                         FICHIERS MODIFIÉS
================================================================================

1. Depots.fxml
   - Changé: VBox → StackPane (comme racine)
   - Raison: Permet le "view swapping" (changer de vue dynamiquement)
   - La liste reste visible dans le StackPane

2. DepotController.java
   - Ajout @FXML VBox listView
   - Implémentation de showForm() et showTableView()
   - Chargement dynamique du formulaire

3. Dashboard.fxml
   - Reste unchanged (sidebar fixe)
   - Le center pane change dynamiquement

================================================================================
                            FLUX DE NAVIGATION
================================================================================

Utilisateur clique "Ajouter un dépôt"
         ↓
DepotController.openAddDepotModal()
         ↓
Charge DepotForm.fxml dans un FXMLLoader
         ↓
Crée instance de DepotFormController
         ↓
DepotController.showForm(formView)
         ↓
Remplace le contenu du StackPane
         ↓
Formulaire s'affiche à la place du tableau
Sidebar reste fixe ✅
         ↓
Utilisateur remplit et valide
         ↓
DepotFormController.handleSave()
         ↓
Ajoute le dépôt à la base
         ↓
Appelle parentController.showTableView()
         ↓
Revient à la vue liste
Tableau se réaffiche ✅

================================================================================
                        AVANTAGES DE CETTE ARCHITECTURE
================================================================================

✅ PAS DE NOUVELLES FENÊTRES - Tout se passe dans le même Dashboard
✅ SIDEBAR FIXE - Navigation fluide, contexte préservé
✅ UX MODERNE - Comme une SPA web (React/Vue/Angular)
✅ MAINTENANCE FACILE - Logique de navigation centralisée
✅ ANIMATIONS POSSIBLES - Transition smooth entre vues
✅ RESPONSIVE - Adapte le layout sans rechargement
✅ STATE MANAGEMENT - Les données restent persistantes

================================================================================
                         STRUCTURE DU STACKPANE
================================================================================

StackPane (racine de Depots.fxml)
├─ Vue 1: VBox listView (liste des dépôts)
└─ Vue 2: Formulaire (chargé dynamiquement quand nécessaire)

Le StackPane affiche une seule vue à la fois:
- Au démarrage: affiche listView
- Quand clic "Ajouter": remplace par formulaire
- Quand clic "Annuler/Enregistrer": revient à listView

================================================================================
                              LANCER L'APP
================================================================================

mvn javafx:run

Puis testez:
1. Cliquer sur "Dépôts"
2. Voir la liste du tableau
3. Cliquer "Ajouter un dépôt"
4. Voir le formulaire s'afficher DANS LE MÊME ESPACE
5. Remplir et enregistrer
6. Revenir à la liste automatiquement
7. Sidebar reste toujours visible ✅

================================================================================
                         PROCHAINES AMÉLIORATIONS
================================================================================

Si vous voulez encore plus professionnel:

1. Ajouter des animations de transition
2. Implémenter un router complet
3. Ajouter un système de breadcrumbs
4. Implémenter un historique de navigation
5. Ajouter des transitions fluides (Fade/Slide)
6. Implémenter le undo/redo

================================================================================

✅ ARCHITECTURE SPA COMPLÈTEMENT IMPLÉMENTÉE
   Pas de nouvelles fenêtres, sidebar fixe, UX fluide!

