package algorithms;

/**
 * Created by Stramek on 10.01.2016.
 */
public class MotorAngle implements Algorithm {

    private static final String TAG = "MotorAngle";

    private final double K = 0.98;
    private final double dt = 20 / 1000.0;

    private double accel[];
    private float beta;

    public MotorAngle() {
        accel = new double[3];
    }

    @Override
    public float[] calculate(float[] values) {

        for (int i = 0; i < accel.length; i++) {
            accel[i] = values[i] / (Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2)));
        }

        double sB = Math.sqrt(Math.pow(accel[0], 2) + Math.pow(accel[1], 2));

        if(accel[0] < 0)
            sB = -sB;

        double w = Math.sqrt(Math.pow(values[6], 2) + Math.pow(values[7], 2));

        if(values[7] < 0)
            w = -w;

        double betaAcc = Math.atan2(sB, accel[2]);

        if((beta < -Math.PI * 6 / 9) && (betaAcc > 0))
            betaAcc = -(2 * Math.PI - Math.abs(betaAcc));

        if((beta > Math.PI * 6 / 9) && (betaAcc < 0))
            betaAcc = 2 * Math.PI - Math.abs(betaAcc);

        beta = (float)(K * (beta + w * dt) + (1 - K) * betaAcc);

        if(beta > Math.PI)
            beta = (float) (beta - 2 * Math.PI);

        if(beta < -Math.PI)
            beta = (float) (beta + 2 * Math.PI);

        float[] ret = { (float)Math.toDegrees(beta) };
        return ret;
    }
}