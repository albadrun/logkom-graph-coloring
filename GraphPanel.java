import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;

import javax.swing.*;
import javax.swing.event.*;

/**
 * @author Ragil, Tirta, Usama Modified from John B. Matthews code (distribution
 *         per GPL). https://sites.google.com/site/drjohnbmatthews/graphpanel
 */
public class GraphPanel extends JComponent {

    private static final int WIDE = 640;
    private static final int HIGH = 480;
    private static final int RADIUS = 25;
    private static final int NUM_COLOR = 3;
    private static final Random rnd = new Random();


    private ControlPanel control = new ControlPanel();
    private int radius = RADIUS;
    private Kind kind = Kind.Circular;
    private int numColor = NUM_COLOR;
    private List<Node> nodes = new ArrayList<Node>();
    private List<Node> selected = new ArrayList<Node>();
    private List<Edge> edges = new ArrayList<Edge>();
    private Point mousePt = new Point(WIDE / 2, HIGH / 2);
    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("GraphPanel");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                GraphPanel gp = new GraphPanel();
                f.add(gp.control, BorderLayout.NORTH);
                f.add(new JScrollPane(gp), BorderLayout.CENTER);
                f.getRootPane().setDefaultButton(gp.control.defaultButton);
                f.pack();
                f.setLocationByPlatform(true);
                f.setVisible(true);
            }
        });
    }

    public GraphPanel() {
        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (Edge e : edges) {
            e.draw(g);
        }
        for (Node n : nodes) {
            n.draw(g);
        }
        if (selecting) {
            g.setColor(Color.darkGray);
            g.drawRect(mouseRect.x, mouseRect.y, mouseRect.width, mouseRect.height);
        }
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            selecting = false;
            mouseRect.setBounds(0, 0, 0, 0);
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
            e.getComponent().repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
            if (e.isShiftDown()) {
                Node.selectToggle(nodes, mousePt);
            } else if (e.isPopupTrigger()) {
                Node.selectOne(nodes, mousePt);
                showPopup(e);
            } else if (Node.selectOne(nodes, mousePt)) {
                selecting = false;
            } else {
                Node.selectNone(nodes);
                selecting = true;
            }
            e.getComponent().repaint();
        }

        private void showPopup(MouseEvent e) {
            control.popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {

        Point delta = new Point();

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selecting) {
                mouseRect.setBounds(Math.min(mousePt.x, e.getX()), Math.min(mousePt.y, e.getY()),
                        Math.abs(mousePt.x - e.getX()), Math.abs(mousePt.y - e.getY()));
                Node.selectRect(nodes, mouseRect);
            } else {
                delta.setLocation(e.getX() - mousePt.x, e.getY() - mousePt.y);
                Node.updatePosition(nodes, delta);
                mousePt = e.getPoint();
            }
            e.getComponent().repaint();
        }
    }

    public JToolBar getControlPanel() {
        return control;
    }

    private class ControlPanel extends JToolBar {

        private Action newNode = new NewNodeAction("New");
        private Action clearAll = new ClearAction("Clear");
        private Action kind = new KindComboAction("Kind");
        private Action color = new ColorAction("Color");
        private Action connect = new ConnectAction("Connect");
        private Action delete = new DeleteAction("Delete");
        private Action random = new RandomAction("Random");
        private Action run = new RunAction("Run");
        private Action help = new HelpAction("Help");
        private JButton defaultButton = new JButton(newNode);
        private JComboBox kindCombo = new JComboBox();
        private ColorIcon hueIcon = new ColorIcon(Node.NONE_COLOR);
        private JPopupMenu popup = new JPopupMenu();

        /*
         * Control Panel constructor. Unused feature is commented.
         */
        ControlPanel() {
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            this.setBackground(Color.lightGray);

            this.add(defaultButton);
            this.add(new JButton(clearAll));

            // this.add(kindCombo);

            this.add(new JButton(color));
            this.add(new JLabel(hueIcon));

            // JSpinner js = new JSpinner();
            // js.setModel(new SpinnerNumberModel(RADIUS, 5, 100, 5));
            // js.addChangeListener(new ChangeListener() {

            // @Override
            // public void stateChanged(ChangeEvent e) {
            // JSpinner s = (JSpinner) e.getSource();
            // radius = (Integer) s.getValue();
            // Node.updateRadius(nodes, radius);
            // GraphPanel.this.repaint();
            // }
            // });
            // this.add(new JLabel("Size:"));
            // this.add(js);

            JSpinner js = new JSpinner();
            js.setModel(new SpinnerNumberModel(NUM_COLOR, 1, 8, 1));
            js.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSpinner s = (JSpinner) e.getSource();
                    numColor = (Integer) s.getValue();
                    for (int i = 0; i < nodes.size(); i++) {
                        Node node = nodes.get(i);
                        int pos = Arrays.asList(Node.POSSIBLE_COLORS).indexOf(node.getColor());

                        // System.out.println(pos);

                        if (pos >= numColor) {
                            node.setColor(null);
                        }
                    }

                    int huePos = Arrays.asList(Node.POSSIBLE_COLORS).indexOf(control.hueIcon.getColor());

                    if (huePos >= numColor) {
                        control.hueIcon.setColor(Node.POSSIBLE_COLORS[numColor - 1]);
                        control.repaint();
                    }

                    GraphPanel.this.repaint();
                }
            });
            this.add(new JLabel("Num Color:"));
            this.add(js);

            this.add(new JButton(random));
            this.add(new JButton(run));
            this.add(new JButton(help));

            popup.add(new JMenuItem(newNode));
            popup.add(new JMenuItem(color));
            popup.add(new JMenuItem(connect));
            popup.add(new JMenuItem(delete));

            // JMenu subMenu = new JMenu("Kind");
            // for (Kind k : Kind.values()) {
            // kindCombo.addItem(k);
            // subMenu.add(new JMenuItem(new KindItemAction(k)));
            // }
            // popup.add(subMenu);
            // kindCombo.addActionListener(kind);
        }

        class KindItemAction extends AbstractAction {

            private Kind k;

            public KindItemAction(Kind k) {
                super(k.toString());
                this.k = k;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                kindCombo.setSelectedItem(k);
            }
        }
    }

    private class ClearAction extends AbstractAction {

        public ClearAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            nodes.clear();
            edges.clear();
            Node.indexCounter = 1;
            repaint();
        }
    }

    private class ColorAction extends AbstractAction {

        public ColorAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Color color = control.hueIcon.getColor();

            String colorString = (String) JOptionPane.showInputDialog(
                GraphPanel.this,
                "Choose one color",
                "Input",
                JOptionPane.INFORMATION_MESSAGE,
                null,
                Arrays.copyOfRange(Node.POSSIBLE_COLORS_STRING, 0, numColor + 1),
                Node.POSSIBLE_COLORS_STRING[0]
            );

            color = Node.stringToColor(colorString);
            Node.updateColor(nodes, color);
            if (color != null) {
                control.hueIcon.setColor(color);
            } else {
                control.hueIcon.setColor(Node.NONE_COLOR);
            }
            control.repaint();
            repaint();
        }
    }

    private class ConnectAction extends AbstractAction {

        public ConnectAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Node.getSelected(nodes, selected);
            if (selected.size() > 1) {
                for (int i = 0; i < selected.size() - 1; ++i) {
                    Node n1 = selected.get(i);
                    Node n2 = selected.get(i + 1);

                    if (newEdge(n1, n2)) {
                        edges.add(new Edge(n1, n2));
                    }
                }
            }
            repaint();
        }

        private boolean newEdge(Node n1, Node n2) {
            for (Edge e : edges) {
                if (e.n1.index == n1.index && e.n2.index == n2.index) {
                    return false;
                } else if (e.n1.index == n2.index && e.n2.index == n1.index) {
                    return false;
                }
            }

            return true;
        }
    }

    private class DeleteAction extends AbstractAction {

        public DeleteAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            ListIterator<Node> iter = nodes.listIterator();
            while (iter.hasNext()) {
                Node n = iter.next();
                if (n.isSelected()) {
                    deleteEdges(n);
                    iter.remove();
                }
            }
            repaint();
        }

        private void deleteEdges(Node n) {
            ListIterator<Edge> iter = edges.listIterator();
            while (iter.hasNext()) {
                Edge e = iter.next();
                if (e.n1 == n || e.n2 == n) {
                    iter.remove();
                }
            }
        }
    }

    private class KindComboAction extends AbstractAction {

        public KindComboAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JComboBox combo = (JComboBox) e.getSource();
            kind = (Kind) combo.getSelectedItem();
            Node.updateKind(nodes, kind);
            repaint();
        }
    }

    private class NewNodeAction extends AbstractAction {

        public NewNodeAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Node.selectNone(nodes);
            Point p = mousePt.getLocation();
            Color color = control.hueIcon.getColor();
            Node n;
            if (!color.equals(Node.NONE_COLOR)) {
                n = new Node(p, radius, color, kind);
            } else {
                n = new Node(p, radius, kind);
            }
            n.setSelected(true);
            nodes.add(n);
            repaint();
        }
    }

    private class RandomAction extends AbstractAction {

        public RandomAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            int total = 8;
            int lastIndex = nodes.size();

            for (int i = 0; i < total; i++) {
                Point p = new Point(rnd.nextInt(getWidth()), rnd.nextInt(getHeight()));
                nodes.add(new Node(p, radius, kind));
            }
            int maxEdges = (total * (total - 1)) / 2;
            int numEdges = rnd.nextInt(maxEdges) + 1;

            int cnt = 0;
            while (cnt < numEdges) {
                Node n1 = nodes.get(rnd.nextInt(8) + lastIndex);
                Node n2 = nodes.get(rnd.nextInt(8) + lastIndex);

                if (n1.index == n2.index) {
                    continue;
                }

                boolean isExists = false;
                for (int j = 0; j < edges.size(); j++) {
                    Node fNode = edges.get(j).getFirstNode();
                    Node sNode = edges.get(j).getSecondNode();

                    if ((fNode.index == n1.index && sNode.index == n2.index)
                            || (fNode.index == n2.index && sNode.index == n1.index)) {
                        isExists = true;
                        break;
                    }
                }

                if (!isExists) {
                    edges.add(new Edge(n1, n2));
                    cnt++;
                }
            }
            repaint();
        }
    }

    private class RunAction extends AbstractAction {
        private StringBuilder strBuilder;
        private int clauses;
        private int literals;

        public RunAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            boolean check = CheckMiniSat();
            if (!check) {
                String msg = "You don't have minisat :(\nPlease install minisat";
                JOptionPane.showMessageDialog(GraphPanel.this, msg);

                return;
            }

            strBuilder = new StringBuilder();

            if (nodes.size() == 0) {
                return;
            }

            literals = (nodes.get(nodes.size() - 1).index - 1) * numColor + numColor;
            clauses = 0;

            try {
                if (SATSolverHelper()) {
                    repaint();
                } else {
                    String msg = "Graph can't be colored :(";
                    JOptionPane.showMessageDialog(GraphPanel.this, msg);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        private boolean SATSolverHelper() throws IOException {
            adjacentNodesHandler();
            nodeHasColorHandler();
            nodeHasOneColorHandler();
            coloredNodesHandler();

            strBuilder.insert(0, String.format("p cnf %d %d\n", literals, clauses));

            FileWriter writer = new FileWriter("in.txt");
            writer.write(strBuilder.toString());
            writer.close();

            ExecuteMiniSat();

            File file = new File("out.txt");
            Scanner reader = new Scanner(file);

            String answers = "";

            while (reader.hasNextLine()) {
                answers = reader.nextLine();
                // System.out.println(answers);

                if (answers.equals("UNSAT")) {
                    reader.close();
                    return false;
                }
            }

            reader.close();

            String[] arrOfLiterals = answers.split(" ");
            ArrayList<Integer> colors = new ArrayList<>();

            for (String literal : arrOfLiterals) {
                int lit = Integer.parseInt(literal);

                if (lit > 0) {
                    colors.add(lit);
                    // System.out.println(">>" + lit);
                }
            }

            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                int number = colors.get(i);
                int colorIndex = number % numColor;

                if (colorIndex == 0) {
                    colorIndex = numColor;
                }

                String colorString = Node.POSSIBLE_COLORS_STRING[colorIndex];
                // System.out.println(colorString);
                Color color = Node.stringToColor(colorString);
                Node.updateColor(node, color);
            }

            return true;
        }

        private boolean CheckMiniSat() {
            try {
                String[] args = new String[] {"minisat", "-h"};
                Process proc = new ProcessBuilder(args).start();
                proc.waitFor();
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        private void adjacentNodesHandler() {
            // System.out.println(numColor);
            // System.out.println(edges.size());
            for (Edge edge : edges) {
                int index1 = (edge.n1.index - 1) * numColor;
                int index2 = (edge.n2.index - 1) * numColor;

                for (int i = 1; i <= numColor; i++) {
                    strBuilder.append(-(index1 + i) + " " + -(index2 + i) + " 0\n");
                    clauses++;
                }
            }
        }

        private void nodeHasColorHandler() {
            for (Node node : nodes) {
                int index = (node.index - 1) * numColor;

                for (int i = 1; i <= numColor; i++) {
                    strBuilder.append((index + i) + " ");
                }

                strBuilder.append("0\n");
                clauses++;
            }
        }

        private void nodeHasOneColorHandler() {
            for (Node node : nodes) {
                int index = (node.index - 1) * numColor;
                for (int i = 1; i < numColor; i++) {
                    for (int j = i + 1; j <= numColor; j++) {
                        strBuilder.append(-(index + i) + " " + -(index + j) + " 0\n");
                        clauses++;
                    }
                }
            }
        }

        private void coloredNodesHandler() {
            for (Node node : nodes) {
                for (int i = 0; i < numColor; i++) {
                    if (node.color == Node.POSSIBLE_COLORS[i]) {
                        // System.out.println(node.index + " = " + i + " " + ((node.index - 1) * numColor + (i + 1)));
                        strBuilder.append(((node.index - 1) * numColor + (i + 1)) + " 0\n");
                        clauses++;
                    }
                }
            }
        }

        private void ExecuteMiniSat() {
            try {
                String[] args = new String[] {"minisat", "in.txt", "out.txt"};
                Process proc = new ProcessBuilder(args).start();
                proc.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class HelpAction extends AbstractAction {

        public HelpAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            String msg = "Hello friends.\nPress shift to select multiple objects. Right click and connect.";
            JOptionPane.showMessageDialog(GraphPanel.this, msg);
        }
    }

    /**
     * The kinds of node in a graph.
     */
    private enum Kind {

        Circular, Rounded, Square;
    }

    /**
     * An Edge is a pair of Nodes.
     */
    private static class Edge {

        private Node n1;
        private Node n2;

        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
        }

        public void draw(Graphics g) {
            Point p1 = n1.getLocation();
            Point p2 = n2.getLocation();
            g.setColor(Color.darkGray);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        public Node getFirstNode() {
            return n1;
        }

        public Node getSecondNode() {
            return n2;
        }
    }

    /**
     * A Node represents a node in a graph.
     */
    private static class Node {

        private Point p;
        private int r;
        private Color color;
        private Kind kind;
        private boolean selected = false;
        private Rectangle b = new Rectangle();
        private int index;

        private static int indexCounter = 1;

        private static final String K_NONE = "None";
        private static final String K_RED = "Red";
        private static final String K_GREEN = "Green";
        private static final String K_BLUE = "Blue";
        private static final String K_YELLOW = "Yellow";
        private static final String K_GRAY = "Gray";
        private static final String K_PINK = "Pink";
        private static final String K_CYAN = "Cyan";  // Kacian :(
        private static final String K_MAGENTA = "Magenta";

        private static final int STROKE_SIZE = 3;
        private static final int SELECTED_STROKE_SIZE = 1;
        private static final Color OUTLINE_COLOR = Color.black;

        private static final Color NONE_COLOR = Color.white;

        public static final String POSSIBLE_COLORS_STRING[] = {
            Node.K_NONE, Node.K_RED, Node.K_GREEN, Node.K_BLUE,
            Node.K_YELLOW, Node.K_GRAY, Node.K_PINK, Node.K_CYAN, Node.K_MAGENTA
        };
        private static final Color POSSIBLE_COLORS[] = {
            Color.red, Color.green, Color.blue, Color.yellow,
            Color.gray, Color.pink, Color.cyan, Color.magenta
        };

        /**
         * Construct a new node with color.
         */
        public Node(Point p, int r, Color color, Kind kind) {
            this.p = p;
            this.r = r;
            this.color = color;
            this.kind = kind;
            this.index = Node.indexCounter++;
            setBoundary(b);
        }

        /**
         * Construct a new node without color.
         */
        public Node(Point p, int r, Kind kind) {
            this.p = p;
            this.r = r;
            this.kind = kind;
            this.color = null;
            this.index = Node.indexCounter++;
            setBoundary(b);
        }

        /**
         * Calculate this node's rectangular boundary.
         */
        private void setBoundary(Rectangle b) {
            b.setBounds(p.x - r, p.y - r, 2 * r, 2 * r);
        }

        /**
         * Draw this node.
         */
        public void draw(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(Node.STROKE_SIZE));

            if (this.color == null) {
                g2.setColor(Node.OUTLINE_COLOR);
                if (this.kind == Kind.Circular) {
                    g2.drawOval(b.x, b.y, b.width, b.height);
                } else if (this.kind == Kind.Rounded) {
                    g2.drawRoundRect(b.x, b.y, b.width, b.height, r, r);
                } else if (this.kind == Kind.Square) {
                    g2.drawRect(b.x, b.y, b.width, b.height);
                }
            } else {
                g2.setColor(this.color);
                if (this.kind == Kind.Circular) {
                    g2.fillOval(b.x, b.y, b.width, b.height);
                } else if (this.kind == Kind.Rounded) {
                    g2.fillRoundRect(b.x, b.y, b.width, b.height, r, r);
                } else if (this.kind == Kind.Square) {
                    g2.fillRect(b.x, b.y, b.width, b.height);
                }
            }

            g2.setStroke(new BasicStroke(Node.SELECTED_STROKE_SIZE));
            if (selected) {
                g2.setColor(Color.darkGray);
                g2.drawRect(b.x, b.y, b.width, b.height);
            }
        }

        /**
         * Return this node's location.
         */
        public Point getLocation() {
            return p;
        }

        public Color getColor() {
            return this.color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        /**
         * Return true if this node contains p.
         */
        public boolean contains(Point p) {
            return b.contains(p);
        }

        /**
         * Return true if this node is selected.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Mark this node as selected.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        /**
         * Collected all the selected nodes in list.
         */
        public static void getSelected(List<Node> list, List<Node> selected) {
            selected.clear();
            for (Node n : list) {
                if (n.isSelected()) {
                    selected.add(n);
                }
            }
        }

        /**
         * Select no nodes.
         */
        public static void selectNone(List<Node> list) {
            for (Node n : list) {
                n.setSelected(false);
            }
        }

        /**
         * Select a single node; return true if not already selected.
         */
        public static boolean selectOne(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    if (!n.isSelected()) {
                        Node.selectNone(list);
                        n.setSelected(true);
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Select each node in r.
         */
        public static void selectRect(List<Node> list, Rectangle r) {
            for (Node n : list) {
                n.setSelected(r.contains(n.p));
            }
        }

        /**
         * Toggle selected state of each node containing p.
         */
        public static void selectToggle(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    n.setSelected(!n.isSelected());
                }
            }
        }

        /**
         * Update each node's position by d (delta).
         */
        public static void updatePosition(List<Node> list, Point d) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.p.x += d.x;
                    n.p.y += d.y;
                    n.setBoundary(n.b);
                }
            }
        }

        /**
         * Update each node's radius r.
         */
        public static void updateRadius(List<Node> list, int r) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.r = r;
                    n.setBoundary(n.b);
                }
            }
        }

        /**
         * Update each node's color.
         */
        public static void updateColor(List<Node> list, Color color) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.color = color;
                }
            }
        }

        /**
         * Update node's color
         */
        public static void updateColor(Node node, Color color) {
            node.color = color;
        }

        /**
         * Update each node's kind.
         */
        public static void updateKind(List<Node> list, Kind kind) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.kind = kind;
                }
            }
        }

        public static Color stringToColor(String colorString) {
            if (colorString == null) {
                return null;
            }

            int pos = Arrays.asList(Node.POSSIBLE_COLORS_STRING).indexOf(colorString);
            if (pos > 0) {
                return Node.POSSIBLE_COLORS[pos - 1];
            }

            return null;
        }

        public int getIndex() {
            return this.index;
        }
    }

    private static class ColorIcon implements Icon {

        private static final int WIDE = 20;
        private static final int HIGH = 20;
        private Color color;

        public ColorIcon(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, WIDE, HIGH);
        }

        public int getIconWidth() {
            return WIDE;
        }

        public int getIconHeight() {
            return HIGH;
        }
    }
}