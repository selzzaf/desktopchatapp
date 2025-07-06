# CASES D'UTILISATION AVANCÉS - APPLICATION DE CHAT

## 1. FONCTIONNALITÉS AVANCÉES PROPOSÉES

### 1.1 UC009 - Création de groupes de discussion

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne "Contacts" → "Créer un groupe"
2. Une boîte de dialogue s'ouvre
3. L'utilisateur saisit :
   - Nom du groupe
   - Description du groupe
4. L'utilisateur sélectionne les membres à ajouter
5. L'utilisateur clique sur "Créer"
6. Le système crée le groupe dans Firebase
7. Le système notifie tous les membres invités
8. Le groupe apparaît dans la liste des conversations

**Scénarios alternatifs**:
- **9a**: Nom du groupe vide → Affichage d'un message d'erreur
- **9b**: Aucun membre sélectionné → Affichage d'un message d'erreur

**Postconditions**: Le groupe est créé et accessible à tous les membres

### 1.2 UC010 - Envoi de fichiers

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté et a sélectionné un contact/groupe

**Scénario principal**:
1. L'utilisateur clique sur l'icône "Pièce jointe"
2. Un sélecteur de fichiers s'ouvre
3. L'utilisateur sélectionne le fichier à envoyer
4. Le système valide le type et la taille du fichier
5. Le système upload le fichier vers Firebase Storage
6. Le système envoie le lien du fichier via WebSocket
7. Le destinataire reçoit la notification de fichier
8. Le destinataire peut télécharger le fichier

**Scénarios alternatifs**:
- **10a**: Fichier trop volumineux → Affichage d'un message d'erreur
- **10b**: Type de fichier non autorisé → Affichage d'un message d'erreur

**Postconditions**: Le fichier est partagé et accessible au destinataire

### 1.3 UC011 - Messages vocaux

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté et a un microphone

**Scénario principal**:
1. L'utilisateur appuie sur le bouton "Microphone"
2. L'utilisateur enregistre son message vocal
3. L'utilisateur relâche le bouton pour arrêter l'enregistrement
4. Le système encode l'audio en format compressé
5. Le système upload l'audio vers Firebase Storage
6. Le système envoie le message vocal via WebSocket
7. Le destinataire reçoit la notification
8. Le destinataire peut écouter le message vocal

**Scénarios alternatifs**:
- **11a**: Durée d'enregistrement trop longue → Affichage d'un message d'erreur
- **11b**: Microphone non disponible → Affichage d'un message d'erreur

**Postconditions**: Le message vocal est envoyé et audible par le destinataire

### 1.4 UC012 - Recherche de messages

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur clique sur l'icône "Recherche"
2. Une barre de recherche s'affiche
3. L'utilisateur saisit les mots-clés
4. Le système recherche dans l'historique des messages
5. Le système affiche les résultats avec contexte
6. L'utilisateur peut cliquer sur un résultat pour y accéder
7. Le système ouvre la conversation correspondante

**Scénarios alternatifs**:
- **12a**: Aucun résultat trouvé → Affichage d'un message "Aucun résultat"
- **12b**: Recherche trop courte → Affichage d'un message d'erreur

**Postconditions**: L'utilisateur peut naviguer vers les messages trouvés

### 1.5 UC013 - Notifications push

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur a activé les notifications

**Scénario principal**:
1. L'utilisateur reçoit un message en arrière-plan
2. Le système génère une notification push
3. La notification s'affiche sur le bureau
4. L'utilisateur clique sur la notification
5. L'application s'ouvre automatiquement
6. L'application navigue vers la conversation

**Scénarios alternatifs**:
- **13a**: Notifications désactivées → Aucune notification
- **13b**: Application en premier plan → Notification silencieuse

**Postconditions**: L'utilisateur est notifié des nouveaux messages

### 1.6 UC014 - Thèmes personnalisables

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne "Paramètres" → "Apparence"
2. Une boîte de dialogue s'ouvre
3. L'utilisateur choisit un thème (Clair, Sombre, Personnalisé)
4. L'utilisateur peut ajuster les couleurs
5. L'utilisateur clique sur "Appliquer"
6. Le système applique le thème immédiatement
7. Le système sauvegarde les préférences

**Scénarios alternatifs**:
- **14a**: Thème personnalisé invalide → Affichage d'un message d'erreur

**Postconditions**: L'interface utilise le nouveau thème

### 1.7 UC015 - Messages éphémères

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur active l'option "Message éphémère"
2. L'utilisateur saisit son message
3. L'utilisateur définit la durée de vie (1h, 24h, 7j)
4. L'utilisateur envoie le message
5. Le système marque le message comme éphémère
6. Le destinataire reçoit le message avec indicateur
7. Le système supprime automatiquement le message après expiration

**Scénarios alternatifs**:
- **15a**: Durée non définie → Utilisation de la durée par défaut

**Postconditions**: Le message disparaît automatiquement après expiration

### 1.8 UC016 - Statuts de lecture avancés

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur envoie un message
2. Le système affiche "Envoyé"
3. Le destinataire reçoit le message
4. Le système affiche "Livré"
5. Le destinataire ouvre la conversation
6. Le système affiche "Lu"
7. L'expéditeur voit le statut mis à jour en temps réel

**Scénarios alternatifs**:
- **16a**: Destinataire hors ligne → Statut "Envoyé" jusqu'à reconnexion

**Postconditions**: L'expéditeur connaît le statut de ses messages

## 2. FONCTIONNALITÉS DE MODÉRATION

### 2.1 UC017 - Gestion des administrateurs

**Acteur principal**: Administrateur

**Préconditions**: L'utilisateur a les droits d'administrateur

**Scénario principal**:
1. L'administrateur accède au panneau d'administration
2. L'administrateur peut :
   - Voir tous les utilisateurs
   - Suspendre des comptes
   - Supprimer des messages
   - Gérer les groupes
3. L'administrateur effectue les actions nécessaires
4. Le système applique les modifications
5. Les utilisateurs concernés sont notifiés

**Postconditions**: L'ordre est maintenu dans l'application

### 2.2 UC018 - Signalement de contenu

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur clique sur "Signaler" sur un message
2. Une boîte de dialogue s'ouvre
3. L'utilisateur sélectionne le motif de signalement
4. L'utilisateur ajoute un commentaire optionnel
5. L'utilisateur clique sur "Signaler"
6. Le système enregistre le signalement
7. Les administrateurs sont notifiés
8. Le message est temporairement masqué

**Postconditions**: Le contenu inapproprié est signalé aux modérateurs

## 3. FONCTIONNALITÉS DE SÉCURITÉ

### 3.1 UC019 - Chiffrement de bout en bout

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur active le chiffrement dans les paramètres
2. Le système génère une paire de clés
3. Les messages sont chiffrés côté client
4. Seuls les destinataires peuvent déchiffrer
5. Les messages sont stockés chiffrés dans Firebase

**Postconditions**: Les communications sont sécurisées de bout en bout

### 3.2 UC020 - Authentification à deux facteurs

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur a configuré la 2FA

**Scénario principal**:
1. L'utilisateur saisit ses identifiants
2. Le système envoie un code par SMS/email
3. L'utilisateur saisit le code reçu
4. Le système valide le code
5. L'utilisateur accède à l'application

**Scénarios alternatifs**:
- **20a**: Code incorrect → Demande d'un nouveau code
- **20b**: Code expiré → Demande d'un nouveau code

**Postconditions**: L'accès est sécurisé par authentification à deux facteurs

## 4. FONCTIONNALITÉS D'INTÉGRATION

### 4.1 UC021 - Intégration avec les réseaux sociaux

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur a un compte sur le réseau social

**Scénario principal**:
1. L'utilisateur sélectionne "Importer contacts"
2. L'utilisateur choisit le réseau social
3. L'utilisateur autorise l'accès
4. Le système importe les contacts
5. Le système propose d'ajouter les contacts existants

**Postconditions**: Les contacts des réseaux sociaux sont importés

### 4.2 UC022 - Synchronisation multi-appareils

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur utilise plusieurs appareils

**Scénario principal**:
1. L'utilisateur se connecte sur un nouvel appareil
2. Le système synchronise automatiquement :
   - Contacts
   - Messages
   - Paramètres
   - Thèmes
3. L'utilisateur peut continuer ses conversations

**Postconditions**: L'expérience utilisateur est cohérente sur tous les appareils

## 5. FONCTIONNALITÉS D'ANALYSE

### 5.1 UC023 - Statistiques d'utilisation

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur accède aux statistiques
2. Le système affiche :
   - Nombre de messages envoyés/reçus
   - Temps passé en ligne
   - Contacts les plus contactés
   - Heures d'activité
3. L'utilisateur peut exporter les données

**Postconditions**: L'utilisateur a une vue d'ensemble de son utilisation

### 5.2 UC024 - Rapports d'activité

**Acteur principal**: Administrateur

**Préconditions**: L'utilisateur a les droits d'administrateur

**Scénario principal**:
1. L'administrateur génère un rapport
2. Le système collecte les données :
   - Utilisateurs actifs
   - Messages échangés
   - Performances système
   - Incidents signalés
3. Le système génère un rapport PDF
4. L'administrateur peut télécharger le rapport

**Postconditions**: L'administrateur a une vue d'ensemble de l'activité

---

**Note**: Ces cas d'utilisation avancés peuvent être implémentés progressivement selon les priorités et ressources disponibles. 