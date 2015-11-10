/**
 * Created by Stramek on 10.11.2015.
 */
public class Angles {
    private float angleX = 0;
    private float angleY = 0;
    private float angleZ = 0;

    public Angles(float angleX, float angleY, float angleZ) {
        this.angleX = angleX;
        this.angleY = angleY;
        this.angleZ = angleZ;
    }

    public float getAlpha() {
        return angleX;
    }

    public float getBetta() {
        return angleY;
    }

    public float getGamma() {
        return  angleZ;
    }
}
