<!-- === Fichier: DEVLOG.md === -->

# Stellar Genesis - Journal de Développement



## Conventions

* Une entrée par session de travail
* Format : date, durée, ce qui a été fait, ce qui a été appris, prochaine étape
* Les décisions d'architecture sont documentées avec le POURQUOI

---



##Session 001 - [04/04/2026]

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



### Prochaine session

* Implémenter `Vec3.java` et `Mat4.java` (bibliothèque mathématique maison)
* Tests unitaires : produit salaire, produit vectoriel, transformations

---



\## Session 002 - \[05/04/2026]

\*\*Durée\*\* : 5-6h

\*\*Phase\*\* : 1 - Fondations mathématiques et physiques 


\### Ce que j'ai fait : 
feat: Mat4 matrices 4x4 + transformations + inverse + tests

**Durée** : ~5-6h

**Phase** : 1 - Fondations matémathiques et physiques

### Ce que j'ai fait

 * Création de `Vec3.java`, `MathUtils.java`, `PlanetPhysics.java`
 * Tests Unitaire : `Vec3.java` :
 * addition de Vecteurs, soustration de Vecteurs, produit scalaire, produit Vectorielle, norme du Vecteur, normalisation, lerp Millieu
 * `MathUtils.java` :
 * clamp dans l'intervalle, clamp sous le min, clamp au dessus du max, inverse Lerp, remap valeur, approx Equals
 * `PlanetPhysics.java` :
 * gravité terrestre correcte, vitesse de libération de la Terre, Température équilibre, pression atmosphérique Altitude, gravité Lune correcte, vitesse orbital ISS


### Ce que j'ai appris
















