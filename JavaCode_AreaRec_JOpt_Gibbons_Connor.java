/* Rectangle area calculator with added circle area option
 * Connor T. Gibbons
 * Created 01SEP2025
 * CSC-151-0901
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */





import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class JavaCode_AreaRec_JOpt_Gibbons_Connor extends JFrame {

    private JTextField lengthField, widthField, radiusField;
    private JButton calculateButton, cancelButton;
    private JLabel resultLabel;

    // Added for units feature
    private JPanel unitPanel;
    private JComboBox<String> unitComboBox;
    private JTextField customUnitField;

    public JavaCode_AreaRec_JOpt_Gibbons_Connor() {
        Menu();
    }

    private void Menu() {
        getContentPane().removeAll();
        revalidate();
        repaint();

        setTitle("Menu");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 1, 10, 10));

        JButton rectButton = new JButton("Rectangle Area Calculator");
        JButton circleButton = new JButton("Circle Area Calculator");
        JButton exitButton = new JButton("Exit");

        rectButton.addActionListener(e -> RectangleAreaCalculator());
        circleButton.addActionListener(e -> CircleAreaCalculator());
        exitButton.addActionListener(e -> System.exit(0));

        add(rectButton);
        add(circleButton);
        add(exitButton);

        setVisible(true);
    }

    private JPanel createUnitSelector() {
        unitPanel = new JPanel(new FlowLayout());
        unitComboBox = new JComboBox<>(new String[]{"cm", "m", "in", "ft", "Custom"});
        customUnitField = new JTextField(10);
        customUnitField.setEnabled(false);

        unitComboBox.addActionListener(e -> {
            if ("Custom".equals(unitComboBox.getSelectedItem())) {
                customUnitField.setEnabled(true);
            } else {
                customUnitField.setEnabled(false);
            }
        });

        unitPanel.add(new JLabel("Units:"));
        unitPanel.add(unitComboBox);
        unitPanel.add(customUnitField);

        return unitPanel;
    }

    private String getSelectedUnit() {
        if ("Custom".equals(unitComboBox.getSelectedItem())) {
            String custom = customUnitField.getText().trim();
            return custom.isEmpty() ? "units" : custom;
        } else {
            return (String) unitComboBox.getSelectedItem();
        }
    }

    private void RectangleAreaCalculator() {
        getContentPane().removeAll();
        revalidate();
        repaint();

        setTitle("Rectangle Area Calculator");
        setLayout(new GridLayout(5, 2, 10, 10));

        add(new JLabel("Length:"));
        lengthField = new JTextField();
        add(lengthField);

        add(new JLabel("Width:"));
        widthField = new JTextField();
        add(widthField);

        add(createUnitSelector());

        calculateButton = new JButton("Calculate");
        cancelButton = new JButton("Cancel");
        resultLabel = new JLabel("");

        calculateButton.addActionListener(e -> calculateArea());
        cancelButton.addActionListener(e -> Menu());

        add(calculateButton);
        add(cancelButton);
        add(resultLabel);

        setVisible(true);
    }

    private void CircleAreaCalculator() {
        getContentPane().removeAll();
        revalidate();
        repaint();

        setTitle("Circle Area Calculator");
        setLayout(new GridLayout(5, 2, 10, 10));

        add(new JLabel("Radius:"));
        radiusField = new JTextField();
        add(radiusField);

        add(createUnitSelector());

        calculateButton = new JButton("Calculate");
        cancelButton = new JButton("Cancel");
        resultLabel = new JLabel("");

        calculateButton.addActionListener(e -> calculateCircleArea());
        cancelButton.addActionListener(e -> Menu());

        add(calculateButton);
        add(cancelButton);
        add(resultLabel);

        setVisible(true);
    }

    private void calculateArea() {
        try {
            double length = Double.parseDouble(lengthField.getText());
            double width = Double.parseDouble(widthField.getText());
            if (length < 0 || width < 0){
                JOptionPane.showMessageDialog(this, "Length and width must be non-negative.");
                return;
            }
            double area = length * width;

            DecimalFormat df = new DecimalFormat("0.000");
            resultLabel.setText("Area: " + df.format(area) + " " + getSelectedUnit() + "²");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
        }
    }

    private void calculateCircleArea() {
        try {
            double radius = Double.parseDouble(radiusField.getText());
            if (radius < 0) {
                JOptionPane.showMessageDialog(this, "Radius must be non-negative.");
            }
            double area = Math.PI * radius * radius;

            DecimalFormat df = new DecimalFormat("0.000");
            resultLabel.setText("Area: " + df.format(area) + " " + getSelectedUnit() + "²");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    public static void main(String[] args) {
        new JavaCode_AreaRec_JOpt_Gibbons_Connor();
    }
}
