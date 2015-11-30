package Algorithms;

/**
 * Created by Stramek on 10.11.2015.
 */
public class ComplementaryAlgorithm {

    private float[] values = new float[9];
    private double[] newRotation = new double[9];


    public ComplementaryAlgorithm(float[] values, double[] newRotation) {
        this.values = values;
        this.newRotation = newRotation;
    }

    public Angles getRadian() {

        float alpha = 0;
        float betta = 0;
        float gamma = 0;

        double norm = 0;

        final double K = 0;

        final double dt = 10.0 / 1000.0;
        double[] gA = new double[3];
        double accelBeta = 0;
        double accelAlpha = 0;
        double magnGamma = 0;

        if(values[0] != 0) {
            double s1 = Math.sin(newRotation[0]);
            double s2 = Math.sin(newRotation[1]);
            double c1 = Math.cos(newRotation[0]);
            double c2 = Math.cos(newRotation[1]);
            gA[0] = ((c2 * -values[6]) + (s1 * s2 * -values[7]) + (c1 * s2 * -values[8]) + (c1 * s2 * -values[8])) / c2;
            gA[1] = ((c1 * c2 * -values[7]) - (s1 * c2 * -values[8])) / c2;
            gA[2] = ((s1 * -values[7]) + (c1 * -values[8])) / c2;
            norm = Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
            accelBeta = Math.asin(values[0] / norm);
            accelAlpha = -Math.atan2(values[1], values[2]);
            magnGamma = Math.atan2((values[4] * c1) + (values[5] * s1), (values[3] * c2) + (values[4] * s1 * s2) - (values[5] * c1 * s2));
            newRotation[0] = K * (newRotation[0] + gA[0] * dt) + (1 - K) * accelAlpha;
            newRotation[1] = K * (newRotation[1] + gA[1] * dt) + (1 - K) * accelBeta;
            newRotation[2] = K * (newRotation[2] + gA[2] * dt) + (1 - K) * magnGamma;
        }

        alpha = (float) newRotation[0];
        betta = (float) newRotation[1];
        gamma = (float) newRotation[2];

        return new Angles(alpha, betta, gamma);
    }

}
