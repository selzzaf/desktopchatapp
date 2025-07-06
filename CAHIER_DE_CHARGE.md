# CAHIER DE CHARGE - APPLICATION DE CHAT EN TEMPS RÉEL

## 1. PRÉSENTATION DU PROJET

### 1.1 Contexte
Développement d'une application de messagerie instantanée en temps réel utilisant Java Swing pour l'interface utilisateur et Spring Boot pour le backend, avec Firebase comme base de données en temps réel. L'application permet uniquement des conversations privées entre deux utilisateurs.

### 1.2 Objectifs
- Permettre aux utilisateurs de communiquer en temps réel en privé
- Gérer les contacts et les conversations individuelles
- Assurer la sécurité des communications
- Fournir une interface utilisateur intuitive

### 1.3 Technologies utilisées
- **Frontend**: Java Swing
- **Backend**: Spring Boot 2.7.0
- **Base de données**: Firebase Realtime Database
- **Authentification**: JWT (JSON Web Tokens)
- **Communication temps réel**: WebSocket (STOMP)
- **Sécurité**: Spring Security

## 2. FONCTIONNALITÉS PRINCIPALES

### 2.1 Gestion des utilisateurs
- Inscription de nouveaux utilisateurs
- Connexion/déconnexion
- Gestion des profils utilisateurs
- Statuts de présence (Online, Offline, Away, Busy)

### 2.2 Gestion des contacts
- Ajout de nouveaux contacts par email
- Liste des contacts avec statuts
- Suppression de contacts
- Recherche de contacts existants

### 2.3 Messagerie instantanée privée
- Envoi de messages en temps réel entre deux utilisateurs
- Réception de messages
- Indicateur de frappe ("typing...")
- Marquage des messages comme lus
- Historique des conversations privées

### 2.4 Interface utilisateur
- Interface graphique avec Java Swing
- Fenêtre de connexion
- Fenêtre principale avec panneaux séparés (contacts, chat, statut)
- Menu contextuel et barre de menu

## 3. CAS D'UTILISATION DÉTAILLÉS

### 3.1 UC001 - Inscription d'un nouvel utilisateur

**Acteur principal**: Utilisateur non inscrit

**Préconditions**: L'application est démarrée

**Scénario principal**:
1. L'utilisateur clique sur "S'inscrire" dans la fenêtre de connexion
2. Une boîte de dialogue d'inscription s'ouvre
3. L'utilisateur saisit :
   - Son nom complet
   - Son adresse email
   - Un mot de passe
   - La confirmation du mot de passe
4. L'utilisateur clique sur "S'inscrire"
5. Le système valide les données saisies
6. Le système crée le compte dans Firebase
7. Le système affiche un message de confirmation
8. La boîte de dialogue se ferme

**Scénarios alternatifs**:
- **3a**: Les champs sont vides → Affichage d'un message d'erreur
- **3b**: Les mots de passe ne correspondent pas → Affichage d'un message d'erreur
- **3c**: L'email existe déjà → Affichage d'un message d'erreur

**Postconditions**: L'utilisateur peut se connecter avec ses identifiants

### 3.2 UC002 - Connexion d'un utilisateur

**Acteur principal**: Utilisateur inscrit

**Préconditions**: L'utilisateur possède un compte valide

**Scénario principal**:
1. L'utilisateur saisit son email et mot de passe
2. L'utilisateur clique sur "Se connecter"
3. Le système valide les identifiants
4. Le système génère un token JWT
5. Le système met à jour le statut utilisateur à "Online"
6. Le système ouvre la fenêtre principale de chat
7. La fenêtre de connexion se ferme

**Scénarios alternatifs**:
- **2a**: Identifiants incorrects → Affichage d'un message d'erreur
- **2b**: Compte inexistant → Affichage d'un message d'erreur

**Postconditions**: L'utilisateur est connecté et peut utiliser l'application

### 3.3 UC003 - Ajout d'un nouveau contact

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne "Contacts" → "Ajouter un contact"
2. Une boîte de dialogue s'ouvre
3. L'utilisateur saisit l'email du contact à ajouter
4. L'utilisateur clique sur "Ajouter"
5. Le système recherche l'utilisateur par email
6. Le système ajoute le contact à la liste
7. Le système affiche un message de confirmation
8. La boîte de dialogue se ferme

**Scénarios alternatifs**:
- **3a**: L'email n'existe pas → Affichage d'un message d'erreur
- **3b**: Le contact est déjà dans la liste → Affichage d'un message d'erreur

**Postconditions**: Le nouveau contact apparaît dans la liste des contacts

### 3.4 UC004 - Envoi d'un message privé

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté et a sélectionné un contact

**Scénario principal**:
1. L'utilisateur sélectionne un contact dans la liste
2. L'utilisateur saisit son message dans la zone de texte
3. L'utilisateur appuie sur Entrée ou clique sur "Envoyer"
4. Le système envoie le message via WebSocket
5. Le système sauvegarde le message dans Firebase
6. Le message apparaît dans la conversation privée
7. Le destinataire reçoit le message en temps réel

**Scénarios alternatifs**:
- **4a**: Le message est vide → Aucune action
- **4b**: Problème de connexion → Affichage d'un message d'erreur

**Postconditions**: Le message est envoyé et visible dans la conversation privée

### 3.5 UC005 - Réception d'un message privé

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. Un autre utilisateur envoie un message privé
2. Le système reçoit le message via WebSocket
3. Le système affiche le message dans la conversation active
4. Le système met à jour la liste des conversations
5. Le système affiche une notification si la conversation n'est pas active

**Postconditions**: Le message est visible dans l'interface

### 3.6 UC006 - Modification du statut de présence

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne un nouveau statut dans le panneau de statut
2. Le système met à jour le statut dans Firebase
3. Le système notifie tous les contacts du changement
4. Les contacts voient le nouveau statut en temps réel

**Statuts disponibles**:
- Online (En ligne)
- Away (Absent)
- Busy (Occupé)
- Offline (Hors ligne)

**Postconditions**: Le nouveau statut est visible par tous les contacts

### 3.7 UC007 - Déconnexion

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne "Profil" → "Déconnexion"
2. Le système met à jour le statut à "Offline"
3. Le système ferme la connexion WebSocket
4. Le système retourne à la fenêtre de connexion
5. La fenêtre principale se ferme

**Scénarios alternatifs**:
- **7a**: L'utilisateur ferme la fenêtre → Déconnexion automatique

**Postconditions**: L'utilisateur est déconnecté et retourne à l'écran de connexion

### 3.8 UC008 - Modification du profil

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté

**Scénario principal**:
1. L'utilisateur sélectionne "Profil" → "Modifier le profil"
2. Une boîte de dialogue s'ouvre avec les informations actuelles
3. L'utilisateur modifie son nom et/ou mot de passe
4. L'utilisateur clique sur "Enregistrer"
5. Le système valide les modifications
6. Le système met à jour les informations dans Firebase
7. Le système affiche un message de confirmation
8. La boîte de dialogue se ferme

**Scénarios alternatifs**:
- **8a**: Les mots de passe ne correspondent pas → Affichage d'un message d'erreur

**Postconditions**: Les informations du profil sont mises à jour

### 3.9 UC009 - Suppression d'un contact

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté et a des contacts

**Scénario principal**:
1. L'utilisateur fait un clic droit sur un contact
2. L'utilisateur sélectionne "Supprimer le contact"
3. Le système affiche une boîte de dialogue de confirmation
4. L'utilisateur confirme la suppression
5. Le système supprime le contact de la liste
6. Le système met à jour Firebase
7. Le contact disparaît de la liste

**Scénarios alternatifs**:
- **9a**: L'utilisateur annule → Aucune action

**Postconditions**: Le contact est supprimé de la liste

### 3.10 UC010 - Indicateur de frappe

**Acteur principal**: Utilisateur connecté

**Préconditions**: L'utilisateur est connecté et a sélectionné un contact

**Scénario principal**:
1. L'utilisateur commence à taper un message
2. Le système envoie une notification "typing" via WebSocket
3. Le destinataire voit "X est en train d'écrire..."
4. L'utilisateur arrête de taper pendant 3 secondes
5. Le système envoie une notification "stopped typing"
6. L'indicateur disparaît chez le destinataire

**Postconditions**: Le destinataire sait quand l'utilisateur est en train d'écrire

## 4. ARCHITECTURE TECHNIQUE

### 4.1 Structure du projet
```
src/main/java/com/chatrealtime/
├── ChatRealtimeApplication.java (Point d'entrée)
├── config/ (Configuration Spring Boot)
├── controller/ (Contrôleurs REST et WebSocket)
├── model/ (Entités de données)
├── security/ (Sécurité et JWT)
├── service/ (Logique métier)
└── ui/ (Interface utilisateur Swing)
```

### 4.2 Composants principaux

#### 4.2.1 Backend (Spring Boot)
- **AuthController**: Gestion de l'authentification
- **ContactController**: Gestion des contacts
- **WebSocketController**: Communication temps réel
- **AuthService**: Logique d'authentification
- **MessageService**: Gestion des messages
- **ContactService**: Gestion des contacts

#### 4.2.2 Frontend (Java Swing)
- **LoginFrame**: Fenêtre de connexion
- **MainFrame**: Fenêtre principale
- **ContactsPanel**: Liste des contacts
- **ChatPanel**: Zone de conversation
- **StatusPanel**: Gestion des statuts

#### 4.2.3 Base de données (Firebase)
- **users**: Informations des utilisateurs
- **messages**: Messages échangés (privés uniquement)
- **contacts**: Relations entre utilisateurs

### 4.3 Communication temps réel
- **WebSocket/STOMP**: Pour les messages instantanés
- **Firebase Realtime Database**: Synchronisation des données
- **JWT**: Authentification sécurisée

## 5. EXIGENCES NON FONCTIONNELLES

### 5.1 Performance
- Temps de réponse < 2 secondes pour les opérations CRUD
- Latence < 500ms pour les messages temps réel
- Support de 100+ utilisateurs simultanés

### 5.2 Sécurité
- Authentification par JWT
- Validation des données côté serveur
- Protection contre les injections
- Chiffrement des mots de passe
- Conversations privées uniquement

### 5.3 Disponibilité
- Application disponible 24/7
- Gestion des déconnexions automatiques
- Reconnexion automatique en cas de perte de connexion

### 5.4 Interface utilisateur
- Interface intuitive et responsive
- Support des raccourcis clavier
- Notifications visuelles
- Gestion des erreurs utilisateur

## 6. TESTS ET VALIDATION

### 6.1 Tests unitaires
- Tests des services métier
- Tests des contrôleurs
- Tests de validation des données

### 6.2 Tests d'intégration
- Tests de communication WebSocket
- Tests d'authentification
- Tests de persistance Firebase

### 6.3 Tests de performance
- Tests de charge
- Tests de latence
- Tests de mémoire

## 7. DÉPLOIEMENT

### 7.1 Prérequis
- Java 11 ou supérieur
- Maven 3.6+
- Connexion Internet pour Firebase

### 7.2 Configuration
- Configuration Firebase dans `firebase-config.json`
- Paramètres dans `application.properties`
- Variables d'environnement pour la production

### 7.3 Lancement
```bash
mvn clean compile
mvn spring-boot:run
```

## 8. MAINTENANCE ET ÉVOLUTION

### 8.1 Fonctionnalités futures possibles
- Envoi de fichiers
- Messages vocaux
- Recherche de messages
- Notifications push
- Thèmes personnalisables
- **Groupes de discussion** (extension majeure)

### 8.2 Maintenance
- Mise à jour des dépendances
- Correction de bugs
- Optimisation des performances
- Amélioration de la sécurité

---

**Version**: 1.0  
**Date**: 2024  
**Auteur**: Équipe de développement ChatRealTime  
**Note**: Cette application gère uniquement les conversations privées entre deux utilisateurs. Les groupes de discussion ne sont pas implémentés dans la version actuelle. 