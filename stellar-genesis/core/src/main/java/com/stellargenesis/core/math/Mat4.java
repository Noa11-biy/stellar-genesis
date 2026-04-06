package com.stellargenesis.core.math;

/**
 * Matrice 4×4 stockée en row-major.
 * Utilisée pour les transformations 3D : translation, rotation, mise à l'échelle, projection.
 *
 * Convention :
 *   m[ligne][colonne]
 *   m[0][0] m[0][1] m[0][2] m[0][3]
 *   m[1][0] m[1][1] m[1][2] m[1][3]
 *   m[2][0] m[2][1] m[2][2] m[2][3]
 *   m[3][0] m[3][1] m[3][2] m[3][3]
 *
 * @author Noa Moal
 *
 */

public class Mat4 {

    public final double[][] m = new double[4][4];

    // --- Constructeurs ---

    /** Matrice nulle (tous les éléments à 0). */
    public Mat4(){}

    /** Copie d'un tableau 4x4 existant. */
    public Mat4(double[][] values){
        for (int i = 0; i < 4; i++) {
            System.arraycopy(values[i], 0, m[i], 0, 4);
        }
    }

    // --- Fabriques statiques ---

    /** Matrices identité (neutre pour la multiplication). */
    public static Mat4 identity(){
        Mat4 r = new Mat4();
        r.m[0][0] = 1; r.m[1][1] = 1; r.m[2][2] = 1; r.m[3][3] = 1;
        return r;
    }

    /** Matrice de translation (tx, ty, tz). */
    public static Mat4 translation(double tx, double ty, double tz){
        Mat4 r = identity();
        r.m[0][3] = tx;
        r.m[1][3] = ty;
        r.m[2][3] = tz;
        return r;
    }

    /** Matrice de mise à l'échelle (sx, sy, sz). */
    public static Mat4 scale(double sx, double sy, double sz) {
        Mat4 r = new Mat4();
        r.m[0][0] = sx;
        r.m[1][1] = sy;
        r.m[2][2] = sz;
        r.m[3][3] = 1;
        return r;
    }

    /** Rotation autour de l'axe X (angle en radians). */
    public static Mat4 rotationX(double angle){
        Mat4 r = identity();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        r.m[1][1] = cos; r.m[1][2] = -sin;
        r.m[2][1] = sin; r.m[2][2] = cos;
        return r;
    }

    /** Rotation autour de l'axe Y (angle en radians). */
    public static Mat4 rotationY(double angle){
        Mat4 r = identity();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        r.m[0][0] = cos; r.m[0][2] = sin;
        r.m[2][0] = -sin; r.m[2][2] = cos;
        return r;
    }

    /** Rotation autour de l'axe Z (angle en radians). */
    public static Mat4 rotationZ(double angle){
        Mat4 r = identity();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        r.m[0][0] = cos; r.m[0][1] = -sin;
        r.m[1][0] = sin; r.m[1][1] = cos;
        return r;
    }

    // --- Opérations ---


    /** Multplication de deux Matrices this x other. */
    public Mat4 multiply(Mat4 other){
        Mat4 r = new Mat4();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += this.m[i][k] * other.m[k][j];
                }
                r.m[i][j] = sum;
            }
        }
        return r;
    }

    /** Transforme un Vec3 en point (w=1) : applique translation + rotation + échelle. */
    public Vec3 transformPoint(Vec3 v){
        double x = m[0][0]*v.x + m[0][1]*v.y + m[0][2]*v.z + m[0][3];
        double y = m[1][0]*v.x + m[1][1]*v.y + m[1][2]*v.z + m[1][3];
        double z = m[2][0]*v.x + m[2][1]*v.y + m[2][2]*v.z + m[2][3];
        double w = m[3][0]*v.x + m[3][1]*v.y + m[3][2]*v.z + m[3][3];
        if(w != 0 && w != 1){x /= w; y /= w; z /= w;}
        return new Vec3(x, y, z);
    }

    /** Transforme un Vec3 en direction (w=0) : ignore la transition. */
    public Vec3 transformDirection(Vec3 v){
        double x = m[0][0]*v.x + m[0][1]*v.y + m[0][2]*v.z;
        double y = m[1][0]*v.x + m[1][1]*v.y + m[1][2]*v.z;
        double z = m[2][0]*v.x + m[2][1]*v.y + m[2][2]*v.z;
        return new Vec3(x, y, z);
    }

    /** Transposée de la matrice. */
    public Mat4 transpose(){
        Mat4 r = new Mat4();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r.m[i][j] = this.m[j][i];
            }
        }
        return r;
    }

    /** Déterminant de la matrice 4x4. */
    public double determinant(){
        double det = 0;
        for (int j = 0; j < 4; j++) {
            det += (j % 2 == 0 ? 1 : -1) * m[0][j] * minor(0, j);
        }
        return det;
    }

    /** Mineur : déterminant de la sous-matrice 3×3 en excluant ligne row et colonne col. */
    private double minor(int row, int col){
        double[][] sub = new double[3][3];
       int si = 0;
        for (int i = 0; i < 4; i++) {
            if (i == row) continue;
            int sj = 0;
            for (int j = 0; j < 4; j++) {
                if (j == col) continue;
                sub[si][sj] = m[i][j];
                sj++;
            }
            si++;
        }
        return sub[0][0] * (sub[1][1]*sub[2][2] - sub[1][2]*sub[2][1])
                - sub[0][1] * (sub[1][0]*sub[2][2] - sub[1][2]*sub[2][0])
                + sub[0][2] * (sub[1][0]*sub[2][1] - sub[1][1]*sub[2][0]);
    }

    /** Inverse de la Matrice (null si derminant ≈ 0). */
    public Mat4 inverse(){
        double det = determinant();
        if (Math.abs(det) < 1e-12) return null; // matrice singulière

        Mat4 r = new Mat4();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double cofactor = ((i+j) % 2 == 0 ? 1 : -1) * minor(i, j);
                r.m[j][i] = cofactor / det;
            }
        }
        return r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mat4[\n");
        for (int i = 0; i < 4; i++) {
            sb.append(String.format("   [%8.4f %8.4f %8.4f %8.4f]\n",
                    m[i][0], m[i][1], m[i][2], m[i][3]));
        }
        sb.append("]");
        return sb.toString();
    }
}
