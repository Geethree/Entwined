import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import toxi.geom.Vec2D;
import toxi.geom.Vec3D;

import heronarts.lx.LX;
import heronarts.lx.LXLayer;
import heronarts.lx.LXLoopTask;
import heronarts.lx.model.LXAbstractFixture;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXTransform;


class Rod {

    public Vec3D mountingPoint;
    private double xKeyPoint;
    private double yKeyPoint;
    private double zKeyPoint;

    Rod(int canopyMajorLength, int rotationalPosition, int clusterBaseHeight, int rodLength) {
        int rotationIndex = rotationalPosition;
        float canopyScaling = canopyMajorLength / 180;
        double rodLengthRatios[] = { 0.37, 0.41, 0.50, 0.56, 0.63 };
        double heightAdjustmentFactors[] = { 1.0, 0.96, 0.92, 0.88, 0.85 };
        double branchLength = canopyMajorLength * rodLengthRatios[rotationIndex];
        xKeyPoint = branchLength;
        yKeyPoint = 72 * heightAdjustmentFactors[rotationIndex];
        zKeyPoint = branchLength * 0.199;

        LXTransform transform = new LXTransform();
        transform.rotateY(rotationalPosition * 45 * (Utils.PI / 180));


//            double ratio = (newX - xKeyPoint[keyPointIndex - 1]) / (xKeyPoint[keyPointIndex] - xKeyPoint[keyPointIndex - 1]);
//            double newY = yKeyPoint[keyPointIndex - 1] + ratio * (yKeyPoint[keyPointIndex] - yKeyPoint[keyPointIndex - 1])
//                    + clusterBaseHeight;
//            double newZ = zKeyPoint[keyPointIndex - 1] + ratio * (zKeyPoint[keyPointIndex] - zKeyPoint[keyPointIndex - 1]);
//            transform.push();
//            transform.translate((float) newX, (float) newY, (float) newZ);



        this.mountingPoint = new Vec3D(transform.x(), transform.y(), transform.z());
        transform.pop(); // (ashley) do we need this? I never took a graphics class so these calculations are confusing to me
    }

}

class EntwinedCluster {
    List<Rod> rods;

    EntwinedCluster(int canopyMajorLength, int clusterType, int clusterBaseHeight) {
        List<Rod> _rods = new ArrayList<Rod>();
        int rotationalPositions[];
        rotationalPositions = new int[] { 0, 1, 2, 3, 4 };

        int rodLengths[];
        switch (clusterType) {
            // A -> 0,
            // B -> 1,
            // C -> 2,
            // D -> 3
            case 0:
                rodLengths = new int[] { 54, 48, 42, 36, 30 };
                break;
            case 1:
                rodLengths = new int[] { 50, 44, 38, 32, 26 };
                break;
            case 2:
                rodLengths = new int[] { 46, 40, 34, 28, 22 };
                break;
            case 3:
                rodLengths = new int[] { 42, 36, 30, 24, 18 };
                break;
            default:
                rodLengths = new int[] {};
        }
        for (int i = 0; i < rotationalPositions.length; i++) {
            Rod p = new Rod(canopyMajorLength, rotationalPositions[i], clusterBaseHeight, rodLengths[i]);
            _rods.add(p);
        }
        this.rods = Collections.unmodifiableList(_rods);
    }
}

class ShrubModel extends LXModel {

    /**
     * Shrubs in the model
     */
    public final List<Shrub> shrubs;

    /**
     * ShrubCubes in the model
     */
    public final List<ShrubCube> shrubCubes;
    public final Map<String, ShrubCube[]> shrubIpMap = new HashMap<String, ShrubCube[]>();

    private final ArrayList<ShrubModelTransform> shrubModelTransforms = new ArrayList<ShrubModelTransform>();
    private final List<ShrubConfig> shrubConfigs;

    ShrubModel(List<ShrubConfig> shrubConfigs, List<ShrubCubeConfig> shrubCubeConfig) {
        super(new ShrubFixture(shrubConfigs, shrubCubeConfig));
        this.shrubConfigs = shrubConfigs;
        ShrubFixture f = (ShrubFixture) this.fixtures.get(0);
        List<ShrubCube> _cubes = new ArrayList<ShrubCube>();
        this.shrubs = Collections.unmodifiableList(f.shrubs);
        for (Shrub shrub : this.shrubs) {
            shrubIpMap.putAll(shrub.ipMap);
            _cubes.addAll(shrub.cubes);
        }
        this.shrubCubes = Collections.unmodifiableList(_cubes);
    }

    private static class ShrubFixture extends LXAbstractFixture {

        final List<Shrub> shrubs = new ArrayList<Shrub>();

        private ShrubFixture(List<ShrubConfig> shrubConfigs, List<ShrubCubeConfig> shrubCubeConfigs) {
            for (int i = 0; i < shrubConfigs.size(); i++) {
                ShrubConfig sc = shrubConfigs.get(i);
                shrubs.add(new Shrub(shrubCubeConfigs, i, sc.x, sc.z, sc.ry, sc.canopyMajorLengths, sc.clusterBaseHeights));
            }
            for (Shrub shrub : shrubs) {
                for (LXPoint p : shrub.points) {
                    points.add(p);
                }
            }
        }
    }

    public Vec3D getShrubMountPoint(ShrubCubeConfig c) {
        Vec3D p = null;
        Shrub shrub;
        try {
            shrub = this.shrubs.get(c.shrubIndex);
            p = shrub.shrubClusters.get(c.clusterIndex).rods.get(c.rodIndex).mountingPoint;
            return shrub.transformPoint(p);
        } catch (Exception e) {
            System.out.println("Error resolving mount point");
            System.out.println(e);
            return null;
        }
    }

    public void addShrubModelTransform(ShrubModelTransform modelTransform) {
        shrubModelTransforms.add(modelTransform);
    }

    public void runShrubTransforms() {
        for (ShrubCube cube : shrubCubes) {
            cube.resetTransform();
        }
        for (ShrubModelTransform modelTransform : shrubModelTransforms) {
            if (modelTransform.isEnabled()) {
                modelTransform.transform(this);
            }
        }
        for (ShrubCube cube : shrubCubes) {
            cube.didTransform();
        }
    }
}

class ShrubCubeConfig {
    int shrubIndex;
    int clusterIndex;
    int rodIndex;
    String ipAddress;
    int outputIndex;
    int cubeSizeIndex;
    boolean isActive;
}

class ShrubConfig {
    float x;
    float z;
    float ry;
    int[] canopyMajorLengths;
    int[] clusterBaseHeights;
}

class Shrub extends LXModel {

    /**
     * NDBs in the shrub
     */
    public final Map<String, ShrubCube[]> ipMap;

    /**
     * Cubes in the shrub
     */
    public final List<ShrubCube> cubes;

    /**
     * Clusters in the shrub
     */
    public final List<EntwinedCluster> shrubClusters;

    /**
     * index of the shrub
     */
    public final int index;

    /**
     * x-position of center of base of shrub
     */
    public final float x;

    /**
     * z-position of center of base of shrub
     */
    public final float z;

    /**
     * Rotation in degrees of shrub about vertical y-axis
     */
    public final float ry;

    Shrub(List<ShrubCubeConfig> shrubCubeConfig, int shrubIndex, float x, float z, float ry, int[] canopyMajorLengths,
            int[] clusterBaseHeights) {
        super(new Fixture(shrubCubeConfig, shrubIndex, x, z, ry, canopyMajorLengths, clusterBaseHeights));
        Fixture f = (Fixture) this.fixtures.get(0);
        this.index = shrubIndex;
        this.cubes = Collections.unmodifiableList(f.cubes);
        this.shrubClusters = f.shrubClusters;
        this.ipMap = f.ipMap;
        this.x = x;
        this.z = z;
        this.ry = ry;

    }

    public Vec3D transformPoint(Vec3D point) {
        return ((Fixture) this.fixtures.get(0)).transformPoint(point);
    }

    private static class Fixture extends LXAbstractFixture {

        final List<ShrubCube> cubes = new ArrayList<ShrubCube>();
        final List<EntwinedCluster> shrubClusters = new ArrayList<EntwinedCluster>();
        public final Map<String, ShrubCube[]> ipMap = new HashMap<String, ShrubCube[]>();
        public final LXTransform transform;

        Fixture(List<ShrubCubeConfig> shrubCubeConfig, int shrubIndex, float x, float z, float ry, int[] canopyMajorLengths,
                int[] clusterBaseHeights) {
            transform = new LXTransform();
            transform.translate(x, 0, z);
            transform.rotateY(ry * Utils.PI / 180);
            for (int i = 0; i < canopyMajorLengths.length; i++) {
                shrubClusters.add(new EntwinedCluster(canopyMajorLengths[i], i, clusterBaseHeights[i]));
            }
            for (ShrubCubeConfig cc : shrubCubeConfig) {
                if (cc.shrubIndex == shrubIndex) {
                    Vec3D p;
                    try {
                        p = shrubClusters.get(cc.clusterIndex).rods.get(cc.rodIndex).mountingPoint;
                    } catch (Exception e) {
                        System.out.println("Error loading config point");
                        System.out.println(e);
                        p = null;
                    }
                    if (p != null) {
                        cc.isActive = true;
                        ShrubCube cube = new ShrubCube(this.transformPoint(p), p, cc);
                        cubes.add(cube);
                        if (!ipMap.containsKey(cc.ipAddress)) {
                            ipMap.put(cc.ipAddress, new ShrubCube[16]);
                        }
                        ShrubCube[] ndbCubes = ipMap.get(cc.ipAddress);
                        ndbCubes[cc.outputIndex] = cube;
                    }
                }
            }
            for (Map.Entry<String, ShrubCube[]> entry : ipMap.entrySet()) {
                String ip = entry.getKey();
                ShrubCube[] ndbCubes = entry.getValue();
                for (int i = 0; i < 16; i++) {
                    if (ndbCubes[i] == null) { // fill all empty outputs with an inactive cube. Maybe this would be nicer to do at
                                               // the model level in the future.
                        ShrubCubeConfig cc = new ShrubCubeConfig();
                        cc.shrubIndex = shrubIndex;
                        cc.rodIndex = 0;
                        cc.cubeSizeIndex = 0;
                        cc.outputIndex = i;
                        cc.clusterIndex = 0;
                        cc.ipAddress = ip;
                        cc.isActive = false;
                        ShrubCube cube = new ShrubCube(new Vec3D(0, 0, 0), new Vec3D(0, 0, 0), cc);
                        cubes.add(cube);
                        ndbCubes[i] = cube;
                    }
                }
            }
            for (ShrubCube cube : this.cubes) {
                for (LXPoint p : cube.points) {
                    this.points.add(p);
                }
            }
        }

        public Vec3D transformPoint(Vec3D point) {
            this.transform.push();
            this.transform.translate(point.x, point.y, point.z);
            Vec3D result = new Vec3D(this.transform.x(), this.transform.y(), this.transform.z());
            this.transform.pop();
            return result;
        }
    }
}

class ShrubCube extends LXModel {

    public static final int[] PIXELS_PER_CUBE = { 6, 6, 6, 12, 12 }; // Tiny cubes actually have less, but for Entwined we want to
                                                                     // tell the NDB that everything is 6
    public static final float[] CUBE_SIZES = { 4f, 7.5f, 11.25f, 15f, 16.5f };

    /**
     * Index of this cube in color buffer, colors[cube.index]
     */
    public final int index;

    /**
     * Size of this cube, one of SMALL/MEDIUM/LARGE/GIANT
     */
    public final float size;

    public final int pixels;

    /**
     * Global x-position of center of cube
     */
    public final float x;

    /**
     * Global y-position of center of cube
     */
    public final float y;

    /**
     * Global z-position of center of cube
     */
    public final float z;

    /**
     * Pitch of cube, in degrees, relative to cluster
     */
    public final float rx;

    /**
     * Yaw of cube, in degrees, relative to cluster, after pitch
     */
    public final float ry;

    /**
     * Roll of cube, in degrees, relative to cluster, after pitch+yaw
     */
    public final float rz;

    /**
     * Local x-position of cube, relative to cluster base
     */
    public final float lx;

    /**
     * Local y-position of cube, relative to cluster base
     */
    public final float ly;

    /**
     * Local z-position of cube, relative to cluster base
     */
    public final float lz;

    /**
     * x-position of cube, relative to center of shrub base
     */
    public final float sx;

    /**
     * y-position of cube, relative to center of shrub base
     */
    public final float sy;

    /**
     * z-position of cube, relative to center of shrub base
     */
    public final float sz;

    /**
     * Radial distance from cube center to center of shrub in x-z plane
     */
    public final float r;

    /**
     * Angle in degrees from cube center to center of shrub in x-z plane
     */
    public final float theta;

    /**
     * Point of the cube in the form (theta, y) relative to center of shrub base
     */

    public float transformedY;
    public float transformedTheta;
    public Vec2D transformedCylinderPoint;
    public ShrubCubeConfig config = null;

    ShrubCube(Vec3D globalPosition, Vec3D shrubPosition, ShrubCubeConfig config) {
        super(Arrays.asList(new LXPoint[] { new LXPoint(globalPosition.x, globalPosition.y, globalPosition.z) }));
        this.index = this.points.get(0).index;
        this.size = CUBE_SIZES[config.cubeSizeIndex];
        this.pixels = PIXELS_PER_CUBE[config.cubeSizeIndex];
        this.rx = 0;
        this.ry = 0;
        this.rz = 0;
        this.lx = 0;
        this.ly = 0;
        this.lz = 0;
        this.x = globalPosition.x;
        this.y = globalPosition.y;
        this.z = globalPosition.z;
        this.sx = shrubPosition.x;
        this.sy = shrubPosition.y;
        this.sz = shrubPosition.z;
        this.r = (float) Point2D.distance(shrubPosition.x, shrubPosition.z, 0, 0);
        this.theta = 180 + 180 / Utils.PI * Utils.atan2(shrubPosition.z, shrubPosition.x);
        this.config = config;
    }

    void resetTransform() {

        transformedTheta = theta;
        transformedY = y;
    }

    void didTransform() {
        transformedCylinderPoint = new Vec2D(transformedTheta, transformedY);
    }
}

abstract class ShrubLayer extends LXLayer {

    protected final ShrubModel model;

    ShrubLayer(LX lx) {
        super(lx);
        model = (ShrubModel) lx.model;
    }
}

abstract class ShrubModelTransform extends Effect {
    ShrubModelTransform(LX lx) {
        super(lx);

        shrubModel.addShrubModelTransform(this);
    }

    @Override
    public void run(double deltaMs) {
    }

    abstract void transform(ShrubModel model);
}

class ShrubModelTransformTask implements LXLoopTask {

    protected final ShrubModel model;

    ShrubModelTransformTask(ShrubModel model) {
        this.model = model;
    }

    @Override
    public void loop(double deltaMs) {
        model.runShrubTransforms();
    }
}
