
import me.gegenbauer.catspy.ui.card.RoundedCard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RoundedCardContainerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 创建一个 JFrame
            JFrame frame = new JFrame("Rounded Card Container Example");
            JPanel panel = new JPanel();

            // 设置关闭操作
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RoundedCard cardContainer = new RoundedCard(20, Color.white, new FlowLayout(), 4);
            // 创建几个控件并添加到容器中
            //cardContainer.add(new JButton("Button"));

            // 将卡片容器添加到 JFrame
            panel.add(cardContainer);
            frame.add(panel);

            // 设置 JFrame 大小
            frame.setSize(400, 300);

            // 设置 JFrame 在屏幕中心显示
            frame.setLocationRelativeTo(null);

            // 显示 JFrame
            frame.setVisible(true);
        });
    }
}