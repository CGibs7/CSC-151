import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Comparator;

/**
 * FixedSizeDodecahedron.java
 *
 * A self-contained Java Swing program that renders a rotating regular dodecahedron.
 * - Window: 300 x 400
 * - Faces: filled pentagons, different colors
 * - Only external (front-facing) faces are drawn (backface culling)
 * - Faces drawn with Painter's algorithm (depth sort) to avoid internal edges showing
 * - Slow rotation
 *
 * Coordinates for the faces are taken directly from Paul Bourke's dodecahedron POV file (b, c constants).
 */
public class FixedSizeDodecahedron extends JPanel implements ActionListener {
    private static final int W = 300;
    private static final int H = 400;
    private static final double ROT_SPEED = 0.01; // radians per tick (slow)

    private double angleX = -0.4;
    private double angleY = 0.5;
    private double angleZ = 0.0;

    private final Timer timer;

    // Colors for faces (12)
    private final Color[] faceColors = new Color[] {
            new Color(0xFF6B6B), new Color(0xFFA94D), new Color(0xFFD43B), new Color(0x9AE66E),
            new Color(0x4DDBFF), new Color(0x6C5CE7), new Color(0xFF9CEE), new Color(0x00C49A),
            new Color(0xE17055), new Color(0xF8EFBA), new Color(0x74B9FF), new Color(0xD3B6E8)
    };

    // We'll use the polygon coordinates directly as in Paul Bourke's dodecahedron.pov file.
    // phi, b, c are computed; coordinates are given exactly as in the POV polygons.
    private final double phi;
    private final double b;
    private final double c;

    // faces[faceIndex][vertexIndex][xyz]
    private final double[][][] faces;

    public FixedSizeDodecahedron() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        phi = (1.0 + Math.sqrt(5.0)) / 2.0;
        b = 1.0 / phi;
        c = 1.0 / (phi * phi); // as used in the Paul Bourke POV file

        // Define the 12 faces directly as coordinate lists (each face: 5 vertices)
            faces = new double[][][] {
            { { c, 0, 1}, {-c, 0, 1}, {-b, b, b}, {0, 1, c}, { b, b, b } },
            { {0, 1, c}, {0, 1, -c}, {-b, b, -b}, {-1, c, 0}, {-b, b, b } },
            { {-c, 0, 1}, { c, 0, 1}, { b, -b, b}, {0, -1, c}, {-b, -b, b } },
            { {0, -1, c}, {0, -1, -c}, { b, -b, -b}, {1, -c, 0}, { b, -b, b } },
            { { c, 0, -1}, {-c, 0, -1}, {-b, -b, -b}, {0, -1, -c}, { b, -b, -b } },
            { {0, -1, -c}, {0, -1, c}, {-b, -b, b}, {-1, -c, 0}, {-b, -b, -b } },
            { {-c, 0, -1}, { c, 0, -1}, { b, b, -b}, {0, 1, -c}, {-b, b, -b } },
            { {0, 1, -c}, {0, 1, c}, { b, b, b}, {1, c, 0}, { b, b, -b } },
            { {1, c, 0}, {1, -c, 0}, { b, -b, b}, { c, 0, 1}, { b, b, b } },
            { {1, -c, 0}, {1, c, 0}, { b, b, -b}, { c, 0, -1}, { b, -b, -b } },
            { {-1, c, 0}, {-1, -c, 0}, {-b, -b, -b}, {-c, 0, -1}, {-b, b, -b } },
            { {-1, -c, 0}, {-1, c, 0}, {-b, b, b}, {-c, 0, 1}, {-b, -b, b } }
        };


        // Scale down a bit to fit nicely and also divide by 2 if you want unit; we will scale later in projection.

        timer = new Timer(20, this); // 50 fps-ish; rotation controlled by small increments
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();

        // Enable antialiasing for smoother shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Center of panel
        int cx = W / 2;
        int cy = H / 2;

        // Prepare transformed polygons and their depths for painter's algorithm
        FaceRender[] toRender = new FaceRender[faces.length];

        // Rotation matrices components
        double sx = Math.sin(angleX), cxA = Math.cos(angleX);
        double sy = Math.sin(angleY), cyA = Math.cos(angleY);
        double sz = Math.sin(angleZ), cz = Math.cos(angleZ);

        // For each face compute rotated vertices, normal, cull if backface, and depth
        for (int i = 0; i < faces.length; i++) {
            double[][] face = faces[i];
            double[][] rotated = new double[5][3];
            double avgZ = 0;

            // Rotate each vertex (apply X, Y, Z rotations)
            for (int v = 0; v < 5; v++) {
                double x = face[v][0];
                double y = face[v][1];
                double z = face[v][2];

                // Optionally scale the model up a bit so it fills the window nicely
                double scaleModel = 120; // model units -> pixels (tune to fit 300x400)
                x *= scaleModel;
                y *= scaleModel;
                z *= scaleModel;

                // Rotation X
                double y1 = y * cxA - z * sx;
                double z1 = y * sx + z * cxA;
                // Rotation Y
                double x2 = x * cyA + z1 * sy;
                double z2 = -x * sy + z1 * cyA;
                // Rotation Z
                double x3 = x2 * cz - y1 * sz;
                double y3 = x2 * sz + y1 * cz;

                rotated[v][0] = x3;
                rotated[v][1] = y3;
                rotated[v][2] = z2;

                avgZ += z2;
            }
            avgZ /= 5.0;

            // Compute face normal using first 3 vertices (in rotated/model space)
            double[] v0 = rotated[0];
            double[] v1 = rotated[1];
            double[] v2 = rotated[2];
            double[] edge1 = new double[] { v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2] };
            double[] edge2 = new double[] { v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2] };
            double[] normal = cross(edge1, edge2);

            // Facing camera if normal.z < 0 (camera looks toward negative z direction)
            // Ensure consistent face orientation: facing camera if normal points toward camera
            // Camera looks along negative Z, so normal.z negative means front face
            double normalLength = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
            double nx = normal[0]/normalLength;
            double ny = normal[1]/normalLength;
            double nz = normal[2]/normalLength;

            // Facing camera if normal points toward camera (negative Z)
            boolean facing = nz < 0;

            // If normal accidentally points inward, flip the polygon
            if (!facing) {
                // reverse vertex order
                double[][] reversed = new double[5][3];
                for (int v = 0; v < 5; v++) reversed[v] = rotated[4 - v];
                rotated = reversed;
                avgZ = 0;
                for (int v = 0; v < 5; v++) avgZ += rotated[v][2];
                avgZ /= 5.0;
                facing = true; // now it's facing
            }


            // If facing camera, prepare projected 2D polygon
            if (facing) {
                int[] px = new int[5];
                int[] py = new int[5];
                for (int v = 0; v < 5; v++) {
                    // Simple perspective projection
                    double perspective = 600; // focal length (bigger -> less perspective)
                    double vz = rotated[v][2] + 400; // translate forward to keep positive denom
                    double proj = perspective / (perspective + vz);
                    double sx2 = rotated[v][0] * proj;
                    double sy2 = rotated[v][1] * proj;

                    px[v] = cx + (int) Math.round(sx2);
                    py[v] = cy - (int) Math.round(sy2);
                }
                toRender[i] = new FaceRender(i, px, py, avgZ);
            } else {
                toRender[i] = null; // culled
            }
        }

        // Depth sort (farther faces first) -> painter's algorithm
        FaceRender[] list = Arrays.stream(toRender).filter(fr -> fr != null).toArray(FaceRender[]::new);
        Arrays.sort(list, Comparator.comparingDouble(fr -> fr.avgZ)); // ascending avgZ (far -> near) because larger z is farther after translation

        // Draw faces
        for (int k = 0; k < list.length; k++) {
            FaceRender fr = list[k];
            Path2D path = new Path2D.Double();
            path.moveTo(fr.x[0], fr.y[0]);
            for (int j = 1; j < fr.x.length; j++) path.lineTo(fr.x[j], fr.y[j]);
            path.closePath();

            // Fill
            g.setColor(faceColors[fr.faceIndex % faceColors.length]);
            g.fill(path);

            // Outline
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(path);
        }

        g.dispose();
    }

    private static class FaceRender {
        int faceIndex;
        int[] x, y;
        double avgZ;
        FaceRender(int faceIndex, int[] x, int[] y, double avgZ) {
            this.faceIndex = faceIndex;
            this.x = x; this.y = y; this.avgZ = avgZ;
        }
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]
        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Slowly change angles
        angleY += ROT_SPEED * 0.6;
        angleX += ROT_SPEED * 0.35;
        angleZ += ROT_SPEED * 0.12;
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Fixed 3D Dodecahedron (300x400)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FixedSizeDodecahedron panel = new FixedSizeDodecahedron();
            f.getContentPane().add(panel);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
