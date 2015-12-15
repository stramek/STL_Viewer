import com.interactivemesh.jfx.importer.stl.StlMeshImporter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private static final String MESH_FILENAME =
            "silnik.stl";

    private static final double MODEL_SCALE_FACTOR = 0.5; //2
    private static final double MODEL_X_OFFSET = 0; // -30
    private static final double MODEL_Y_OFFSET = 30; // standard

    private static final int VIEWPORT_SIZE = 800;

    private static final Color lightColor = Color.rgb(244, 255, 250);
    private static final Color objectColor = Color.rgb(100, 100, 100);//Color.rgb(0, 190, 222);

    private Group root;
    private PointLight pointLight;

    private MeshView[] meshViews;

    private double[] lastQuaternion;

    private Group group;

    private boolean madgickAngle = true;

    public static int REFRESH_RATE = 15;

    private final int RAW_DATA = 1;
    private final int ACCELEROMETER = 2;
    private final int COMPLEMENTARY = 3;
    private final int MADGWICK = 4;
    private final int MADGWICK_IMU = 5;
    private final int MADGWICK_IMU_KAT = 6;

    class Refresh implements Runnable {
        @Override
        public void run() {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Service<Void> service = new Service<Void>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    //Background work
                                    final CountDownLatch latch = new CountDownLatch(1);
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                refreshValues(UDP.getValues());
                                            } finally {
                                                latch.countDown();
                                            }
                                        }
                                    });
                                    latch.await();
                                    //Keep with the background work
                                    return null;
                                }
                            };
                        }
                    };
                    service.start();
                }
            }, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
        }
    }


    private String getAddress() {
        InetAddress IP = null;
        try {
            IP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return IP.getHostAddress();
    }

    private void refreshValues(float[] f) {
        if(f[9] == ACCELEROMETER || f[9] == COMPLEMENTARY) {
            for(MeshView meshView : meshViews) {
                meshView.getTransforms().setAll(new Rotate(-f[0], Rotate.X_AXIS),
                        new Rotate(f[1], Rotate.Y_AXIS)/*,
                        new Rotate(-f[2], Rotate.Z_AXIS)*/);
            }
        }

        if(f[9] == MADGWICK || f[9] == MADGWICK_IMU) {
            for(MeshView meshView : meshViews) {
                meshView.getTransforms().setAll(calculateAffine(f));
            }
        }

        if(f[9] == MADGWICK_IMU_KAT) {
            for(MeshView meshView : meshViews) {
                meshView.getTransforms().setAll(new Rotate(calculateAngleMotor(f), Rotate.Z_AXIS));
            }
        } else {
            if(!madgickAngle)
                madgickAngle = true;
        }
    }

    private Affine calculateAffine(float[] quaternion) {
        double[][] matrix = new double[3][3];
        matrix[0][0] = 2 * Math.pow(quaternion[0], 2) - 1 + 2 * Math.pow(quaternion[1], 2);
        matrix[0][1] = 2 * (quaternion[1] * quaternion[2] + quaternion[0] * quaternion[3]);
        matrix[0][2] = 2 * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2]);
        matrix[1][0] = 2 * (quaternion[1] * quaternion[2] - quaternion[0] * quaternion[3]);
        matrix[1][1] = 2 * Math.pow(quaternion[0], 2) - 1 + 2 * Math.pow(quaternion[2], 2);
        matrix[1][2] = 2 * (quaternion[2] * quaternion[3] + quaternion[0] * quaternion[1]);
        matrix[2][0] = 2 * (quaternion[1] * quaternion[3] + quaternion[0] * quaternion[2]);
        matrix[2][1] = 2 * (quaternion[2] * quaternion[3] - quaternion[0] * quaternion[1]);
        matrix[2][2] = 2 * Math.pow(quaternion[0], 2) - 1 + 2 * Math.pow(quaternion[3], 2);
        matrix = trasposeMatrix(matrix);
        return new Affine(-matrix[0][0], -matrix[0][1], -matrix[0][2], 0,
                          matrix[1][0], matrix[1][1], matrix[1][2], 0,
                          matrix[2][0], matrix[2][1], matrix[2][2], 0);
    }

    private double calculateAngleMotor(float[] quaternion) {
        double quaternProd = (lastQuaternion[0] * quaternion[0]) -
                (lastQuaternion[1] * -quaternion[1]) -
                (lastQuaternion[2] * -quaternion[2]) -
                (lastQuaternion[3] * -quaternion[3]);

        if(madgickAngle) {
            for (int i = 0; i < 4; i++) {
                lastQuaternion[i] = quaternion[i];
            }
            madgickAngle = false;
        }
        //System.out.println(quaternProd);
        //System.out.println("" + Math.toDegrees(2 * Math.acos(quaternProd)));
        return Math.toDegrees(2 * Math.acos(quaternProd));
    }

    public static double[][] trasposeMatrix(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;

        double[][] trasposedMatrix = new double[n][m];

        for(int x = 0; x < n; x++) {
            for(int y = 0; y < m; y++) {
                trasposedMatrix[x][y] = matrix[y][x];
            }
        }
        return trasposedMatrix;
    }

    static MeshView[] loadMeshViews() {
        File file = new File(MESH_FILENAME);
        StlMeshImporter importer = new StlMeshImporter();
        importer.read(file);
        Mesh mesh = importer.getImport();

        return new MeshView[] { new MeshView(mesh) };
    }

    private Group buildScene() {
        meshViews = loadMeshViews();
        for (int i = 0; i < meshViews.length; i++) {
            meshViews[i].setTranslateX(VIEWPORT_SIZE / 2 + MODEL_X_OFFSET);
            meshViews[i].setTranslateY(VIEWPORT_SIZE / 2 + MODEL_Y_OFFSET);
            meshViews[i].setTranslateZ(VIEWPORT_SIZE / 2);
            meshViews[i].setScaleX(MODEL_SCALE_FACTOR);
            meshViews[i].setScaleY(MODEL_SCALE_FACTOR);
            meshViews[i].setScaleZ(MODEL_SCALE_FACTOR);

            PhongMaterial sample = new PhongMaterial(objectColor);
            sample.setSpecularColor(lightColor);
            sample.setSpecularPower(16);
            meshViews[i].setMaterial(sample);

        }

        pointLight = new PointLight(lightColor);
        pointLight.setTranslateX(VIEWPORT_SIZE*3/4);
        pointLight.setTranslateY(VIEWPORT_SIZE/2);
        pointLight.setTranslateZ(VIEWPORT_SIZE/2);
        PointLight pointLight2 = new PointLight(lightColor);
        pointLight2.setTranslateX(VIEWPORT_SIZE*1/4);
        pointLight2.setTranslateY(VIEWPORT_SIZE*3/4);
        pointLight2.setTranslateZ(VIEWPORT_SIZE*3/4);
        PointLight pointLight3 = new PointLight(lightColor);
        pointLight3.setTranslateX(VIEWPORT_SIZE*5/8);
        pointLight3.setTranslateY(VIEWPORT_SIZE/2);
        pointLight3.setTranslateZ(0);

        Color ambientColor = Color.rgb(80, 80, 80, 0);
        AmbientLight ambient = new AmbientLight(ambientColor);

        root = new Group(meshViews);
        root.getChildren().add(pointLight);
        root.getChildren().add(pointLight2);
        root.getChildren().add(pointLight3);
        root.getChildren().add(ambient);

/*
        final Text text1 = new Text(300, 300, getAddress());
        text1.setFill(Color.WHITE);
        text1.setFont(Font.font(java.awt.Font.SERIF, 30));
        root.getChildren().add(text1);
*/

        return root;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera();

        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }

    @Override
    public void start(Stage primaryStage) {
        lastQuaternion = new double[4];
        for(double d : lastQuaternion) d = 0;

        lastQuaternion = new double[4];
        for(double d : lastQuaternion) d = 0;

        group = buildScene();
        group.setScaleX(2);
        group.setScaleY(2);
        group.setScaleZ(2);
        //group.setTranslateX(50);
        //group.setTranslateY(50);

        Scene scene = new Scene(group, VIEWPORT_SIZE, VIEWPORT_SIZE, true);
        scene.setFill(Color.rgb(10, 10, 40));
        addCamera(scene);
        primaryStage.setTitle("Orientacja Server.UDP");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                });
            }
        });

        System.out.println("IP: " + getAddress());
        Thread t = new Thread(new UDP());
        t.start();

        Thread tr = new Thread(new Refresh());
        tr.start();
    }

    public static void main(String[] args) {
        System.setProperty("prism.dirtyopts", "false");
        launch(args);
    }
}