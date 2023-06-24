import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DraggableCardContainerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Draggable Card Container");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GridLayer gridLayer = new GridLayer();
            DragLayer dragLayer = new DragLayer(gridLayer);
            frame.setContentPane(dragLayer);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class GridLayer extends JPanel {
        private final int rows = 3;
        private final int columns = 3;
        private final Dimension cardSize = new Dimension(100, 50);

        public GridLayer() {
            setLayout(new GridLayout(rows, columns));
            for (int i = 1; i <= rows * columns; i++) {
                add(createCard("Card " + i));
            }
        }

        private JPanel createCard(String text) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            card.setPreferredSize(cardSize);
            card.setBackground(Color.LIGHT_GRAY);
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            card.add(label, BorderLayout.CENTER);
            return card;
        }
    }

    static class DragLayer extends JLayeredPane {
        private final GridLayer gridLayer;
        private final int offsetX = 20, offsetY = 20, rows = 3, columns = 3;
        private JPanel draggedCard, placeholderCard;
        private Point initialLocation, dragPoint;

        public DragLayer(GridLayer gridLayer) {
            this.gridLayer = gridLayer;
            setPreferredSize(gridLayer.getPreferredSize());
            gridLayer.setBounds(0, 0, gridLayer.getPreferredSize().width, gridLayer.getPreferredSize().height);
            setLayout(null);
            add(gridLayer, JLayeredPane.DEFAULT_LAYER);
            addMouseListeners();
        }

        private void addMouseListeners() {
            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragPoint = gridLayer.getMousePosition();
                    if (dragPoint == null) return;
                    int index = pointToIndex(dragPoint, gridLayer.getWidth() / columns, gridLayer.getHeight() / rows);
                    JPanel card = (JPanel) gridLayer.getComponent(index);
                    if (draggedCard == null) {
                        draggedCard = createDraggedCard(card);
                        placeholderCard = createPlaceholderCard();
                        initialLocation = card.getComponentAt(dragPoint).getLocation();
                        initialLocation = SwingUtilities.convertPoint(card, initialLocation, DragLayer.this);
                        gridLayer.add(placeholderCard, index);
                        gridLayer.remove(card);
                        add(draggedCard, JLayeredPane.DRAG_LAYER);
                    }
                    draggedCard.setLocation(initialLocation.x + offsetX, initialLocation.y + offsetY);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedCard == null || dragPoint == null) return;

                    int index = pointToIndex(e.getPoint(), gridLayer.getWidth() / columns, gridLayer.getHeight() / rows);
                    gridLayer.add(draggedCard, index);
                    gridLayer.remove(placeholderCard);

                    draggedCard.setBounds(placeholderCard.getBounds());

                    remove(draggedCard);
                    draggedCard = null;
                    placeholderCard = null;
                    initialLocation = null;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedCard == null || dragPoint == null) return;

                    int deltaX = e.getX() - dragPoint.x;
                    int deltaY = e.getY() - dragPoint.y;

                    draggedCard.setLocation(initialLocation.x + deltaX, initialLocation.y + deltaY);

                    int index = pointToIndex(dragPoint, gridLayer.getWidth() / columns, gridLayer.getHeight() / rows);
                    if (index >= 0 && index <= rows * columns) {
                        int draggedIndex = pointToIndex(dragPoint, gridLayer.getWidth() / columns, gridLayer.getHeight() / rows);
                        if (draggedIndex != index) {
                            gridLayer.add(placeholderCard, index);
                            gridLayer.add(new JPanel(), draggedIndex);
                            SwingUtilities.invokeLater(() -> gridLayer.repaint());
                        }
                    }
                }

                private int pointToIndex(Point point, int cellWidth, int cellHeight) {
                    int row = point.y / cellHeight;
                    int column = point.x / cellWidth;
                    return row * columns + column;
                }
            };
            gridLayer.addMouseListener(adapter);
            gridLayer.addMouseMotionListener(adapter);
        }

        private JPanel createDraggedCard(JPanel originalCard) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            card.setPreferredSize(originalCard.getPreferredSize());
            card.setBackground(Color.LIGHT_GRAY);
            JLabel label = new JLabel(((JLabel) ((BorderLayout) originalCard.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getText(), SwingConstants.CENTER);
            card.add(label, BorderLayout.CENTER);
            return card;
        }

        private JPanel createPlaceholderCard() {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            card.setPreferredSize(gridLayer.getComponent(0).getPreferredSize());
            card.setBackground(new Color(0, 0, 0, 50));
            card.setOpaque(true);
            return card;
        }
    }
}