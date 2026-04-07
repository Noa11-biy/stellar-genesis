package com.stellargenesis.core.physics;

import com.stellargenesis.core.math.MathUtils;
import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.shared.constants.PhysicsConstants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanetPhysicsTest {

    // ============================================================
    // TEST : Gravité de surface de la Terre
    // Attendu : ≈ 9.82 m/s² (la petite différence avec 9.81 vient
    //           du fait que la Terre n'est pas parfaitement sphérique)
    // ============================================================
    @Test
    void graviteTerrestreCorrecte() {
        double g = PlanetPhysics.surfaceGravity(
                PhysicsConstants.EARTH_MASS,
                PhysicsConstants.EARTH_RADIUS
        );
        assertEquals(9.82, g, 0.05, "Gravité terrestre doit être ≈ 9.82 m/s²");
    }

    // ============================================================
    // TEST : Vitesse de libération de la Terre
    // Attendu : ≈ 11 186 m/s (≈ 11.2 km/s)
    // ============================================================
    @Test
    void vitesseLibérationTerreCorrecte() {
        double vEsc = PlanetPhysics.escapeVelocity(
                PhysicsConstants.EARTH_MASS,
                PhysicsConstants.EARTH_RADIUS
        );
        assertEquals(11186.0, vEsc, 50.0, "v_esc Terre ≈ 11 186 m/s");
    }

    // ============================================================
    // TEST : Température d'équilibre de la Terre
    // Attendu : ≈ 255 K (-18°C)
    // La vraie température moyenne est ~288 K, la différence (+33K)
    // est due à l'effet de serre (non modélisé dans cette formule simple)
    // ============================================================
    @Test
    void temperatureEquilibreTerreCorrecte() {
        double T = PlanetPhysics.equilibriumTemperature(
                PhysicsConstants.SOLAR_TEMPERATURE,
                PhysicsConstants.SOLAR_RADIUS,
                PhysicsConstants.AU,
                0.30  // albedo terrestre
        );
        assertEquals(255.0, T, 5.0, "T_eq Terre ≈ 255 K");
    }

    // ============================================================
    // TEST : Pression à 5500m d'altitude sur Terre
    // Attendu : ≈ moitié de la pression au sol
    // "Scale height" de la Terre ≈ 8500m
    // P(5500) = 101325 × exp(-5500/8500) ≈ 53 000 Pa
    // ============================================================
    @Test
    void pressionAtmospheriqueAltitude() {
        double g = PhysicsConstants.G_EARTH;
        double P = PlanetPhysics.atmosphericPressure(
                101325.0,  // P0 = 1 atm en Pascals
                5500.0,    // altitude 5500m
                PhysicsConstants.MOLAR_MASS_AIR_EARTH,
                g,
                288.0      // température moyenne ~15°C
        );
        // Doit être entre 45000 et 55000 Pa (environ moitié)
        assertTrue(P > 45000 && P < 55000,
                "P à 5500m doit être ≈ moitié de 101325 Pa, obtenu : " + P);
    }

    // ============================================================
    // TEST : Gravité de surface de la Lune
    // Attendu : ≈ 1.62 m/s²
    // ============================================================
    @Test
    void graviteLuneCorrecte() {
        double masseLune = 7.342e22;  // kg
        double rayonLune = 1.737e6;   // m
        double g = PlanetPhysics.surfaceGravity(masseLune, rayonLune);
        assertEquals(1.62, g, 0.05, "Gravité lunaire ≈ 1.62 m/s²");
    }

    // ============================================================
    // TEST : Vitesse orbitale de l'ISS
    // ISS orbite à ~400 km d'altitude → r = R_terre + 400 km
    // Attendu : ≈ 7670 m/s
    // ============================================================
    @Test
    void vitesseOrbitaleISS() {
        double r = PhysicsConstants.EARTH_RADIUS + 400_000; // 400 km en mètres
        double vOrb = PlanetPhysics.orbitalVelocity(PhysicsConstants.EARTH_MASS, r);
        assertEquals(7670.0, vOrb, 50.0, "v_orb ISS ≈ 7670 m/s");
    }

    // ============================================================
    // TEST : Paramètres invalides → exceptions
    // ============================================================
    @Test
    void rayonZeroLanceException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PlanetPhysics.surfaceGravity(PhysicsConstants.EARTH_MASS, 0);
        });
    }

    @Test
    void albedoInvalideLanceException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PlanetPhysics.equilibriumTemperature(5778, 6.957e8, 1.496e11, -0.5);
        });
    }

    @Test
    void scenarioComplet_GenererUnePlaneteEtCalculerSesPrametres(){

        // Paramètre d'entrée : une planète type
        double masse = 5.972e24;
        double rayon = 6.371e6;
        double distanceEtoile = 1.496e11;
        double tempEtoile = 5778;
        double rayonEtoile = 6.957e8;
        double albedo = 0.3;

        // Calculs physiques
        double g = PlanetPhysics.surfaceGravity(masse, rayon);
        double T = PlanetPhysics.equilibriumTemperature(tempEtoile, rayonEtoile, distanceEtoile, albedo);
        double vEsc = PlanetPhysics.escapeVelocity(masse, rayon);

        // Vérif de cohérence juste savoir si c'est le bon ordre de grandeurs
        assertTrue(g > 9.0 && g < 10.0, "g Terrestre ~9.8");
        assertTrue(T > 240 && T < 270, "T_eq est 255 K");
        assertTrue(vEsc > 11000 && vEsc < 11500, "v_esc ~11.2 km/s");

        // Utilisation de MathUtils pour normaliser
        double gNorm = MathUtils.inverseLerp(0, 30, g); // 0..30 m/s²
        assertTrue(gNorm > 0.3 && gNorm < 0.4);

        // Utilisation
        Vec3 positionPlanete = new Vec3(distanceEtoile, 0, 0);
        assertEquals(distanceEtoile, positionPlanete.length(), 1e3);

    }


}
