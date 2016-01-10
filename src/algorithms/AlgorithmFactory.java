package algorithms;

/**
 * Created by Stramek on 04.01.2016.
 */
public class AlgorithmFactory {
    public Algorithm getAlgorithm(String algorithm) {

        if(algorithm == null) {
            return null;
        }

        if (algorithm.equalsIgnoreCase("ACCELEROMETER")) {
            return new Accelerometer();
        } else if (algorithm.equalsIgnoreCase("COMPLEMENTARY")) {
            return new Complementary();
        } else if (algorithm.equalsIgnoreCase("MADGWICKAMG")) {
            return new MadgwickAMG();
        } else if (algorithm.equalsIgnoreCase("MADGWICKAG")) {
            return new MadgwickAG();
        } else if (algorithm.equalsIgnoreCase("MOTORANGLE")) {
            return new MotorAngle();
        }

        return null;
    }
}
