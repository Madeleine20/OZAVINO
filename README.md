# OZAVINO - Bibliothèque Universelle (Projet SWE 324)

Ozavino est une application de bureau développée en **Java Swing** permettant de gérer et de promouvoir un catalogue de livres issus de la littérature afro-descendante et multiculturelle. L'application intègre une gestion complète de l'expérience utilisateur, du suivi de lecture et de l'internationalisation.

---

## 🚀 Fonctionnalités Clés

* **Catalogue Interactif :** Exploration de livres répartis en 6 catégories culturelles (Littérature, Histoire, Société, Arts, Entrepreneuriat, Jeunesse).
* **Internationalisation (i18n) :** Traduction dynamique et intégrale de l'interface en 3 langues (**Français, Anglais, Espagnol**) via un système de dictionnaire centralisé (`HashMap`).
* **Suivi de Progression :** Barres de progression visuelles (`JProgressBar`) pour les lectures en cours et système d'objectifs personnalisés.
* **Persistance des Données :** Sauvegarde automatique des données utilisateur (profil, listes de lecture, avancée) via l'API `Preferences` de Java.
* **Recherche Avancée :** Moteur de recherche insensible à la casse filtrant instantanément par titre, auteur ou genre.
* **Notifications Stylisées :** Remplacement des fenêtres d'alerte standards par des boîtes de dialogue éphémères et personnalisées (`JDialog`).

---

## 🎨 Architecture & Concepts Swing Utilisés

* **Conteneur Principal :** `JFrame` structurée avec un `BorderLayout`.
* **Navigation Dynamique :** Gestion des pages via un `CardLayout` avec un système de cache de pages pour optimiser la mémoire vive.
* **Composants Personnalisés :** Utilisation de `Graphics2D` pour dessiner des panneaux aux coins arrondis (`fillRoundRect`) et intégration d'une barre de défilement (`JScrollPane`) personnalisée.
* **Gestion des Événements :** Utilisation d'écouteurs d'événements (`ActionListener`, `ItemListener`, `MouseAdapter`) pour une interface fluide et réactive.
