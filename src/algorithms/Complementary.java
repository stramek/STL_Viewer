package algorithms;

public class Complementary implements Algorithm {

    private static final String TAG = "Complementary";

    private final double K = 0.98;
    private final double dt = 20.0 / 1000.0;

    private float[] newRotation;
    double[] gA;

    public Complementary() {
        gA = new double[3];
        newRotation = new float[3];
    }

    @Override
    public float[] calculate(float[] values) {
        if(values != null) {
            double s1 = Math.sin(newRotation[0]);
            double s2 = Math.sin(newRotation[1]);
            double c1 = Math.cos(newRotation[0]);
            double c2 = Math.cos(newRotation[1]);
            gA[0] = ((c2 * -values[6]) + (s1 * s2 * -values[7]) + (c1 * s2 * -values[8]) + (c1 * s2 * -values[8])) / c2;
            gA[1] = ((c1 * c2 * -values[7]) - (s1 * c2 * -values[8])) / c2;
            gA[2] = ((s1 * -values[7]) + (c1 * -values[8])) / c2;
            double norm = Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
            double accelBeta = Math.asin(values[0] / norm);
            double accelAlpha = -Math.atan2(values[1], values[2]);
            double magnGamma = Math.atan2((values[4] * c1) + (values[5] * s1), (values[3] * c2) + (values[4] * s1 * s2) - (values[5] * c1 * s2));
            newRotation[0] = (float) ((K * (newRotation[0] + (gA[0] * dt))) + ((1 - K) * accelAlpha));
            newRotation[1] = (float) ((K * (newRotation[1] + (gA[1] * dt))) + ((1 - K) * accelBeta));
            newRotation[2] = (float) ((K * (newRotation[2] + (gA[2] * dt))) + ((1 - K) * magnGamma));
        }

        float[] ret = {(float) Math.toDegrees(newRotation[0]), (float) Math.toDegrees(newRotation[1]), (float) Math.toDegrees(newRotation[2])};
        return ret;
    }
}