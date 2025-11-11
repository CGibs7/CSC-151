import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Shape_Calc_Group_Gibbons extends JFrame {

    private JPanel cards;
    private CardLayout cardLayout;

    private JPanel inputPanel;
    private Map<String, JTextField> inputs = new HashMap<>();
    private ShapeRenderPanel renderPanel;
    private ShapeRenderPanel compareRenderPanel;
    private Map<String, JTextField> compareInputs = new HashMap<>();
    private JPanel centerContainer;
    private CardLayout centerLayout;
    private JSplitPane splitPane;
    private JPanel singleWrapper;
    private JLabel resultLabel;
    private JComboBox<String> compareComboBox;
    private JLabel compareResultLabel;
    private JComboBox<String> compareMetricComboBox; // For 3D: Volume vs Surface Area
    private JComboBox<String> unitComboBox;
    private JTextField customUnitField;
    private String currentShape = "";
    private String previousMenu = "MAIN";
    private Double lastComputedVolume = null;
    private boolean lastIs2D = false;
    private Double lastComputedSurfaceArea = null; // for 3D
    private Double lastComputedArea2D = null; // for 2D

    public Shape_Calc_Group_Gibbons() {
        setTitle("Shape Calculator");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        getContentPane().add(cards);

        initMenu();
        init2DMenu();
        init3DMenu();

        cardLayout.show(cards, "MENU");
        setVisible(true);
    }

    // ------------------ Menus ------------------
    private void initMenu() {
        JPanel menuPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        JButton twoDButton = new JButton("2D Shapes");
        JButton threeDButton = new JButton("3D Shapes");
        JButton exitButton = new JButton("Exit");

        twoDButton.addActionListener(e -> { previousMenu = "MAIN"; cardLayout.show(cards, "2D"); });
        threeDButton.addActionListener(e -> { previousMenu = "MAIN"; cardLayout.show(cards, "3D"); });
        exitButton.addActionListener(e -> System.exit(0));

        menuPanel.add(twoDButton);
        menuPanel.add(threeDButton);
        menuPanel.add(exitButton);

        cards.add(menuPanel, "MENU");
    }

    private void init2DMenu() {
        JPanel twoDPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        String[] shapes = {"Rectangle", "Circle", "Ellipse", "Trapezoid", "Triangle"};
        for (String s : shapes) {
            JButton b = new JButton(s);
            b.addActionListener(e -> { currentShape = s; previousMenu = "2D"; showCalcScreen(); });
            twoDPanel.add(b);
        }
        JButton back = new JButton("Back");
        back.addActionListener(e -> cardLayout.show(cards, "MENU"));
        twoDPanel.add(back);
        cards.add(twoDPanel, "2D");
    }

    private void init3DMenu() {
        JPanel threeDPanel = new JPanel(new GridLayout(8, 1, 8, 8));
        String[] shapes = {"Tetrahedron", "Cube", "Octahedron", "Dodecahedron", "Icosahedron", "Sphere", "Cylinder"};
        for (String s : shapes) {
            JButton b = new JButton(s);
            b.addActionListener(e -> { currentShape = s; previousMenu = "3D"; showCalcScreen(); });
            threeDPanel.add(b);
        }
        JButton back = new JButton("Back");
        back.addActionListener(e -> cardLayout.show(cards, "MENU"));
        threeDPanel.add(back);
        cards.add(threeDPanel, "3D");
    }

    // ------------------ Calculation Screen ------------------
    private void showCalcScreen() {
        JPanel calcPanel = new JPanel(new BorderLayout(10, 10));
        inputs.clear();
        inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        switch (currentShape) {
            case "Rectangle":
                addField("Length"); addField("Width"); break;
            case "Triangle":
                addField("Length"); addField("Height"); break;
            case "Trapezoid":
                addField("Length"); addField("Length B"); addField("Height"); break;
            case "Circle":
            case "Sphere":
                addField("Radius"); break;
            case "Ellipse":
                addField("Major Axis"); addField("Minor Axis"); break;
            case "Cylinder":
                addField("Radius"); addField("Height"); break;
            case "Tetrahedron": case "Cube": case "Octahedron": case "Dodecahedron": case "Icosahedron":
                addField("Length"); break;
        }

        JPanel unitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        unitComboBox = new JComboBox<>(new String[]{"cm","m","in","ft","Custom"});
        customUnitField = new JTextField(8);
        customUnitField.setEnabled(false);
        unitComboBox.addActionListener(e -> customUnitField.setEnabled("Custom".equals(unitComboBox.getSelectedItem())));
        unitPanel.add(new JLabel("Units:"));
        unitPanel.add(unitComboBox);
        unitPanel.add(customUnitField);

        renderPanel = new ShapeRenderPanel(inputs);
        renderPanel.setPreferredSize(new Dimension(500, 350));
        renderPanel.setShape(currentShape);

        // Center container that switches between single and split view
        centerLayout = new CardLayout();
        centerContainer = new JPanel(centerLayout);
        singleWrapper = new JPanel(new BorderLayout());
        singleWrapper.add(renderPanel, BorderLayout.CENTER);
        centerContainer.add(singleWrapper, "SINGLE");
        // placeholder split pane
        compareInputs = new HashMap<>();
        compareRenderPanel = new ShapeRenderPanel(compareInputs);
        compareRenderPanel.setPreferredSize(new Dimension(500, 350));
        // Do NOT pass renderPanel here to avoid reparenting from singleWrapper
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), compareRenderPanel);
        splitPane.setResizeWeight(0.5);
        centerContainer.add(splitPane, "SPLIT");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new GridLayout(1,3,10,10));
        JButton calcButton = new JButton("Calculate");
        JButton cancelButton = new JButton("Cancel");
        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setForeground(Color.BLUE);
        buttonPanel.add(calcButton); buttonPanel.add(cancelButton); buttonPanel.add(resultLabel);

        JPanel comparePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comparePanel.add(new JLabel("Compare to:"));
        compareComboBox = new JComboBox<>();
        compareMetricComboBox = new JComboBox<>(new String[]{"Volume","Surface Area"});
        compareResultLabel = new JLabel("");
        refreshCompareOptions();
        comparePanel.add(compareComboBox);
        comparePanel.add(compareMetricComboBox);
        comparePanel.add(compareResultLabel);
        bottomPanel.add(unitPanel, BorderLayout.NORTH);
        bottomPanel.add(comparePanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        calcPanel.add(inputPanel, BorderLayout.NORTH);
        calcPanel.add(centerContainer, BorderLayout.CENTER);
        calcPanel.add(bottomPanel, BorderLayout.SOUTH);

        addInputListeners();
        calcButton.addActionListener(e -> calculate());
        cancelButton.addActionListener(e -> {
            resultLabel.setText("");
            compareResultLabel.setText("");
            lastComputedVolume = null;
            lastIs2D = false;
            if(previousMenu.equals("2D")) cardLayout.show(cards, "2D");
            else if(previousMenu.equals("3D")) cardLayout.show(cards, "3D");
            else cardLayout.show(cards, "MENU");
        });

        compareComboBox.addActionListener(e -> updateComparison());
        compareMetricComboBox.addActionListener(e -> updateComparison());

        cards.add(calcPanel, "CALC");
        cardLayout.show(cards, "CALC");
        revalidate();
        repaint();
    }

    private void addField(String label){
        JLabel lbl = new JLabel(label+":");
        JTextField tf = new JTextField();
        inputs.put(label, tf);
        inputPanel.add(lbl);
        inputPanel.add(tf);
    }

    private void addInputListeners(){
        for(JTextField tf: inputs.values()){
            tf.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e){ renderPanel.repaint(); }
                public void removeUpdate(DocumentEvent e){ renderPanel.repaint(); }
                public void changedUpdate(DocumentEvent e){ renderPanel.repaint(); }
            });
        }
    }

    private boolean isCurrent2D(){
        return Arrays.asList("Rectangle","Circle","Ellipse","Trapezoid","Triangle").contains(currentShape);
    }

   private void refreshCompareOptions(){
    if(compareComboBox == null) return;
    compareComboBox.removeAllItems();
    if(isCurrent2D()){
        compareMetricComboBox.setVisible(false);
        compareComboBox.addItem("(select)");
        // Add all 2D shapes
        String[] shapes2D = {"Square", "Rectangle", "Circle", "Ellipse", "Trapezoid", "Triangle", "Equilateral Triangle"};
        for(String s : shapes2D) compareComboBox.addItem(s);
    } else {
        compareMetricComboBox.setVisible(true);
        compareComboBox.addItem("(select)");
        for(String s : new String[]{"Tetrahedron","Cube","Octahedron","Dodecahedron","Icosahedron","Sphere","Cylinder"}){
            compareComboBox.addItem(s);
        }
    }
}


    private double getVal(String k){
        JTextField tf = inputs.get(k);
        if(tf!=null && !tf.getText().isEmpty()) return Double.parseDouble(tf.getText());
        return 50;
    }

    private String getUnit(){
        if("Custom".equals(unitComboBox.getSelectedItem())) {
            String c = customUnitField.getText().trim();
            return c.isEmpty()?"units":c;
        }
        return (String) unitComboBox.getSelectedItem();
    }

    private void calculate() {
        try {
            double length = getVal("Length"), lengthB = getVal("Length B"), width = getVal("Width");
            double height = getVal("Height"), radius = getVal("Radius");
            double major = getVal("Major Axis"), minor = getVal("Minor Axis");

            double result=0; boolean is2D=false;
            Double surfaceArea = null;
            switch(currentShape){
                case "Rectangle": result=length*width; is2D=true; break;
                case "Circle": result=Math.PI*radius*radius; is2D=true; break;
                case "Ellipse": result=Math.PI*major*minor; is2D=true; break;
                case "Trapezoid": result=0.5*(length+lengthB)*height; is2D=true; break;
                case "Triangle": result=0.5*length*height; is2D=true; break;
                case "Tetrahedron":
                    result=Math.pow(length,3)/(6.0*Math.sqrt(2.0));
                    surfaceArea = Math.sqrt(3.0)*length*length;
                    break;
                case "Cube":
                    result=Math.pow(length,3);
                    surfaceArea = 6.0*length*length;
                    break;
                case "Octahedron":
                    result=(Math.sqrt(2.0)/3.0)*Math.pow(length,3);
                    surfaceArea = 2.0*Math.sqrt(3.0)*length*length;
                    break;
                case "Dodecahedron":
                    result=((15.0+7.0*Math.sqrt(5.0))/4.0)*Math.pow(length,3);
                    surfaceArea = 3.0*Math.sqrt(25.0+10.0*Math.sqrt(5.0))*length*length;
                    break;
                case "Icosahedron":
                    result=(5.0/12.0)*(3.0+Math.sqrt(5.0))*Math.pow(length,3);
                    surfaceArea = 5.0*Math.sqrt(3.0)*length*length;
                    break;
                case "Sphere":
                    result=(4.0/3.0)*Math.PI*Math.pow(radius,3);
                    surfaceArea = 4.0*Math.PI*radius*radius;
                    break;
                case "Cylinder":
                    result=Math.PI*Math.pow(radius,2)*height;
                    surfaceArea = 2.0*Math.PI*radius*(radius+height);
                    break;
            }
            resultLabel.setForeground(Color.BLUE);
            resultLabel.setText(new DecimalFormat("0.000").format(result)+" "+getUnit()+(is2D?"²":"³"));
            lastComputedVolume = is2D ? null : result;
            lastComputedSurfaceArea = is2D ? null : surfaceArea;
            lastComputedArea2D = is2D ? result : null;
            lastIs2D = is2D;
            updateComparison();
        } catch(Exception e){
            resultLabel.setForeground(Color.RED);
            resultLabel.setText("Invalid input");
            compareResultLabel.setText("");
            lastComputedVolume = null;
            lastComputedSurfaceArea = null;
            lastComputedArea2D = null;
            lastIs2D = false;
        }
    }

    private void updateComparison(){
        if(compareComboBox == null) return;
        Object sel = compareComboBox.getSelectedItem();
        if(sel == null || "(select)".equals(sel)) { compareResultLabel.setText(""); return; }
        String target = sel.toString();
        String units = getUnit();
        if(isCurrent2D()){
            if(lastComputedArea2D == null){ compareResultLabel.setText(""); return; }
            String eq2 = computeEquivalent2D(target, lastComputedArea2D, units);
            compareResultLabel.setText(eq2);
            applyComparisonRender2D(target, eq2);
        } else {
            String metric = compareMetricComboBox != null && compareMetricComboBox.getSelectedItem()!=null ? compareMetricComboBox.getSelectedItem().toString() : "Volume";
            if("Surface Area".equals(metric)){
                if(lastComputedSurfaceArea == null){ compareResultLabel.setText(""); return; }
                String eqA = computeEquivalentDimensionsArea(target, lastComputedSurfaceArea, units);
                compareResultLabel.setText(eqA);
                applyComparisonRender3D(target, eqA);
            } else {
                if(lastComputedVolume == null){ compareResultLabel.setText(""); return; }
                String eqV = computeEquivalentDimensions(target, lastComputedVolume, units);
                compareResultLabel.setText(eqV);
                applyComparisonRender3D(target, eqV);
            }
        }
    }

    private void showSplit(){
        if(centerLayout == null) return;
        // Move renderPanel into split pane left side if not already there
        if(splitPane.getLeftComponent() != renderPanel){
            try { singleWrapper.remove(renderPanel); } catch(Exception ignored) {}
            splitPane.setLeftComponent(renderPanel);
        }
        centerLayout.show(centerContainer, "SPLIT");
    }

    private void showSingle(){
        if(centerLayout == null) return;
        // Move renderPanel back to the single wrapper if it sits in split pane
        if(splitPane.getLeftComponent() == renderPanel){
            splitPane.setLeftComponent(new JPanel());
            singleWrapper.add(renderPanel, BorderLayout.CENTER);
            singleWrapper.revalidate();
            singleWrapper.repaint();
        }
        centerLayout.show(centerContainer, "SINGLE");
    }

    private void applyComparisonRender2D(String target, String eq){
    Map<String, Double> params = parseParams(eq);
    compareInputs.clear();
    switch(target){
        case "Square":
            double side = params.getOrDefault("side", 50.0);
            compareInputs.put("Length", tfOf(side));
            compareInputs.put("Width", tfOf(side));
            compareRenderPanel.setShape("Rectangle");
            break;
        case "Rectangle":
            double l = params.getOrDefault("Length", 50.0);
            double w = params.getOrDefault("Width", 30.0);
            compareInputs.put("Length", tfOf(l));
            compareInputs.put("Width", tfOf(w));
            compareRenderPanel.setShape("Rectangle");
            break;
        case "Circle":
            double r = params.getOrDefault("r", 50.0);
            compareInputs.put("Radius", tfOf(r));
            compareRenderPanel.setShape("Circle");
            break;
        case "Ellipse":
            double a = params.getOrDefault("a", 50.0);
            double b = params.getOrDefault("b", 30.0);
            compareInputs.put("Major Axis", tfOf(a));
            compareInputs.put("Minor Axis", tfOf(b));
            compareRenderPanel.setShape("Ellipse");
            break;
        case "Trapezoid":
            double L1 = params.getOrDefault("Length", 50.0);
            double L2 = params.getOrDefault("Length B", 30.0);
            double H = params.getOrDefault("Height", 25.0);
            compareInputs.put("Length", tfOf(L1));
            compareInputs.put("Length B", tfOf(L2));
            compareInputs.put("Height", tfOf(H));
            compareRenderPanel.setShape("Trapezoid");
            break;
        case "Triangle":
        case "Equilateral Triangle":
            double sideEq = params.getOrDefault("side", 50.0);
            double heightEq = Math.sqrt(3.0)/2.0 * sideEq;
            compareInputs.put("Length", tfOf(sideEq));
            compareInputs.put("Height", tfOf(heightEq));
            compareRenderPanel.setShape("Triangle");
            break;
    }
    showSplit();
    compareRenderPanel.repaint();
}


    private void applyComparisonRender3D(String target, String eq){
        Map<String, Double> params = parseParams(eq);
        compareInputs.clear();
        if("Cube".equals(target) || "Tetrahedron".equals(target) || "Octahedron".equals(target)
            || "Dodecahedron".equals(target) || "Icosahedron".equals(target)){
            double edge = params.getOrDefault("edge", 50.0);
            compareInputs.put("Length", tfOf(edge));
            compareRenderPanel.setShape(target);
        } else if("Sphere".equals(target)){
            double r = params.getOrDefault("r", 50.0);
            compareInputs.put("Radius", tfOf(r));
            compareRenderPanel.setShape("Sphere");
        } else if("Cylinder".equals(target)){
            double r = params.getOrDefault("r", 30.0);
            double h = params.getOrDefault("h", 60.0);
            compareInputs.put("Radius", tfOf(r));
            compareInputs.put("Height", tfOf(h));
            compareRenderPanel.setShape("Cylinder");
        }
        showSplit();
        compareRenderPanel.repaint();
    }

    private Map<String, Double> parseParams(String eq){
        Map<String, Double> map = new HashMap<>();
        if(eq == null) return map;
        String[] parts = eq.split(",");
        for(String p : parts){
            String[] kv = p.trim().split("=");
            if(kv.length==2){
                try { map.put(kv[0].trim(), Double.parseDouble(kv[1].trim().split(" ")[0])); } catch(Exception ignored) {}
            }
        }
        return map;
    }

    private JTextField tfOf(double v){
        JTextField tf = new JTextField();
        tf.setText(new DecimalFormat("0.###").format(v));
        return tf;
    }

    private String computeEquivalentDimensions(String targetShape, double volume, String unit){
        DecimalFormat df = new DecimalFormat("0.###");
        switch(targetShape){
            case "Cube": {
                double edge = Math.cbrt(volume);
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Sphere": {
                double r = Math.cbrt((3.0*volume)/(4.0*Math.PI));
                return "r="+df.format(r)+" "+unit;
            }
            case "Cylinder": {
                // Assume h = 2r so V = 2π r^3
                double r = Math.cbrt(volume/(2.0*Math.PI));
                double h = 2.0*r;
                return "r="+df.format(r)+", h="+df.format(h)+" "+unit;
            }
            case "Tetrahedron": {
                double edge = Math.cbrt(volume * (6.0*Math.sqrt(2.0)));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Octahedron": {
                double edge = Math.cbrt(volume * (3.0/Math.sqrt(2.0)));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Dodecahedron": {
                double edge = Math.cbrt(volume * (4.0/(15.0+7.0*Math.sqrt(5.0))));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Icosahedron": {
                double edge = Math.cbrt(volume * (12.0/(5.0*(3.0+Math.sqrt(5.0)))));
                return "edge="+df.format(edge)+" "+unit;
            }
        }
        return "";
    }

    private String computeEquivalentDimensionsArea(String targetShape, double area, String unit){
        DecimalFormat df = new DecimalFormat("0.###");
        switch(targetShape){
            case "Cube": {
                double edge = Math.sqrt(area/6.0);
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Sphere": {
                double r = Math.sqrt(area/(4.0*Math.PI));
                return "r="+df.format(r)+" "+unit;
            }
            case "Cylinder": {
                // Assume h = 2r => A = 2πr(r+h) = 2πr(3r) = 6π r^2
                double r = Math.sqrt(area / (6.0*Math.PI));
                double h = 2.0*r;
                return "r="+df.format(r)+", h="+df.format(h)+" "+unit;
            }
            case "Tetrahedron": {
                double edge = Math.sqrt(area/Math.sqrt(3.0));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Octahedron": {
                double edge = Math.sqrt(area/(2.0*Math.sqrt(3.0)));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Dodecahedron": {
                double edge = Math.sqrt(area/(3.0*Math.sqrt(25.0+10.0*Math.sqrt(5.0))));
                return "edge="+df.format(edge)+" "+unit;
            }
            case "Icosahedron": {
                double edge = Math.sqrt(area/(5.0*Math.sqrt(3.0)));
                return "edge="+df.format(edge)+" "+unit;
            }
        }
        return "";
    }

    private String computeEquivalent2D(String targetShape, double area, String unit){
    DecimalFormat df = new DecimalFormat("0.###");
    switch(targetShape){
        case "Square": {
            double side = Math.sqrt(area);
            return "side="+df.format(side)+" "+unit;
        }
        case "Rectangle": {
            double length = Math.sqrt(area * 2/3.0); // Example ratio
            double width = area / length;
            return "Length="+df.format(length)+", Width="+df.format(width)+" "+unit;
        }
        case "Circle": {
            double r = Math.sqrt(area/Math.PI);
            return "r="+df.format(r)+" "+unit;
        }
        case "Ellipse": {
            double a = Math.sqrt(area);  // Example: a=b*2
            double b = area/(Math.PI*a);
            return "a="+df.format(a)+", b="+df.format(b)+" "+unit;
        }
        case "Trapezoid": {
            double L1 = Math.sqrt(area * 2/3.0);
            double L2 = L1 * 0.6;
            double H = 2*area/(L1+L2);
            return "Length="+df.format(L1)+", Length B="+df.format(L2)+", Height="+df.format(H)+" "+unit;
        }
        case "Triangle":
        case "Equilateral Triangle": {
            double side = Math.sqrt((4.0*area)/Math.sqrt(3.0));
            return "side="+df.format(side)+" "+unit;
        }
    }
    return "";
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Shape_Calc_Group_Gibbons::new);
    }

    // ------------------ Shape Rendering Panel ------------------
    static class ShapeRenderPanel extends JPanel{
        private Map<String,JTextField> inputs;
        private String shape="";
        private Timer timer;
        private double angleX=0, angleY=0;

        public ShapeRenderPanel(Map<String,JTextField> inputs){
            this.inputs=inputs;
            timer = new Timer(40, e -> {
                angleX += 0.02; angleY += 0.01;
                repaint();
            });
            timer.start();
        }
        public void setShape(String s){ shape=s; repaint(); }

        private double getRenderVal(String key){
            // Lock 3D sizes independent of inputs
            if(Arrays.asList("Tetrahedron","Cube","Octahedron","Dodecahedron","Icosahedron","Sphere","Cylinder").contains(shape)){
                switch(key){
                    case "Length": return 100.0; // nominal edge
                    case "Radius": return 50.0;  // nominal radius
                    case "Height": return 100.0; // nominal height
                }
            }
            return getVal(key);
        }

        private double getVal(String key){
            JTextField tf = inputs.get(key);
            if(tf!=null && !tf.getText().isEmpty()){
                try { return Double.parseDouble(tf.getText()); } catch(Exception e){ return 50; }
            }
            return 50;
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setStroke(new BasicStroke(2));
            int w=getWidth(), h=getHeight();
            int cx=w/2, cy=h/2;

            g2.setColor(Color.BLACK);
            g2.drawString("Legend: rotating 3D shapes", 10, 20);

            switch(shape){
                case "Rectangle": drawRect(g2,w,h,20); break;
                case "Circle": drawCircle(g2,w,h,20); break;
                case "Ellipse": drawEllipse(g2,w,h,20); break;
                case "Triangle": drawTriangle(g2,w,h,20); break;
                case "Trapezoid": drawTrapezoid(g2,w,h,20); break;
                case "Cube": drawPolyhedron(g2,w,h,cx,cy,getCubeVertices(), getCubeFaces(), new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.MAGENTA,Color.CYAN}); break;
                case "Tetrahedron": drawPolyhedron(g2,w,h,cx,cy,getTetraVertices(), getTetraFaces(), new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE}); break;
                case "Octahedron": drawPolyhedron(g2,w,h,cx,cy,getOctaVertices(), getOctaFaces(), new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.MAGENTA,Color.CYAN,Color.PINK,Color.YELLOW}); break;
                case "Dodecahedron": drawDodecahedron(g2,w,h,cx,cy); break;
                case "Icosahedron": drawPolyhedron(g2,w,h,cx,cy,getIcosaVertices(), getIcosaFaces(), new Color[]{Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.MAGENTA,Color.CYAN}); break;
                case "Sphere": drawSphere(g2,w,h,cx,cy); break;
                case "Cylinder": drawCylinder(g2,w,h,cx,cy); break;
            }
        }

        // --- 2D shapes ---
        private void drawRect(Graphics2D g,int w,int h,int m){
            double length=getVal("Length"), width=getVal("Width");
            int rw=(int)Math.min(length,w-2*m);
            int rh=(int)Math.min(width,h-2*m);
            g.setColor(Color.LIGHT_GRAY); g.fillRect(m,m,rw,rh);
            g.setColor(Color.BLACK); g.drawRect(m,m,rw,rh);
        }
        private void drawCircle(Graphics2D g,int w,int h,int m){
            double r=getVal("Radius");
            int rad=(int)Math.min(r,Math.min((w-2*m)/2,(h-2*m)/2));
            g.setColor(Color.LIGHT_GRAY); g.fillOval(w/2-rad,h/2-rad,2*rad,2*rad);
            g.setColor(Color.BLACK); g.drawOval(w/2-rad,h/2-rad,2*rad,2*rad);
        }
        private void drawEllipse(Graphics2D g,int w,int h,int m){
            double a=getVal("Major Axis"), b=getVal("Minor Axis");
            int rw=(int)Math.min(a,w-2*m), rh=(int)Math.min(b,h-2*m);
            g.setColor(Color.LIGHT_GRAY); g.fillOval((w-rw)/2,(h-rh)/2,rw,rh);
            g.setColor(Color.BLACK); g.drawOval((w-rw)/2,(h-rh)/2,rw,rh);
        }
        private void drawTriangle(Graphics2D g,int w,int h,int m){
            double l=getVal("Length"), ht=getVal("Height");
            int x1=m,y1=h-m, x2=(int)Math.min(m+l,w-m), y2=h-m, x3=x1+(int)(l/2), y3=(int)Math.max(m,h-ht);
            g.setColor(Color.LIGHT_GRAY); g.fillPolygon(new int[]{x1,x2,x3},new int[]{y1,y2,y3},3);
            g.setColor(Color.BLACK); g.drawPolygon(new int[]{x1,x2,x3},new int[]{y1,y2,y3},3);
        }
        private void drawTrapezoid(Graphics2D g,int w,int h,int m){
            double l=getVal("Length"), lb=getVal("Length B"), ht=getVal("Height");
            int x1=(w-(int)lb)/2, y1=h-m;
            int x2=x1+(int)lb, y2=y1;
            int x3=x1+(int)(lb-(lb-l)/2), y3=y1-(int)ht;
            int x4=x1+(int)((lb-l)/2), y4=y1-(int)ht;
            g.setColor(Color.LIGHT_GRAY); g.fillPolygon(new int[]{x1,x2,x3,x4}, new int[]{y1,y2,y3,y4},4);
            g.setColor(Color.BLACK); g.drawPolygon(new int[]{x1,x2,x3,x4}, new int[]{y1,y2,y3,y4},4);
        }

        // --- 3D polyhedron (cyclic ordering + painter sort) ---
        private void drawPolyhedron(Graphics2D g,int panelW,int panelH,int cx,int cy,double[][] verts,int[][] faces, Color[] faceColors){
            double[][] rot = new double[verts.length][3];
            for(int i=0;i<verts.length;i++){
                double x=verts[i][0], y=verts[i][1], z=verts[i][2];
                double x1 = x*Math.cos(angleY) - z*Math.sin(angleY);
                double z1 = x*Math.sin(angleY) + z*Math.cos(angleY);
                double y1 = y*Math.cos(angleX) - z1*Math.sin(angleX);
                double z2 = y*Math.sin(angleX) + z1*Math.cos(angleX);
                rot[i][0]=x1; rot[i][1]=y1; rot[i][2]=z2;
            }

            double target = 0.42 * Math.min(panelW, panelH);
            double maxXY = 1.0;
            for(double[] p : rot){
                double m = Math.max(Math.abs(p[0]), Math.abs(p[1]));
                if(m>maxXY) maxXY = m;
            }
            double scale = target / maxXY;

            class FaceDraw {
                int[] xs, ys;
                double depth;
                Color color;
            }
            List<FaceDraw> toDraw = new ArrayList<>();

            for(int f=0; f<faces.length; f++){
                int[] ordered = orderFaceCyclic(faces[f], rot);

                FaceDraw fd = new FaceDraw();
                fd.xs = new int[ordered.length];
                fd.ys = new int[ordered.length];

                double sumZ = 0.0;
                for(int i=0;i<ordered.length;i++){
                    double[] p = rot[ordered[i]];
                    fd.xs[i] = cx + (int)Math.round(p[0]*scale);
                    fd.ys[i] = cy - (int)Math.round(p[1]*scale);
                    sumZ += p[2];
                }
                fd.depth = sumZ / ordered.length;
                if(faceColors != null && faceColors.length > 0) {
                    fd.color = faceColors[f % faceColors.length];
                } else {
                    fd.color = Color.LIGHT_GRAY;
                }
                toDraw.add(fd);
            }

            toDraw.sort((fd1,fd2) -> Double.compare(fd1.depth, fd2.depth));

            for(FaceDraw fd : toDraw){
                g.setColor(fd.color);
                g.fillPolygon(fd.xs, fd.ys, fd.xs.length);
                g.setColor(Color.BLACK);
                g.drawPolygon(fd.xs, fd.ys, fd.xs.length);
            }
        }

        // --- Dodecahedron (special handling to preserve pentagon shape) ---
        private void drawDodecahedron(Graphics2D g, int panelW, int panelH, int cx, int cy){
            double phi = (1 + Math.sqrt(5)) / 2.0;
            double l = getRenderVal("Length") / 2.0;
            double a = 1.0;
            double b = 1.0 / phi;
            double c = phi;
            double s = l / c;

            double[][] verts = new double[][]{
                {-a*s,-a*s,-a*s},{-a*s,-a*s, a*s},{-a*s, a*s,-a*s},{-a*s, a*s, a*s},
                { a*s,-a*s,-a*s},{ a*s,-a*s, a*s},{ a*s, a*s,-a*s},{ a*s, a*s, a*s},
                { 0,-b*s,-c*s},{ 0,-b*s, c*s},{ 0, b*s,-c*s},{ 0, b*s, c*s},
                {-b*s,-c*s, 0},{-b*s, c*s, 0},{ b*s,-c*s, 0},{ b*s, c*s, 0},
                {-c*s, 0,-b*s},{ c*s, 0,-b*s},{-c*s, 0, b*s},{ c*s, 0, b*s}
            };

            // Rotate vertices
            double[][] rot = new double[verts.length][3];
            for(int i=0;i<verts.length;i++){
                double x=verts[i][0], y=verts[i][1], z=verts[i][2];
                double x1 = x*Math.cos(angleY) - z*Math.sin(angleY);
                double z1 = x*Math.sin(angleY) + z*Math.cos(angleY);
                double y1 = y*Math.cos(angleX) - z1*Math.sin(angleX);
                double z2 = y*Math.sin(angleX) + z1*Math.cos(angleX);
                rot[i][0]=x1; rot[i][1]=y1; rot[i][2]=z2;
            }

            double target = 0.42 * Math.min(panelW, panelH);
            double maxXY = 1.0;
            for(double[] p : rot){
                double m = Math.max(Math.abs(p[0]), Math.abs(p[1]));
                if(m>maxXY) maxXY = m;
            }
            double scale = target / maxXY;
            Color[] colors = {Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.MAGENTA,Color.CYAN};

            // ✅ Corrected external pentagon faces (matches the vertex ordering above)
            int[][] faces = new int[][] {
                {0, 8, 9, 1, 12}, 
                {1, 9, 5, 19, 11}, 
                {11, 19, 17, 10, 3}, 
                {3, 10, 16, 2, 18}, 
                {2, 18, 6, 14, 4}, 
                {4, 14, 15, 5, 9}, 
                {5, 15, 7, 17, 19}, 
                {7, 13, 16, 10, 17}, 
                {7, 15, 14, 6, 13}, 
                {13, 6, 18, 2, 16}, 
                {8, 0, 12, 1, 9}, 
                {0, 8, 4, 14, 6}
            };

            class FaceDraw {
                int[] xs, ys;
                double depth;
                Color color;
            }
            List<FaceDraw> toDraw = new ArrayList<>();

            for(int f=0; f<faces.length; f++){
                int[] face = faces[f];
                int[] ordered = orderFaceCyclic(face, rot);

                FaceDraw fd = new FaceDraw();
                fd.xs = new int[ordered.length];
                fd.ys = new int[ordered.length];

                double sumZ = 0.0;
                for(int i=0;i<ordered.length;i++){
                    double[] p = rot[ordered[i]];
                    fd.xs[i] = cx + (int)Math.round(p[0]*scale);
                    fd.ys[i] = cy - (int)Math.round(p[1]*scale);
                    sumZ += p[2];
                }
                fd.depth = sumZ / ordered.length;
                fd.color = colors[f % colors.length];
                toDraw.add(fd);
            }

            toDraw.sort((fd1,fd2) -> Double.compare(fd1.depth, fd2.depth));

            for(FaceDraw fd : toDraw){
                g.setColor(fd.color);
                g.fillPolygon(fd.xs, fd.ys, fd.xs.length);
                g.setColor(Color.BLACK);
                g.drawPolygon(fd.xs, fd.ys, fd.xs.length);
            }
        }

        private int[] orderFaceCyclic(int[] faceIdx, double[][] rot){
            double cx=0, cy=0;
            for(int idx : faceIdx){
                cx += rot[idx][0];
                cy += rot[idx][1];
            }
            cx /= faceIdx.length;
            cy /= faceIdx.length;

            double[][] tmp = new double[faceIdx.length][2];
            for(int i=0;i<faceIdx.length;i++){
                int idx = faceIdx[i];
                double ax = rot[idx][0] - cx;
                double ay = rot[idx][1] - cy;
                double ang = Math.atan2(ay, ax);
                tmp[i][0] = ang;
                tmp[i][1] = idx;
            }
            Arrays.sort(tmp, Comparator.comparingDouble(angData -> angData[0]));

            int[] ordered = new int[faceIdx.length];
            for(int i=0;i<faceIdx.length;i++){
                ordered[i] = (int) tmp[i][1];
            }
            return ordered;
        }

        // --- Sphere ---
        private void drawSphere(Graphics2D g,int panelW,int panelH,int cx,int cy){
            int res=16;
            double[][] points=new double[(res+1)*(res+1)][3];
            int idx=0;
            for(int i=0;i<=res;i++){
                double theta=Math.PI*i/res;
                for(int j=0;j<=res;j++){
                    double phi=2*Math.PI*j/res;
                    double x=Math.sin(theta)*Math.cos(phi);
                    double y=Math.cos(theta);
                    double z=Math.sin(theta)*Math.sin(phi);
                    double x1 = x*Math.cos(angleY) - z*Math.sin(angleY);
                    double z1 = x*Math.sin(angleY) + z*Math.cos(angleY);
                    double y1 = y*Math.cos(angleX) - z1*Math.sin(angleX);
                    double z2 = y*Math.sin(angleX) + z1*Math.cos(angleX);
                    points[idx++]=new double[]{x1,y1,z2};
                }
            }
            double target = 0.42 * Math.min(panelW, panelH);
            double maxXY = 1.0;
            for(double[] p : points){
                double m = Math.max(Math.abs(p[0]), Math.abs(p[1]));
                if(m>maxXY) maxXY = m;
            }
            double scale = target / maxXY;
            g.setColor(Color.BLUE);
            for(double[] p:points){
                if(p[2]<0) continue;
                int px=cx+(int)Math.round(p[0]*scale);
                int py=cy-(int)Math.round(p[1]*scale);
                g.fillOval(px-2,py-2,4,4);
            }
        }

        // --- Cylinder (painter sort) ---
        private void drawCylinder(Graphics2D g,int panelW,int panelH,int cx,int cy){
            int res=24;
            double[][] top=new double[res][3];
            double[][] bottom=new double[res][3];
            for(int i=0;i<res;i++){
                double angle=2*Math.PI*i/res;
                top[i]=new double[]{Math.cos(angle), 0.5, Math.sin(angle)};
                bottom[i]=new double[]{Math.cos(angle), -0.5, Math.sin(angle)};
            }

            double[][] topR=new double[res][3], botR=new double[res][3];
            for(int i=0;i<res;i++){
                double[] t=top[i], b=bottom[i];
                double x1 = t[0]*Math.cos(angleY) - t[2]*Math.sin(angleY);
                double z1 = t[0]*Math.sin(angleY) + t[2]*Math.cos(angleY);
                double y1 = t[1]*Math.cos(angleX) - z1*Math.sin(angleX);
                double z2 = t[1]*Math.sin(angleX) + z1*Math.cos(angleX);
                topR[i]=new double[]{x1,y1,z2};
                double x1b = b[0]*Math.cos(angleY) - b[2]*Math.sin(angleY);
                double z1b = b[0]*Math.sin(angleY) + b[2]*Math.cos(angleY);
                double y1b = b[1]*Math.cos(angleX) - z1b*Math.sin(angleX);
                double z2b = b[1]*Math.sin(angleX) + z1b*Math.cos(angleX);
                botR[i]=new double[]{x1b,y1b,z2b};
            }

            double target = 0.42 * Math.min(panelW, panelH);
            double maxXY = 1.0;
            for(int i=0;i<res;i++){
                double[] a=topR[i];
                double[] b=botR[i];
                double ma=Math.max(Math.abs(a[0]), Math.abs(a[1]));
                double mb=Math.max(Math.abs(b[0]), Math.abs(b[1]));
                if(ma>maxXY) maxXY = ma;
                if(mb>maxXY) maxXY = mb;
            }
            double scale = target / maxXY;

            class Quad { int[] xs, ys; double depth; }
            List<Quad> quads = new ArrayList<>();
            for(int i=0;i<res;i++){
                int ni=(i+1)%res;
                double[] p1=topR[i], p2=topR[ni], p3=botR[ni], p4=botR[i];
                int[] xs=new int[4], ys=new int[4];
                double depth=(p1[2]+p2[2]+p3[2]+p4[2])/4.0;
                double[][] q={p1,p2,p3,p4};
                for(int j=0;j<4;j++){
                    xs[j]=cx+(int)Math.round(q[j][0]*scale);
                    ys[j]=cy-(int)Math.round(q[j][1]*scale);
                }
                Quad qd=new Quad(); qd.xs=xs; qd.ys=ys; qd.depth=depth;
                quads.add(qd);
            }
            quads.sort((q1,q2)->Double.compare(q1.depth,q2.depth));
            for(Quad q:quads){
                g.setColor(Color.CYAN);
                g.fillPolygon(q.xs,q.ys,4);
                g.setColor(Color.BLACK);
                g.drawPolygon(q.xs,q.ys,4);
            }

            class Ring { int[] xs, ys; double depth; Color color; }
            List<Ring> rings=new ArrayList<>();
            for(double[][] circle:new double[][][]{topR,botR}){
                int[] xs=new int[res];
                int[] ys=new int[res];
                double sumZ=0;
                for(int i=0;i<res;i++){
                    xs[i]=cx+(int)Math.round(circle[i][0]*scale);
                    ys[i]=cy-(int)Math.round(circle[i][1]*scale);
                    sumZ+=circle[i][2];
                }
                Ring rg=new Ring();
                rg.xs=xs; rg.ys=ys; rg.depth=sumZ/res; rg.color=Color.ORANGE;
                rings.add(rg);
            }
            rings.sort((r1,r2)->Double.compare(r1.depth,r2.depth));
            for(Ring rg:rings){
                g.setColor(rg.color);
                g.fillPolygon(rg.xs,rg.ys,res);
                g.setColor(Color.BLACK);
                g.drawPolygon(rg.xs,rg.ys,res);
            }
        }

        // --- Polyhedron vertices & faces ---
        private double[][] getCubeVertices(){ double l=getRenderVal("Length")/2; return new double[][]{{-l,-l,-l},{l,-l,-l},{l,l,-l},{-l,l,-l},{-l,-l,l},{l,-l,l},{l,l,l},{-l,l,l}}; }
        private int[][] getCubeFaces(){ return new int[][]{{0,1,2,3},{4,5,6,7},{0,1,5,4},{2,3,7,6},{0,3,7,4},{1,2,6,5}}; }
        private double[][] getTetraVertices(){ double l=getRenderVal("Length"); double h=Math.sqrt((2.0/3.0)*l*l); return new double[][]{{0,0,0},{l,0,0},{l/2,h,0},{l/2,h/Math.sqrt(2.0),Math.sqrt(2.0)/2.0*l}}; }
        private int[][] getTetraFaces(){ return new int[][]{{0,1,2},{0,1,3},{1,2,3},{2,0,3}}; }
        private double[][] getOctaVertices(){ double l=getRenderVal("Length")/2; return new double[][]{{l,0,0},{-l,0,0},{0,l,0},{0,-l,0},{0,0,l},{0,0,-l}}; }
        private int[][] getOctaFaces(){ return new int[][]{{0,2,4},{2,1,4},{1,3,4},{3,0,4},{0,2,5},{2,1,5},{1,3,5},{3,0,5}}; }
        private double[][] getIcosaVertices(){ double l=getRenderVal("Length")/2; double phi=(1+Math.sqrt(5))/2; return new double[][]{
            {-l,phi*l,0},{l,phi*l,0},{-l,-phi*l,0},{l,-phi*l,0},{0,-l,phi*l},{0,l,phi*l},{0,-l,-phi*l},{0,l,-phi*l},{phi*l,0,-l},{phi*l,0,l},{-phi*l,0,-l},{-phi*l,0,l}}; }
        private int[][] getIcosaFaces(){ return new int[][]{{0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},{1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},{3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},{4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}}; }

    }
}
