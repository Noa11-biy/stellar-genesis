package com.stellargenesis.client.render;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.stellargenesis.core.physics.PlanetData;
import com.stellargenesis.core.physics.RayleighScattering;

/**
 * Cycle jour/nuit basé sur la rotation planétaire.
 *
 * === PHYSIQUE ===
 *
 * Angle du soleil dans le ciel :
 *   θ(t) = 2π × t / T_rotation
 *
 *   T_rotation = période de rotation de la planète (secondes réelles en jeu)
 *   t = temps écoulé depuis le dernier "midi"
 *
 * Position du soleil (simplifiée, pas d'inclinaison axiale pour l'instant) :
 *   sun_dir.x = cos(θ)
 *   sun_dir.y = sin(θ)    ← positif = au-dessus de l'horizon
 *   sun_dir.z = 0
 *
 * La lumière pointe VERS le sol, donc :
 *   light_dir = -sun_dir  (le soleil éclaire depuis sa position)
 *
 * Intensité selon la hauteur du soleil (loi de Lambert) :
 *   I = max(0, sin(θ))
 *   → à midi (θ = π/2) : I = 1.0 (pleine lumière)
 *   → à l'horizon (θ = 0 ou π) : I = 0 (coucher/lever)
 *   → sous l'horizon (θ < 0) : I = 0 (nuit)
 *
 * Couleur du ciel au coucher/lever :
 *   Quand le soleil est bas, la lumière traverse plus d'atmosphère.
 *   Le chemin optique augmente → plus de diffusion Rayleigh
 *   → le bleu est entièrement diffusé, il ne reste que le rouge/orange
 *   C'est pourquoi les couchers de soleil sont rouges.
 *
 * Formule simplifiée du chemin optique :
 *   optical_depth ∝ 1 / max(sin(θ), 0.01)
 *   → à midi : chemin court → ciel bleu
 *   → à l'horizon : chemin ~50× plus long → ciel rouge
 */

public class DayNightCycle {

    // === Paramètres de la planète ===
    private float dayDurationSeconds; // durée d'un jour complet en secondes réelles
    private ColorRGBA baseSkyColor;   // couleur du ciel à midi (calculé par SkyManager)
    private double surfacePressure;
    private double starTemperature;

    // === État interne ===
    private float timeOfDay;          // 0.0 = midi, 0.25 = coucher, 0.5 = minuit, 0.75 = lever

    // === Références au lumières de la scène ===
    private DirectionalLight sunLight;
    private AmbientLight ambientLight;
    private SkyManager skyManager;

    // === Facteur de vitesse pour le debug
    private float timeScale = 1.0f;

    /**
     * @param planet       données physiques de la planète
     * @param sunLight     la DirectionalLight de la scène
     * @param ambientLight l'AmbientLight de la scène
     * @param skyManager   pour changer la couleur du ciel
     */
    public void init(PlanetData planet, DirectionalLight sunLight,
                     AmbientLight ambientLight, SkyManager skyManager){

        this.sunLight = sunLight;
        this.ambientLight = ambientLight;
        this.skyManager = skyManager;
        this.baseSkyColor = skyManager.getZenithColor();
        this.surfacePressure = planet.getSurfacePressure();
        this.starTemperature = planet.getStarTemperature();

        /*
         * Durée du jour en jeu :
         *
         * La vraie période de rotation (ex: Terre = 86400s = 24h) serait trop longue.
         * On compresse : 1 jour en jeu = quelques minutes réelles.
         *
         * Ratio : on garde les DIFFÉRENCES entre planètes.
         * Une planète qui tourne 2× plus vite que la Terre
         * aura des jours 2× plus courts en jeu aussi.
         *
         * Base : 1 jour Terre = 10 minutes réelles = 600 secondes
         */
        double rotationHours = planet.getRotationPeriod(); // ex : 24.0 pour la Terre
        double earthRatio = rotationHours / 86400.0;
        this.dayDurationSeconds = (float) (600.0 * earthRatio);

        // Commencer à midi
        this.timeOfDay = 0.0f;
    }

    /**
     * Appelé chaque frame.
     *
     * @param tpf temps écoulé depuis la dernière frame (secondes)
     */
    public void update(float tpf) {
//        System.out.println("day=" + dayDurationSeconds + " scale=" + timeScale);
        // === 1. Avancer le temps ===
        // timeOfDay va de 0 à 1 (un cycle complet)
        // 0.0  = midi
        // 0.25 = coucher de soleil
        // 0.5  = minuit
        // 0.75 = lever de soleil
        timeOfDay += (tpf * timeScale) / dayDurationSeconds;
        if (timeOfDay >= 1.0f) timeOfDay -= 1.0f;

        // === 2. Calculer l'angle du soleil ===
        // θ = 2π × timeOfDay, décalé pour que 0 = midi (soleil au zénith)
        float theta = FastMath.TWO_PI * timeOfDay + FastMath.HALF_PI;

        // Position du soleil
        // sin(theta) donne la hauteur : +1 à midi, -1 à minuit
        // cos(theta) donne la direction horizontale
        float sunHeight = FastMath.sin(theta);   // >0 = jour, <0 = nuit
        float sunHoriz = FastMath.cos(theta);

        // Direction de la lumière (pointe DEPUIS le soleil VERS le sol)
        Vector3f lightDir = new Vector3f(-sunHoriz, -sunHeight, -0.3f).normalizeLocal();
        sunLight.setDirection(lightDir);

        // === 3. Calculer l'intensité lumineuse ===
        // Jour : intensité proportionnelle à la hauteur du soleil
        // Nuit : intensité = 0 pour la lumière directionnelle
        float sunIntensity = Math.max(0f, sunHeight);

        // === 4. Couleur de la lumière directionnelle ===
        // Midi : blanche (1,1,1)
        // Coucher/lever : orange (1, 0.6, 0.3)
        // Nuit : éteinte (0,0,0)
        ColorRGBA sunColor = calculateSunColor(sunHeight, sunIntensity);
        sunLight.setColor(sunColor);

        // === 5. Lumière ambiante ===
        // Toujours un minimum la nuit (lumière des étoiles, réflexion atmosphérique)
        // Jour : ambiante plus forte
        float ambientFactor = 0.05f + 0.25f * sunIntensity;
        ambientLight.setColor(new ColorRGBA(ambientFactor, ambientFactor, ambientFactor * 1.2f, 1f));

        // === 6. Couleur du ciel ===
        ColorRGBA skyColor = calculateSkyColorForTime(sunHeight, sunIntensity);
        skyManager.setSkyColor(skyColor);

//        System.out.println("timeOfDay=" + timeOfDay + " sunHeight=" + FastMath.sin(FastMath.TWO_PI * timeOfDay + FastMath.HALF_PI));
    }

    /**
     * Couleur du ciel selon le moment de la journée.
     *
     * Midi     → baseSkyColor (bleu calculé par SkyManager)
     * Coucher  → orange/rouge (diffusion Rayleigh sur chemin long)
     * Nuit     → noir (pas de lumière à diffuser)
     * Lever    → orange/rouge (symétrique du coucher)
     */
    /**
     * Couleur de la lumière directionnelle du soleil.
     *
     * La lumière DIRECTE est ce qui reste APRÈS la diffusion.
     * C'est le COMPLÉMENTAIRE de ce qui est diffusé dans le ciel.
     *
     * À midi : peu de diffusion → lumière blanche
     * Au coucher : beaucoup de bleu diffusé → lumière rouge/orange
     *
     * I_direct(λ) = star(λ) × exp(-τ(λ))
     * (loi de Beer-Lambert : extinction exponentielle)
     */
    private ColorRGBA calculateSunColor(float sunHeight, float intensity) {
        if (intensity <= 0f) {
            return ColorRGBA.Black;
        }

        // Chemin optique
        float sinAngle = Math.max(0.035f, sunHeight);
        float opticalPath = Math.min(30f, 1.0f / sinAngle);
        float densityFactor = (float) Math.min(surfacePressure / 1.013, 3.0);

        // Coefficients de Rayleigh (identiques à RayleighScattering)
        float tau_R = 1.00f * opticalPath * densityFactor * 0.1f;
        float tau_G = 1.96f * opticalPath * densityFactor * 0.1f;
        float tau_B = 4.36f * opticalPath * densityFactor * 0.1f;

        // Beer-Lambert : ce qui survit = exp(-τ)
        float r = intensity * (float) Math.exp(-tau_R);
        float g = intensity * (float) Math.exp(-tau_G);
        float b = intensity * (float) Math.exp(-tau_B);

        return new ColorRGBA(
                Math.min(1f, r),
                Math.min(1f, g),
                Math.min(1f, b),
                1f
        );
    }

    /**
     * Couleur du ciel selon le moment de la journée.
     *
     * Midi     → baseSkyColor (bleu calculé par SkyManager)
     * Coucher  → orange/rouge (diffusion Rayleigh sur chemin long)
     * Nuit     → noir (pas de lumière à diffuser)
     * Lever    → orange/rouge (symétrique du coucher)
     */
    /**
     * Couleur du ciel — déléguée au calcul Rayleigh.
     */
    private ColorRGBA calculateSkyColorForTime(float sunHeight, float sunIntensity) {
        float[] rgb = RayleighScattering.computeSkyColor(
                sunHeight, surfacePressure, starTemperature
        );
        return new ColorRGBA(rgb[0], rgb[1], rgb[2], 1f);
    }

    // === Accesseurs ===

    /** 0.0 = midi, 0.25 = coucher, 0.5 = minuit, 0.75 = lever */
    public float getTimeOfDay() {
        return timeOfDay;
    }

    /** Forcer l'heure (debug). Ex: 0.25 = coucher de soleil */
    public void setTimeOfDay(float t) {
        this.timeOfDay = t % 1.0f;
    }

    /** Accélérer/ralentir le temps. 1.0 = normal, 10.0 = 10× plus vite */
    public void setTimeScale(float scale) {
        this.timeScale = scale;
    }

    /** true si le soleil est au-dessus de l'horizon */
    public boolean isDay() {
        float theta = FastMath.TWO_PI * timeOfDay;
        return FastMath.sin(theta) > 0;
    }

    public float getTimeScale() {
        return timeScale;
    }

}
