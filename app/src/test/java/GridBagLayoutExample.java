import javax.swing.*;
import java.awt.*;

public class GridBagLayoutExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("GridBagLayout Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        JPanel panel = new JPanel(new GridBagLayout());

        JButton button1 = new JButton("Button 1 (2 columns)");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0; // Distribute space among components
        panel.add(button1, gridBagConstraints);


        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        JButton button2 = new JButton("Button 2 (1 column)");
        gridBagConstraints1.weightx = 1.0;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        panel.add(button2, gridBagConstraints1);


        frame.add(panel);
        frame.setVisible(true);
    }
}