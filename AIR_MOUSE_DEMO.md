# 🖱️ AIR MOUSE - Démonstration Live

## Quick Start

### Accès Direct
```
http://localhost:8000/air-mouse
```

---

## 🎯 Scénario de Présentation (5 minutes)

### Phase 1: Introduction (30 sec)
```
"Nous avons ajouté une fonctionnalité expérimentale appelée Air Mouse 
qui utilise votre webcam et l'intelligence artificielle (MediaPipe) 
pour transformer votre main en contrôleur de souris virtuel."
```

### Phase 2: Démonstration (3 min)
```
1. Accéder à /air-mouse [30 sec]
   - Montrer l'interface
   - Expliquer le panel de contrôle

2. Cliquer ON [30 sec]
   - Demander la permission caméra
   - Montrer le flux vidéo en direct
   - Expliquer la visualisation du squelette de la main

3. Démonstration du suivi (60 sec)
   - Lever la main
   - Pointer avec l'index
   - Montrer que le curseur vert suit le doigt
   - Faire des mouvements
   - Montrer les statistiques en temps réel (FPS, Position)

4. Test des clics (60 sec)
   - PINCER le pouce et l'index ensemble
   - Cliquer sur les boutons du navigateur
   - Cliquer sur les liens
   - Montrer l'effet ripple au point de clic

5. Arrêt (30 sec)
   - Cliquer OFF
   - Expliquer: caméra arrête, curseur disparaît
   - Vérifier que les ressources sont libérées (pas de LED caméra)
```

### Phase 3: Conclusion (1 min 30 sec)
```
"Points clés:
- Complètement frontend (pas de base de données)
- Utilise MediaPipe via CDN (pas d'installation)
- Gestion propre des ressources (caméra libérée)
- Cas d'usage réel: accessibilité, contrôle sans mains"
```

---

## 🎮 Guide Utilisateur

### Étape 1: ALLUMER (ON)
```
Click sur le bouton bascule ON/OFF
→ Vous verrez un message de permission caméra
→ Cliquer "Autoriser"
→ Vous verrez le flux vidéo en direct
```

### Étape 2: POINTER
```
Levez votre main face à la caméra
Pointez avec votre INDEX (doigt pointeur)
→ Le curseur vert suit votre doigt en temps réel
```

### Étape 3: CLIQUER
```
Pour cliquer:
1. Pointer avec l'index
2. Lever votre POUCE
3. Rapprocher pouce et index (PINCER)
4. Quand la distance est < 5%, clic automatique!

Vous verrez:
- Une animation "ripple" circulaire
- Le curseur fait un effet cliquable
- L'élément sous le curseur reçoit le clic
```

### Étape 4: ÉTEINDRE (OFF)
```
Click sur le bouton OFF
→ Caméra arrête immédiatement
→ Curseur disparaît
→ LED caméra s'éteint (ressources libérées)
```

---

## 📊 Statistiques Affichées

### En temps réel, vous verrez:

| Métrique | Signification |
|----------|---------------|
| **Status** | Inactif / Actif / Erreur caméra |
| **Position X** | Coordonnée horizontale du curseur (px) |
| **Position Y** | Coordonnée verticale du curseur (px) |
| **Distance** | % distance pouce-index (0-100%) |
| **FPS** | Frames par seconde (30-60 idéal) |
| **Main détectée** | Oui ✋ / Non |

### Indicateur lumineux
- 🔴 Gris = Inactif
- 🟢 Vert = Actif + pulsant

---

## 🔧 Détails Techniques (pour questions)

### Technologies Utilisées
```javascript
✅ MediaPipe Hands (via CDN)
✅ WebRTC getUserMedia API
✅ Canvas pour visualisation
✅ JavaScript Vanille (pas de dépendances)
```

### Comment ça marche?
```
1. MediaPipe détecte 21 points sur votre main
2. L'index (point #8) contrôle la position du curseur
3. Le pouce (point #4) est utilisé pour détecter un pincement
4. Distance pouce-index < 5% = clic
5. Lissage appliqué (smoothingFactor: 0.3) pour réduire sautillements
```

### Optimisations
```javascript
✅ Single Hand: Optimisé pour 1 main
✅ Model: modelComplexity: 1 (rapide)
✅ Smoothing: Réduit les sautillements
✅ Debounce: 200ms entre clics
✅ FPS Counter: Affichage en temps réel
```

---

## 🎨 Interface UI/UX

### Télécommande (Panel)
- Bouton ON/OFF avec animation
- Indicateur visuel d'état
- Statistiques en temps réel
- Police monospace pour le style tech

### Curseur Personnalisé
```css
- Anneau vert avec effet glow
- Point central luminescent
- Animation pulsante (1.5s)
- 40px de diamètre
- Smooth trailing
```

### Effet Ripple (Clic)
```css
- Cercle se dilate au point de clic
- Gradient radial (opaque → transparent)
- Durée: 600ms
- Auto-removed après animation
```

### Visualisation Main
```javascript
- Squelette vert (connexions entre joints)
- Points rouges = Pouce + Index (importants)
- Points verts = Autres joints
- Mise à jour 30-60 FPS
```

---

## ⚡ Performance Notes

### Caméra
- Résolution: 1280x720 (idéal pour MediaPipe)
- FPS attendus: 30-60
- Latence: < 100ms

### CPU Usage
- Leger: MediaPipe est optimisé (utilise Web Workers)
- Browser: Chrome/Firefox/Edge supportés
- Mobile: Fonctionne même sur téléphone (avec caméra)

### Qualité de Détection
```
Optimale: 
✅ Bonne luminosité
✅ Main complète visible
✅ Distance 30-100cm de caméra
✅ Sans obstacles

Problématique:
⚠️ Sombre = détection mauvaise
⚠️ Main partiellement visible
⚠️ Gants/masques
⚠️ Mouvements très rapides
```

---

## 🐛 Troubleshooting Rapide

### Caméra ne s'active pas
```
→ Vérifier permissions navigateur
→ Chercher l'icône caméra dans la barre URL
→ Cliquer "Autoriser"
→ Recharger page si besoin
```

### Curseur saute ou sautille
```
→ Améliorer la luminosité (lampe, fenêtre)
→ Rapprocher la main de la caméra
→ Bouger plus lentement
→ Nettoyer la caméra
```

### Clics ne fonctionnent pas
```
→ Pincer plus serré (distance < 5%)
→ Vérifier la distance affichée en live
→ S'assurer que la main est détectée (✋ Oui)
→ Attendre 200ms entre clics
```

### FPS bas (< 20)
```
→ Fermer autres onglets/apps
→ Réduire autres vidéos en cours
→ Vérifier luminosité de l'écran
→ Essayer un autre navigateur
```

---

## 🚀 Points Clés à Souligner

### Innovation ✨
- Accessibilité: Personnes handicapées peuvent contrôler sans mains
- Modern: Utilise technologie AI (MediaPipe) du moment
- Frontend: Zéro base de données, pure JavaScript

### Robustesse 🛡️
- Gestion propre ressources: Caméra libérée complètement
- Pas de crash: Try-catch et error handling
- Sécurité: Aucune donnée n'est envoyée au serveur
- Confidentialité: Tout local, pas de cloud

### UX/UI 🎨
- Interface moderne avec thème sombre
- Feedback visuel: cursor glow + ripple effect
- Statistiques en temps réel
- Dark mode optimisé pour soutenance

---

## 📁 Architecture Fichiers

```
src/Controller/FrontController.php
├── Route: /air-mouse
├── Method: airMouse()
└── Template: front/air_mouse.html.twig

templates/front/air_mouse.html.twig
├── HTML structure
├── CSS complet (inline)
└── CDN scripts (MediaPipe)

public/js/airmouse.js
├── Class AirMouse
├── MediaPipe initialization
├── Hand tracking logic
├── Click detection
└── Resource management

documentation/
├── air-mouse-feature.md (technique)
└── AIR_MOUSE_DEMO.md (ce fichier)
```

---

## 🎓 Réponses aux Questions Potentielles

**Q: Pourquoi uniquement pour la soutenance?**
```
R: C'est une feature expérimentale impressive pour montrer 
   nos capacités techniques, pas un core feature du projet.
```

**Q: Fonctionne sur mobile?**
```
R: Oui, si le téléphone a une caméra et supporte getUserMedia.
   Testé sur: Chrome, Firefox, Safari (iOS 14+)
```

**Q: Et la latence?**
```
R: < 100ms généralement. Optimal pour démonstration.
   Si hébergé sur serveur lointain, peut être 200-300ms.
```

**Q: Peut-on l'utiliser sans internet?**
```
R: Non, MediaPipe est chargé via CDN. 
   Nécessite connexion, mais les modèles s'exécutent localement.
```

**Q: Est-ce que les données sont sauvegardées?**
```
R: Non! Aucune donnée n'est envoyée au serveur.
   Tout fonctionne en local sur la machine.
```

**Q: Pourquoi pas intégré dans le app principal?**
```
R: Pour garder le code propre et focalisé. 
   Air Mouse est un addon amusant, pas un core feature.
```

---

## ✅ Checklist Avant Soutenance

- [ ] Test de la caméra sur chaque navigateur (Chrome, Firefox)
- [ ] S'assurer que la luminosité est bonne lors de la démo
- [ ] Pratiquer le pincement (vitesse, timing)
- [ ] Préparer l'URL: `http://localhost:8000/air-mouse`
- [ ] Vérifier la route fonctionne: `symfony console route:list | grep air`
- [ ] Testp du OFF: caméra LED s'éteint bien
- [ ] Screenshot des stats affichées (pour slides si besoin)
- [ ] Prévoir un backup (slides statiques) au cas où caméra ne marche pas

---

## 🎬 Démo Alternative (si caméra ne marche pas)

```
1. Montrer les screenshots dans les slides
2. Montrer le code source
3. Montrer cette documentation
4. Rediriger vers les vraies features du projet
```

---

**Version**: 1.0  
**Date**: 27 Février 2026  
**Duration**: 5-7 minutes recommended  
**Difficulty**: Facile (UI intuitive)
