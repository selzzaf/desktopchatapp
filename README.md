# ChatRealTime - Application de Chat en Temps Réel

Une application de chat en temps réel construite avec Spring Boot, WebSocket, Firebase et Java Swing.

## Fonctionnalités

- Authentification sécurisée avec JWT
- Chat en temps réel via WebSocket
- Notifications système
- Statuts utilisateur (En ligne, Absent, Occupé, Hors ligne)
- Stockage des données avec Firebase Realtime Database

## Prérequis

- Java 11 ou supérieur
- Maven 3.6 ou supérieur
- Un compte Firebase avec une base de données Realtime Database

## Configuration

1. Clonez le dépôt :
```bash
git clone https://github.com/votre-username/ChatRealTime.git
cd ChatRealTime
```

2. Configurez Firebase :
   - Créez un projet dans la [Console Firebase](https://console.firebase.google.com)
   - Activez la Realtime Database
   - Générez un fichier de configuration de compte de service
   - Placez le fichier JSON de configuration dans `src/main/resources/firebase-config.json`
   
   **⚠️ IMPORTANT :** Les fichiers de configuration Firebase contiennent des clés privées sensibles. 
   - Ne committez JAMAIS les vrais fichiers de credentials dans Git
   - Utilisez le fichier `.gitignore` fourni pour éviter les commits accidentels
   - Pour le développement local, créez une copie de `firebase-config.json` avec vos vraies credentials

3. Configurez l'application :
   - Modifiez `src/main/resources/application.properties` :
     - Changez `jwt.secret` pour une clé secrète sécurisée
     - Ajustez les autres paramètres selon vos besoins

## Compilation et Exécution

1. Nettoyer puis Compilez le projet :
```bash
mvn clean package -DskipTests
```
```bash
mvn clean install
```

2. Exécutez l'application :
```bash
mvn spring-boot:run
```

L'interface graphique Swing devrait s'ouvrir automatiquement.

## Utilisation

1. Connexion :
   - Utilisez l'un des comptes de test :
     - Email : alice@example.com, Mot de passe : password123
     - Email : bob@example.com, Mot de passe : password123
     - Email : charlie@example.com, Mot de passe : password123
   - Ou créez un nouveau compte

2. Interface principale :
   - Liste des contacts à gauche
   - Zone de chat au centre
   - Statut utilisateur en haut
   - Notifications système en bas

3. Fonctionnalités :
   - Cliquez sur un contact pour démarrer une conversation
   - Changez votre statut en cliquant sur l'icône de statut
   - Recevez des notifications en temps réel

## Structure du Projet

```
src/main/java/com/chatrealtime/
├── config/           # Configuration (Firebase, WebSocket, Sécurité)
├── controller/       # Contrôleurs REST
├── model/           # Modèles de données
├── security/        # Configuration de sécurité JWT
├── service/         # Services métier
└── ui/              # Interface utilisateur Swing
```

## Sécurité

- Les mots de passe sont hashés avec BCrypt
- Les communications sont sécurisées avec JWT
- Les règles de sécurité Firebase sont configurées pour restreindre l'accès
- Les clés privées Firebase sont protégées par `.gitignore`

## Configuration Firebase

### Étapes de configuration :

1. **Créer un projet Firebase :**
   - Allez sur [Firebase Console](https://console.firebase.google.com)
   - Créez un nouveau projet ou utilisez un projet existant

2. **Activer Realtime Database :**
   - Dans la console Firebase, allez dans "Realtime Database"
   - Créez une nouvelle base de données
   - Choisissez les règles de sécurité appropriées

3. **Générer les credentials :**
   - Allez dans "Paramètres du projet" > "Comptes de service"
   - Cliquez sur "Générer une nouvelle clé privée"
   - Téléchargez le fichier JSON

4. **Configurer l'application :**
   - Copiez le fichier téléchargé vers `src/main/resources/firebase-config.json`
   - **NE COMMITTEZ JAMAIS ce fichier dans Git**
   - Utilisez `firebase-config-template.json` comme référence pour la structure

### Structure des données Firebase :

L'application utilise la structure suivante dans Firebase Realtime Database :
- `users_metadata` : Informations des utilisateurs
- `presence` : Statuts de présence
- `conversations` : Conversations entre utilisateurs
- `messages` : Messages des conversations
- `user_relations` : Relations entre utilisateurs
- `notifications` : Notifications système

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
1. Fork le projet
2. Créer une branche pour votre fonctionnalité
3. Commiter vos changements
4. Pousser vers la branche
5. Ouvrir une Pull Request

## Auteurs

**Github:**@selzzaf

## Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails. 

mvn clean install -DskipTests

mvn spring-boot:run -Dspring-boot.run.

java -jar target/ChatRealTime-1.0-SNAPSHOT.jar

mvn spring-boot:run
