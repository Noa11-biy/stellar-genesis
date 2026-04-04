<!-- === Fichier: README.md === -->

\# Stellar Genesis



\*\*Space Survival Factory RPG\*\* - Java / jMonkeyEngine 3



Stellar Genesis est un jeu de survie et de construction industrielle en monde 

ouvert spatial. L'univers est généré procéduralement et la physique est 

simulée de manière réaliste : gravité, température, pression atmosphérique, 

thermodynamique, mécanique orbitale.



\## Architecture 



stellar-genesis/

├── shared/    Constantes physiques, utilitaires mathématiques, DTOs

├── core/      Physique, génération procédurale, logique de jeu, IA

└── client/    Rendu jMonkeyEngine, contrôles joueur, UI



\## Prérequis

* Java 17+ (LTS)
* Maven 3.8+



\## Build \& Test

```bash

mvn clean compile	# Compile tout

mvn test		# Lance tous les tests









