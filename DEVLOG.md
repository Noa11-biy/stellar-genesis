<!-- === Fichier: DEVLOG.md === -->

# Stellar Genesis - Journal de Développement



## Conventions

* Une entrée par session de travail
* Format : date, durée, ce qui a été fait, ce qui a été appris, prochaine étape
* Les décisions d'architecture sont documentées avec le POURQUOI

---



## Session 001 - [04/04/2026]

**Durée** : ~3.5-4h

**Phase** : 0 - Structure du projet



### Ce que j'ai fait

* Initialisation du dépôt Git avec .gitignore
* Création de la structure Maven multi-modules (shared, core, client)
* Implémentation `PhysicsConstants.java` avec toutes les constantes SI
* 4 tests unitaires de cohérence physique (tous passent)



### Décisions d'architecture

- **Pourquoi multi-modules ?** Pour séparer la physique (core) du rendu (client).

&#x20; Avantage : je peux tester 100% de ma physique sans démarrer jMonkeyEngine.

&#x20; Ça veut dire des tests rapides et une logique indépendante du moteur graphique.

&#x20; 

- **Pourquoi `shared` comme module séparé ?** Les constantes physiques sont utilisées

&#x20; partout (core ET client). Si elles étaient dans core, client devrait dépendre de core

&#x20; pour y accéder — ce qui est déjà le cas. Mais si un jour j'ajoute un module `server`,

&#x20; il pourra dépendre de shared sans dépendre de core.



### Ce que j'ai appris

- La relation R = k\_B × N\_A : la constante des gaz parfaits est littéralement

&#x20; "l'énergie thermique par mole par Kelvin", qui se décompose en

&#x20; "énergie par particule par Kelvin" × "nombre de particules par mole".

&#x20; 

- La luminosité stellaire dépend de T⁴ : une étoile 2× plus chaude que le Soleil

&#x20; rayonne 2⁴ = 16× plus d'énergie. C'est pour ça que les étoiles de type O

&#x20; (40 000 K) sont des millions de fois plus lumineuses que les naines rouges (3000 K).

### Difficulté rencontrées :
* Pas de grande diffculté rencontré pour cette Session mise à part de bien noté les constantes et de faire attention au arrondi de double le résultat en Java est considéré différent avec les arrondis donc au moins lui dire qu'il y aura une tolérence de plus ou moins 2-3%

### Prochaine session

* Implémenter `Vec3.java` et `Mat4.java` (bibliothèque mathématique maison)
* Tests unitaires : produit salaire, produit vectoriel, transformations

---



## Session 002 - [05/04/2026]

**Durée** : ~5-6h

**Phase** : 1 - Fondations matémathiques et physiques


### Ce que j'ai pas fait : 
feat: Mat4 matrices 4x4 + transformations + inverse + tests

### Ce que j'ai fait

 * Création de `Vec3.java`, `MathUtils.java`, `PlanetPhysics.java`
 * Tests Unitaire : `Vec3.java` :
 * addition de Vecteurs, soustration de Vecteurs, produit scalaire, produit Vectorielle, norme du Vecteur, normalisation, lerp Millieu
 * `MathUtils.java` :
 * clamp dans l'intervalle, clamp sous le min, clamp au dessus du max, inverse Lerp, remap valeur, approx Equals
 * `PlanetPhysics.java` :
 * gravité terrestre correcte, vitesse de libération de la Terre, Température équilibre, pression atmosphérique Altitude, gravité Lune correcte, vitesse orbital ISS


### Ce que j'ai appris/revu
 * Le produit scalaire : c'est pour me dire dans quel sens sont deux vecteurs généralement utilisé pour l'instensité d'éclairage, détécté un joueurs si il est devant ou derrière un ennemi

* L'interpolation linéaire : elle me permet d'estimer la valeur d'une fonction donnée dans un intervalle de deux points donnés

 * La partie hashCode de Vec3 : elle permet de transformer les 3 coordonnées en un seul int de manière à ce que deux Vec3 différents aient presque toujours un hashCode différent
POURQUOI pas toujours un hashCode différent ? : un int c'est 32 bits soit 4 milliards de valeurs possibles
Un Vec3 c'est 3 double donc 3 x 64bits soit 192 bits de combinaisons possibles
Comprimer 192 bits dans 32 bits c'est mathématiquement impossible que Vec3 ait un hash unique

* Remap : Convetir une valeur d'un intervalle à un autre.
Je cherche avec inverseLerp un valeurs dans l'intervalle de départ puis avec Lerp (DIFFÉRENT DE CELUI DE `Vec3`)
Exemple : `remap(50, 0, 100, 200, 400)` : inverLerp me dit que : 50 est à 50% de [0, 100] -> t = 0.5 
et Lerp me dit que : 50% de [200, 400] -> 300
Un usage concret pour ce projet : une alititude de 500m sur une planète où max=1000m, je veux convertir en pression entre 1.0 et 0.0bar -> 
`remap(500, 0, 1000, 1.0, 0.0)` -> 0.5 bar

### Prochaine session
* Implémenter `Mat4.java` 
* Tests unitaires : de toutes les méthodes de `Mat4`

--- 

## Session 003 - [06/04/26]

**Durée** : ~2-3h

**Phase** : 1 - Fondations matémathiques et physiques complétion

### Ce que j'ai fait
 * Implementer `Mat4` donc les matrices 4x4:
     * Doubles constructeurs : nulle et un copie d'un autre matrice déjà              existante
     * Les frabrique statiques :
         * La matrice identité (la base pour la les prochaines matrices)
         * Translation
         * Scale
         * Rotation X, Y, Z
     * Multiplication de matrice
     * Transformation de Vecteur en point / directions
     * Transposée d'un matrice
     * Inverse

### Ce que j'ai appris/revu
La moitié des méthodes de `Mat4` j'ai vu en cours sauf l'autre mais c'est pas difficile à comprendre sauf l'inverse compliqué à comprendre j'ai eu beaucoup de mal à comprendre 

### Difficulté rencontré
L'inverse, avec ces sous méthodes mineurs déterminant

### Prochaine session
RÉVISION DE PROJET !!!!

---


## Session 004 - [07/04/26]

**Durée** : 2.5 h

**Phase** : 1 - Révision de projet Test Unitaires plus complets avec un scénario réel

### Ce que j'ai fait
Révisé tout le projet corrigé quelques lignes, comprendre le code pour certaine partie que je n'ai pas totalement comprise essayé de l'expliqué

### Ce que j'ai appris
Pas plus mise à part certain point spécifique comme l'albedo

### Difficulté rencontré
Aucune

### Prochaine session
Commencer la phase 2 si possible sinon révision de projet

--- 

## Session 005 - [DATE]









