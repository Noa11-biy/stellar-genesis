package com.stellargenesis.core.physics;


/**
 * Calcul de la couleur du ciel par diffusion de Rayleigh.
 *
 * === PHYSIQUE ===
 *
 * La diffusion de Rayleigh décrit comment les molécules de l'atmosphère
 * diffusent la lumière. L'intensité diffusée dépend de la longueur d'onde :
 *
 *   I_scatter(λ) ∝ 1/λ⁴
 *
 * Conséquences :
 *   λ_bleu  = 450 nm → (1/450)⁴ = 2.44 × 10⁻¹¹  (beaucoup diffusé)
 *   λ_vert  = 550 nm → (1/550)⁴ = 1.09 × 10⁻¹¹
 *   λ_rouge = 650 nm → (1/650)⁴ = 0.56 × 10⁻¹¹  (peu diffusé)
 *
 *   Ratio bleu/rouge : (650/450)⁴ ≈ 4.36
 *   → Le bleu est diffusé ~4.4× plus que le rouge
 *   → C'est pourquoi le ciel est bleu à midi
 *
 * Chemin optique (épaisseur d'atmosphère traversée) :
 *
 *   optical_path = 1 / max(sin(θ_soleil), 0.035)
 *
 *   θ = 90° (zénith)  → path = 1.0    → peu de diffusion → ciel bleu
 *   θ = 5°  (horizon) → path ≈ 11.5   → beaucoup de diffusion
 *   θ = 2°  (sunset)  → path ≈ 28.6   → le bleu est ENTIÈREMENT diffusé
 *                                         il ne reste que le rouge/orange
 *
 * C'est exactement pourquoi les couchers de soleil sont rouges :
 *   la lumière a traversé tellement d'atmosphère que tout le bleu
 *   a été diffusé sur le côté. Il ne reste que le rouge qui arrive
 *   en ligne droite jusqu'à l'observateur.
 *
 * Densité atmosphérique :
 *   Plus l'atmosphère est dense (pression élevée), plus il y a de molécules,
 *   plus la diffusion est forte.
 *
 *   scatter_strength ∝ P_surface / P_terre
 *
 *   P ≈ 0     → pas de diffusion → ciel noir (Lune, Mercure)
 *   P ≈ 1 bar → diffusion normale → ciel bleu (Terre)
 *   P ≈ 90 bar → diffusion extrême → ciel opaque orange (Vénus)
 *
 * Spectre stellaire (loi de Wien) :
 *   L'étoile n'émet pas toutes les couleurs également.
 *
 *   λ_max = 2.898 × 10⁶ / T_étoile  (nm)
 *
 *   T = 3000K (naine rouge M) → λ_max = 966 nm (infrarouge → lumière rouge)
 *   T = 5800K (Soleil G)      → λ_max = 500 nm (vert-jaune → lumière blanche)
 *   T = 10000K (étoile A)     → λ_max = 290 nm (UV → lumière bleu-violet)
 *
 *   La couleur de la lumière incidente modifie ce qui est disponible
 *   pour être diffusé. Autour d'une naine rouge, il y a MOINS de bleu
 *   dans la lumière → le ciel tire vers l'orange même à midi.
 */

public class RayleighScattering {

    // Longueurs d'onde de réf (nanomètre)
    private static final float LAMBDA_R = 650f;
    private static final float LAMBDA_G = 550f;
    private static final float LAMBDA_B = 450f;

    // Coefficients de Rayleigh normalisés : (λ_ref / λ)⁴
    // On normalise par rapport au rouge (le moins diffusé)
    // scatter_R = 1.0 (référence)
    // scatter_G = (650/550)⁴ = 1.96
    // scatter_B = (650/450)⁴ = 4.36
    private static final float SCATTER_R = 1.00f;
    private static final float SCATTER_G = (float) Math.pow(LAMBDA_R / LAMBDA_G, 4);
    private static final float SCATTER_B = (float) Math.pow(LAMBDA_R / LAMBDA_B, 4);

    /**
     * Calcule la couleur du ciel pour une configuration donnée.
     *
     * @param sunElevation   hauteur du soleil : 0 = horizon, 1 = zénith, <0 = sous l'horizon
     * @param surfacePressure pression atmosphérique en bars
     * @param starTemperature température de l'étoile en Kelvin
     * @return tableau [r, g, b] chaque composante dans [0, 1]
     */
    public static float[] computeSkyColor(float sunElevation, double surfacePressure,
                                          double starTemperature) {

        // === Pas d'atmosphère → ciel noir ===
        if (surfacePressure < 0.01) {
            return new float[]{0f, 0f, 0f};
        }

        // === Nuit → ciel très sombre ===
        if (sunElevation <= 0f) {
            // Petite lueur résiduelle (étoiles lointaines)
            float nightGlow = 0.01f;
            return new float[]{nightGlow, nightGlow, nightGlow * 1.5f};
        }

        // === 1. Spectre stellaire (quelle lumière arrive) ===
        // Approximation du spectre par corps noir à 3 longueurs d'onde
        // Loi de Planck simplifiée : B(λ,T) ∝ 1/(λ⁵ × (exp(hc/λkT) - 1))
        // On simplifie avec Wien : intensité ∝ exp(-hc / λkT)
        //
        // hc/k = 14388 μm·K = 1.4388 × 10⁷ nm·K
        float hc_over_k = 1.4388e7f;
        float T = (float) starTemperature;

        // === 1. Spectre stellaire simplifié ===
        // Au lieu de Wien (qui écrase le bleu pour T=5800K),
        // on utilise directement les coefficients Rayleigh comme poids.
        // La couleur du ciel vient de la DIFFUSION, pas du spectre direct.
        // Le spectre stellaire module légèrement, pas massivement.
        float lambda_peak = 2.898e6f / T; // nm (Wien)
        // Facteur spectral doux : gaussienne centrée sur le pic
        float starR = (float) Math.exp(-0.5 * Math.pow((LAMBDA_R - lambda_peak) / 150.0, 2));
        float starG = (float) Math.exp(-0.5 * Math.pow((LAMBDA_G - lambda_peak) / 150.0, 2));
        float starB = (float) Math.exp(-0.5 * Math.pow((LAMBDA_B - lambda_peak) / 150.0, 2));

        float starMax = Math.max(starR, Math.max(starG, starB));
        if (starMax > 0) { starR /= starMax; starG /= starMax; starB /= starMax; }

        // === 2. Chemin optique ===
        // Plus le soleil est bas, plus la lumière traverse d'atmosphère
        // sunElevation : 0 = horizon, 1 = zénith
        // On convertit en angle : elevation 1.0 = 90°, 0.0 = 0°
        float sinAngle = Math.max(0.035f, sunElevation);
        float opticalPath = 1.0f / sinAngle;
        // Clamp pour éviter des valeurs extrêmes
        opticalPath = Math.min(opticalPath, 30f);

        // === 3. Force de diffusion selon la pression ===
        // Normalisé à la Terre (1.013 bar)
        float densityFactor = (float) Math.min(surfacePressure / 1.013, 3.0);

        float scatterStrength = 0.8f * densityFactor;

        // === 4. Calcul de la diffusion pour chaque couleur ===
        // Intensité diffusée = starIntensity × scatter_coeff × densité
        // Mais : sur un long chemin optique, la lumière diffusée est RETIRÉE
        // du faisceau direct → extinction exponentielle
        //
        // I_diffusé(λ) = star(λ) × (1 - exp(-τ(λ)))
        // τ(λ) = scatter_coeff(λ) × optical_path × density
        //
        // Quand τ est petit : peu de diffusion (atmosphère fine ou midi)
        // Quand τ est grand : saturation (toute la lumière est diffusée)
        // Le bleu sature EN PREMIER → à l'horizon, le bleu est "épuisé"

        float tau_R = SCATTER_R * opticalPath * scatterStrength;
        float tau_G = SCATTER_G * opticalPath * scatterStrength;
        float tau_B = SCATTER_B * opticalPath * scatterStrength;

        // Le facteur 0.1 est un facteur d'échelle pour que les valeurs
        // soient visuellement correctes. Sans ça, τ serait trop grand
        // et tout serait saturé immédiatement.

        // Lumière diffusée vers l'observateur
        float skyR = starR * (1f - (float) Math.exp(-tau_R));
        float skyG = starG * (1f - (float) Math.exp(-tau_G));
        float skyB = starB * (1f - (float) Math.exp(-tau_B));

        // === 5. Modulation par l'intensité solaire ===
        // Plus le soleil est haut, plus il y a de lumière totale
        float brightness = Math.min(1f, sunElevation * 2f);

        float rayleighBoost_R = SCATTER_R / SCATTER_B; // ~0.23
        float rayleighBoost_G = SCATTER_G / SCATTER_B; // ~0.44
        float rayleighBoost_B = 1.0f;                  // référence


        skyR *= rayleighBoost_R * brightness;
        skyG *= rayleighBoost_G * brightness;
        skyB *= rayleighBoost_B * brightness;

        // Clamp
        skyR = Math.min(1f, Math.max(0f, skyR));
        skyG = Math.min(1f, Math.max(0f, skyG));
        skyB = Math.min(1f, Math.max(0f, skyB));

        System.out.println("sunElev=" + sunElevation
                + " tau_RGB=[" + tau_R + "," + tau_G + "," + tau_B + "]"
                + " star_RGB=[" + starR + "," + starG + "," + starB + "]"
                + " sky_RGB=[" + skyR + "," + skyG + "," + skyB + "]");

        return new float[]{skyR, skyG, skyB};
    }
}
