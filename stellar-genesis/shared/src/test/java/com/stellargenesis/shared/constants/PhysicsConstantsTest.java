package com.stellargenesis.shared.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validation des constantes physiques.
 *
 * POURQUOI TESTER DES CONSTANTES ?
 * _________________________________
 *
 * On teste les relations connues entre constantes pour vérifier leur cohérence
 *
 * @author Noa Moal
 */

class PhysicsConstantsTest {
    /**
     * Vérifie que g = GM/R² donne bien ~9.81 m/s² pour la Terre.
     *
     * C'est un test de COHÉRENCE : si G, EARTH_MASS ou EARTH_RADIUS
     * est faux, ce test échoue.
     *
     * Calcul attendu :
     *   g = 6.674e-11 × 5.972e24 / (6.371e6)²
     *   g = 3.986e14 / 4.059e13
     *   g ≈ 9.82 m/s²  (la vraie valeur est 9.81, l'écart vient des arrondis)
     *
     * On accepte 2% de tolérance car nos constantes sont arrondies.
     */
    @Test
    @DisplayName("g-Terre = GM/R² soit environ 9.81 m/s² (tolérance 2%)")
    void earthSurfaceGravityIsConsistent(){
        double g_calculated = PhysicsConstants.G * PhysicsConstants.EARTH_MASS
                / (PhysicsConstants.EARTH_RADIUS * PhysicsConstants.EARTH_RADIUS);

        assertEquals(PhysicsConstants.G_EARTH, g_calculated,
                PhysicsConstants.EARTH_RADIUS * 0.02,
                "g calculé depuis G, M_Terre, R_Terre doit être cohérent avec g_Terre Standard");
    }

    /**
     * Vérifie que la luminosité solaire est cohérente avec Stefan-Boltzmann.
     *
     * Relation : L = 4π × R² × σ × T⁴
     *
     * Si SOLAR_LUMINOSITY, SOLAR_RADIUS, SOLAR_TEMPERATURE ou STEFAN_BOLTZMANN
     * est incohérent, ce test échoue.
     *
     * Calcul :
     *   L = 4π × (6.957e8)² × 5.670e-8 × (5778)⁴
     *   L ≈ 3.85e26 W  (vs 3.846e26 W annoncé)
     */

    @Test
    @DisplayName("L_Soleil = 4πR²σT⁴ ≈ 3.846×10²⁶ W (tolérance 3%)")
    void solarLuminosityIsConsistent(){
        double L_calculated = 4.0 * Math.PI
                * PhysicsConstants.SOLAR_RADIUS * PhysicsConstants.SOLAR_RADIUS
                * PhysicsConstants.STEFAN_BLOTZMANN
                * Math.pow(PhysicsConstants.SOLAR_TEMPERATURE, 4);

        assertEquals(PhysicsConstants.SOLAR_LUMINOSITY, L_calculated,
                PhysicsConstants.SOLAR_LUMINOSITY* 0.03,
                "Luminosité solaire calculée via Stefan-Boltzmann doit être cohérente");
    }

    /**
     * Vérifie la relation R_gaz = k_B × N_A.
     *
     * La constante des gaz parfaits est le produit de la constante de Boltzmann
     * par le nombre d'Avogadro. C'est une relation EXACTE en physique.
     *
     * R = 1.38065e-23 × 6.022e23 = 8.314 J·mol⁻¹·K⁻¹
     */

    @Test
    @DisplayName("R_gaz = k_B x N_A (tolérence 0.1%)")
    void gasConstantRelation(){
        double R_calculated = PhysicsConstants.K_BLOTZMANN * PhysicsConstants.AVOGADRO;

        assertEquals(PhysicsConstants.R_GAZ, R_calculated,
                PhysicsConstants.R_GAZ * 0.001,
                "R_gaz doit être égal à k_B x N_A");
    }

    /**
     * Vérifie que la vitesse de libération terrestre est ~11.2 km/s.
     *
     * v_esc = √(2GM/R)
     *
     * C'est la vitesse minimum pour quitter la Terre sans propulsion continue.
     * C'est aussi un paramètre clé du gameplay : sur une planète avec
     * v_esc élevée, le joueur a besoin de plus de carburant pour décoller.
     */
    @Test
    @DisplayName("v_esc Terre = √(2GM/R) ≈ 11 186 m/s (tolérance 2%)")
    void earthEscapeVelocityIsReasonable(){
        double v_esc = Math.sqrt(
                2.0 * PhysicsConstants.G * PhysicsConstants.EARTH_MASS
                / PhysicsConstants.EARTH_RADIUS
        );

        double expected = 11_186.0; // m/s - valeur connue
        assertEquals(expected, v_esc, expected * 0.02,
                "Vitesse de libération terrestre doit être ~11.2Km/s");
    }


}
