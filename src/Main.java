import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import com.interactivemesh.jfx.importer.stl.StlMeshImporter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    private static final String MESH_FILENAME =
            "Original-satellite-dish.stl";

    private static final double MODEL_SCALE_FACTOR = 2; //400
    private static final double MODEL_X_OFFSET = -30; // standard
    private static final double MODEL_Y_OFFSET = 0; // standard

    private static final int VIEWPORT_SIZE = 800;

    private static final Color lightColor = Color.rgb(244, 255, 250);
    private static final Color objectColor = Color.rgb(100, 100, 100);//Color.rgb(0, 190, 222);

    private Group root;
    private PointLight pointLight;

    private MeshView[] meshViews;

    private double[] newRotation;
    private double[] lastRotation;

    private Group group;

    class Refresh implements Runnable {
        @Override
        public void run() {
            while(true) {
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
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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

        //AccelerometerAlgorithm aa = new AccelerometerAlgorithm(f);
        //newRotation[0] = aa.getAngle().getAlpha();
        //newRotation[1] = aa.getAngle().getBetta();

        ComplementaryAlgorithm ca = new ComplementaryAlgorithm(f, newRotation);
        newRotation[0] = ca.getAngle().getAlpha();
        newRotation[1] = ca.getAngle().getBetta();
        newRotation[2] = ca.getAngle().getGamma();

        for (int i = 0; i < meshViews.length; i++) {
            meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(lastRotation[1]), Rotate.Z_AXIS));
            //meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(-lastRotation[2]), Rotate.Y_AXIS));
            meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(lastRotation[0]), Rotate.X_AXIS));
            meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(-newRotation[0]), Rotate.X_AXIS));
            //meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(newRotation[2]), Rotate.Y_AXIS));
            meshViews[i].getTransforms().add(new Rotate(Math.toDegrees(-newRotation[1]), Rotate.Z_AXIS));
        }

        lastRotation[0] = newRotation[0];
        lastRotation[1] = newRotation[1];
        lastRotation[2] = newRotation[2];
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
        //fadfasd

        //final Text text1 = new Text(300, 300, getAddress());
        //text1.setFill(Color.WHITE);
        //text1.setFont(Font.font(java.awt.Font.SERIF, 30));
        //root.getChildren().add(text1);

        return root;
    }

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera();

        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }

    @Override
    public void start(Stage primaryStage) {
        lastRotation = new double[9];
        for(double d : lastRotation) d = 0;

        newRotation = new double[9];
        for(double d : newRotation) d = 0;

        group = buildScene();
        group.setScaleX(2);
        group.setScaleY(2);
        group.setScaleZ(2);
        group.setTranslateX(50);
        group.setTranslateY(50);

        Scene scene = new Scene(group, VIEWPORT_SIZE, VIEWPORT_SIZE, true);
        scene.setFill(Color.rgb(10, 10, 40));
        addCamera(scene);
        primaryStage.setTitle("Orientacja UDP");
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