package cn.exercise.algs4.datastructure.twothreefourtree;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 左倾红黑树(RedBlackBST)旋转与颜色变化可视化(Swing)。
 *
 * 给 {@link RedBlackBST} 加了默认关闭的操作跟踪(putTraced / deleteTraced):
 * 每做一次旋转/颜色翻转/删除/替换,就记一帧树快照 + 中文说明 + 高亮。
 *
 * 绘制要点:
 *  - 节点本身染红/黑(红链接的「颜色」直接画在节点上);
 *  - 父到子的连线按子节点颜色染红/黑——红链接一眼可见;
 *  - 旋转(rotateLeft/Right)与颜色翻转(flipColors)各占「变化前 + 变化后」两帧,高亮相关节点。
 */
public class RedBlackBSTVisualizer extends JFrame {

    // ---------- 红/黑与高亮配色 ----------
    private static final Color COLOR_RED_NODE    = new Color(214, 64, 64);
    private static final Color COLOR_BLACK_NODE  = new Color(45, 45, 45);
    private static final Color COLOR_BG          = new Color(250, 250, 252);
    private static final Color COLOR_EDGE_RED    = new Color(224, 52, 52);
    private static final Color COLOR_EDGE_BLACK  = new Color(112, 112, 112);

    private static final Color H_ROTATE    = new Color(255, 176, 84);   // 橙:旋转涉及
    private static final Color H_FLIP      = new Color(204, 124, 222);   // 紫:颜色翻转涉及
    private static final Color H_INSERT    = new Color(150, 232, 150);   // 绿:新插入的红节点
    private static final Color H_DELETE    = new Color(250, 122, 122);   // 红:被删除节点
    private static final Color H_TARGET    = new Color(170, 212, 255);   // 蓝:当前处理节点
    private static final Color H_SUCCESSOR = new Color(255, 232, 122);   // 黄:后继替换
    private static final Color H_ROOT      = new Color(142, 226, 226);   // 青:根变色

    // ---------- 绘制常量 ----------
    private static final int NODE_W = 46, NODE_H = 34;
    private static final int H_GAP = 26, V_GAP = 48, MARGIN = 24;

    private RedBlackBST<Integer, Integer> tree = new RedBlackBST<>();

    // ---------- 控件 ----------
    private final TreePanel treePanel = new TreePanel();
    private final JTextField insertField = new JTextField(5);
    private final JTextField deleteField = new JTextField(5);
    private final JLabel treeInfo = new JLabel(" ");
    private final JLabel stepInfo = new JLabel("步骤 0 / 0", SwingConstants.CENTER);
    private final JComboBox<String> stepBox = new JComboBox<>();
    private final JTextArea descArea = new JTextArea(3, 72);
    private final JSlider speedSlider = new JSlider(150, 1500, 500);
    private final JButton firstBtn = new JButton("⏮ 初始");
    private final JButton prevBtn = new JButton("◀ 上一步");
    private final JButton nextBtn = new JButton("下一步 ▶");
    private final JButton lastBtn = new JButton("⏭ 结尾");
    private final JButton playBtn = new JButton("▶ 自动播放");
    private final JButton randomBtn = new JButton("随机树");
    private final JButton insertBtn = new JButton("插入并演示");
    private final JButton deleteBtn = new JButton("删除并演示");
    private final JButton clearBtn = new JButton("清空");
    private Timer timer;

    // ---------- 播放状态 ----------
    private RedBlackBST.NodeView<Integer> initialRoot;                 // 操作前的初始快照
    private List<RedBlackBST.TraceStep<Integer>> steps = new ArrayList<>();
    private int currentStep = -1;                                      // -1 = 初始状态
    private boolean updatingSteps;                                     // 重建步骤下拉时忽略选择事件

    public RedBlackBSTVisualizer() {
        super("左倾红黑树:旋转与颜色变化可视化");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildControlPanel(), BorderLayout.NORTH);
        treePanel.setBackground(COLOR_BG);
        treePanel.setOpaque(true);
        JScrollPane sp = new JScrollPane(treePanel);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getHorizontalScrollBar().setUnitIncrement(24);
        add(sp, BorderLayout.CENTER);
        add(buildSouthPanel(), BorderLayout.SOUTH);
        wireEvents();
        resetStepBox();
        showStep();
        pack();
        setMinimumSize(new Dimension(920, 640));
        setLocationRelativeTo(null);
    }

    // ==================== 界面构建 ====================

    private JPanel buildControlPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.add(new JLabel("插入键:"));
        p.add(insertField);
        p.add(insertBtn);
        p.add(new JLabel("  删除键:"));
        p.add(deleteField);
        p.add(deleteBtn);
        p.add(randomBtn);
        p.add(clearBtn);
        p.add(treeInfo);
        return p;
    }

    private JPanel buildSouthPanel() {
        JPanel south = new JPanel(new BorderLayout());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.add(firstBtn);
        bar.add(prevBtn);
        bar.add(nextBtn);
        bar.add(lastBtn);
        bar.add(playBtn);
        bar.add(stepInfo);
        bar.add(new JLabel("  速度"));
        bar.add(speedSlider);
        stepBox.setPreferredSize(new Dimension(430, 24));
        bar.add(stepBox);
        south.add(bar, BorderLayout.NORTH);

        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        descArea.setBackground(new Color(248, 250, 246));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "当前步骤说明", TitledBorder.LEFT, TitledBorder.TOP));
        descScroll.setPreferredSize(new Dimension(900, 82));
        south.add(descScroll, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        legend.add(legendChip(COLOR_RED_NODE, "红节点(红链接)"));
        legend.add(legendChip(COLOR_BLACK_NODE, "黑节点"));
        legend.add(legendChip(H_ROTATE, "旋转"));
        legend.add(legendChip(H_FLIP, "颜色翻转"));
        legend.add(legendChip(H_INSERT, "新插入"));
        legend.add(legendChip(H_DELETE, "被删除"));
        legend.add(legendChip(H_TARGET, "当前节点"));
        legend.add(legendChip(H_SUCCESSOR, "后继替换"));
        legend.add(legendChip(H_ROOT, "根变色"));
        south.add(legend, BorderLayout.SOUTH);
        return south;
    }

    private JPanel legendChip(Color c, String label) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel sw = new JLabel("   ");
        sw.setOpaque(true);
        sw.setBackground(c);
        sw.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        chip.add(sw);
        chip.add(new JLabel(label));
        return chip;
    }

    // ==================== 事件 ====================

    private void wireEvents() {
        insertBtn.addActionListener(e -> insertDemo());
        deleteBtn.addActionListener(e -> deleteDemo());
        randomBtn.addActionListener(e -> randomTree());
        clearBtn.addActionListener(e -> clearTree());
        insertField.addActionListener(e -> insertDemo());
        deleteField.addActionListener(e -> deleteDemo());

        firstBtn.addActionListener(e -> setCurrentStep(-1));
        prevBtn.addActionListener(e -> {
            if (currentStep > -1) setCurrentStep(currentStep - 1);
        });
        nextBtn.addActionListener(e -> {
            if (currentStep < steps.size() - 1) setCurrentStep(currentStep + 1);
        });
        lastBtn.addActionListener(e -> setCurrentStep(steps.size() - 1));
        playBtn.addActionListener(e -> togglePlay());
        speedSlider.addChangeListener(e -> {
            if (timer != null && timer.isRunning()) {
                timer.setDelay(speedSlider.getValue());
            }
        });
        // 步骤下拉选择监听只注册一次;重建条目期间由 updatingSteps 忽略
        stepBox.addActionListener(e -> {
            if (updatingSteps) {
                return;
            }
            int j = stepBox.getSelectedIndex();
            if (j >= 0) {
                setCurrentStep(j - 1);          // 第 0 项 = 初始态(-1),之后 = steps[j-1]
            }
        });
    }

    private void clearTree() {
        stopPlay();
        tree = new RedBlackBST<>();
        initialRoot = tree.snapshot();
        steps = new ArrayList<>();
        resetStepBox();
        currentStep = -1;
        treeInfo.setText("size=0, height=-1");
        showStep();
    }

    private void randomTree() {
        stopPlay();
        tree = new RedBlackBST<>();
        Random rnd = new Random();
        int count = 0;
        while (count < 50) {
            int v = rnd.nextInt(200) - 50;
            if (!tree.contains(v)) {
                tree.put(v, v);
                count++;
            }
        }
        initialRoot = tree.snapshot();
        steps = new ArrayList<>();
        resetStepBox();
        currentStep = -1;
        treeInfo.setText("size=" + tree.size() + ", height=" + tree.height());
        showStep();
    }

    private void insertDemo() {
        Integer key = parseKey(insertField);
        if (key == null) {
            return;
        }
        stopPlay();
        RedBlackBST.NodeView<Integer> before = tree.snapshot();
        List<RedBlackBST.TraceStep<Integer>> traced = tree.putTraced(key, key);
        setSteps(before, traced);
        treeInfo.setText("size=" + tree.size() + ", height=" + tree.height());
    }

    private void deleteDemo() {
        Integer key = parseKey(deleteField);
        if (key == null) {
            return;
        }
        stopPlay();
        RedBlackBST.NodeView<Integer> before = tree.snapshot();
        List<RedBlackBST.TraceStep<Integer>> traced = tree.deleteTraced(key);
        setSteps(before, traced);
        treeInfo.setText("size=" + tree.size() + ", height=" + tree.height()
                + "  (删除" + (tree.contains(key) ? "失败" : "成功") + ")");
    }

    private Integer parseKey(JTextField field) {
        String txt = field.getText().trim();
        try {
            return Integer.parseInt(txt);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入整数键,例如 15");
            return null;
        }
    }

    private void setSteps(RedBlackBST.NodeView<Integer> before, List<RedBlackBST.TraceStep<Integer>> traced) {
        stopPlay();
        playBtn.setText("▶ 自动播放");
        initialRoot = before;
        steps = traced;
        resetStepBox();
        currentStep = -1;
        showStep();
    }

    private void stopPlay() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void resetStepBox() {
        updatingSteps = true;
        stepBox.removeAllItems();
        stepBox.addItem("0: 初始状态");
        for (int i = 0; i < steps.size(); i++) {
            stepBox.addItem((i + 1) + ": " + steps.get(i).desc);
        }
        updatingSteps = false;
    }

    private void setCurrentStep(int k) {
        currentStep = k;
        showStep();
    }

    private void showStep() {
        if (currentStep == -1) {
            treePanel.setTree(initialRoot);
            treePanel.setHighlights(null);
            descArea.setText("初始状态: size = " + tree.size() + ", height = " + tree.height()
                    + "\n输入键点「插入并演示」看旋转与颜色翻转;先「随机树」造一棵树, 再删键看删除的 moveRedLeft/Right 与平衡。");
        } else {
            RedBlackBST.TraceStep<Integer> s = steps.get(currentStep);
            treePanel.setTree(s.root);
            treePanel.setHighlights(s.hl);
            descArea.setText(s.desc);
        }
        stepInfo.setText("步骤 " + (currentStep + 1) + " / " + (steps.size() + 1));
        stepBox.setSelectedIndex(currentStep + 1);
        firstBtn.setEnabled(currentStep > -1);
        prevBtn.setEnabled(currentStep > -1);
        nextBtn.setEnabled(currentStep < steps.size() - 1);
        lastBtn.setEnabled(currentStep < steps.size() - 1);
    }

    private void togglePlay() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
            playBtn.setText("▶ 自动播放");
            return;
        }
        if (currentStep >= steps.size() - 1) {
            setCurrentStep(-1);
        }
        timer = new Timer(speedSlider.getValue(), e -> {
            if (currentStep < steps.size() - 1) {
                setCurrentStep(currentStep + 1);
            } else {
                ((Timer) e.getSource()).stop();
                playBtn.setText("▶ 自动播放");
            }
        });
        playBtn.setText("⏸ 暂停");
        timer.start();
    }

    // ==================== 树绘制面板 ====================

    private static class NodePos {
        double x, y;                    // 节点中心坐标
        int depth;
    }

    class TreePanel extends JPanel {
        private RedBlackBST.NodeView<Integer> root;
        private List<RedBlackBST.Hl> highlights;
        private final Map<RedBlackBST.NodeView<Integer>, NodePos> pos = new HashMap<>();
        private Map<RedBlackBST.NodeView<Integer>, Color> nodeColors = new HashMap<>();
        private double treeWidth;
        private int maxDepth;

        void setTree(RedBlackBST.NodeView<Integer> r) {
            this.root = r;
            revalidate();
            repaint();
        }

        void setHighlights(List<RedBlackBST.Hl> hls) {
            this.highlights = hls;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (root == null) {
                return new Dimension(400, 220);
            }
            compute();
            return new Dimension((int) Math.ceil(treeWidth) + 2 * MARGIN,
                    (int) Math.ceil((maxDepth + 1) * (NODE_H + V_GAP)) + 2 * MARGIN);
        }

        private void compute() {
            pos.clear();
            maxDepth = 0;
            treeWidth = layout(root, MARGIN, 0);
        }

        /** 递归布局:节点居中于子树上方,返回子树宽度。 */
        private double layout(RedBlackBST.NodeView<Integer> n, double x, int depth) {
            if (n == null) {
                return 0;
            }
            NodePos p = new NodePos();
            p.depth = depth;
            p.y = depth * (NODE_H + V_GAP) + NODE_H / 2.0 + MARGIN;

            double leftW = n.left == null ? 0 : layout(n.left, x, depth + 1);
            double rightX = x + leftW + (n.left == null ? 0 : H_GAP);
            double rightW = n.right == null ? 0 : layout(n.right, rightX, depth + 1);

            double w;
            if (n.left == null && n.right == null) {
                w = NODE_W;
                p.x = x + NODE_W / 2.0;
            } else if (n.left == null) {
                w = rightW;
                p.x = x + rightW / 2.0;
            } else if (n.right == null) {
                w = leftW;
                p.x = x + leftW / 2.0;
            } else {
                w = leftW + H_GAP + rightW;
                p.x = x + leftW + H_GAP / 2.0;
            }
            pos.put(n, p);
            if (depth > maxDepth) {
                maxDepth = depth;
            }
            return w;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (root == null) {
                g.setColor(Color.GRAY);
                g.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
                g.drawString("(树为空)", 20, 40);
                return;
            }
            compute();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            resolveHighlights();
            double ox = (getWidth() - treeWidth) / 2.0 - MARGIN;
            g2.translate(ox, 0);
            drawNode(g2, root);
            g2.dispose();
        }

        private void resolveHighlights() {
            nodeColors = new HashMap<>();
            if (highlights == null) {
                return;
            }
            for (RedBlackBST.Hl h : highlights) {
                RedBlackBST.NodeView<Integer> nd = resolve(root, h.path);
                if (nd != null) {
                    nodeColors.put(nd, colorOf(h.kind));
                }
            }
        }

        /** 沿 path 从根走(0=左, 1=右);路径失效返回 null。 */
        private RedBlackBST.NodeView<Integer> resolve(RedBlackBST.NodeView<Integer> n, int[] path) {
            RedBlackBST.NodeView<Integer> cur = n;
            for (int idx : path) {
                if (cur == null) {
                    return null;
                }
                cur = idx == 0 ? cur.left : cur.right;
            }
            return cur;
        }

        private void drawNode(Graphics2D g, RedBlackBST.NodeView<Integer> n) {
            NodePos p = pos.get(n);
            Color fill = n.red ? COLOR_RED_NODE : COLOR_BLACK_NODE;
            Color nodeFill = nodeColors.getOrDefault(n, fill);
            Color text = textColorFor(nodeFill);

            int x0 = (int) (p.x - NODE_W / 2.0);
            int y0 = (int) (p.y - NODE_H / 2.0);

            // 先画子节点与连线
            if (n.left != null) {
                NodePos lp = pos.get(n.left);
                drawEdge(g, p, lp, n.left.red);
                drawNode(g, n.left);
            }
            if (n.right != null) {
                NodePos rp = pos.get(n.right);
                drawEdge(g, p, rp, n.right.red);
                drawNode(g, n.right);
            }

            // 节点本体(后画,盖在连线上)
            g.setColor(nodeFill);
            g.fillRoundRect(x0, y0, NODE_W, NODE_H, 10, 10);
            g.setColor(Color.DARK_GRAY);
            g.drawRoundRect(x0, y0, NODE_W, NODE_H, 10, 10);
            String s = String.valueOf(n.key);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(s);
            g.setColor(text);
            g.drawString(s, x0 + (NODE_W - tw) / 2, y0 + (NODE_H + fm.getAscent() - fm.getDescent()) / 2);
        }

        private void drawEdge(Graphics2D g, NodePos parent, NodePos child, boolean red) {
            g.setColor(red ? COLOR_EDGE_RED : COLOR_EDGE_BLACK);
            g.setStroke(new BasicStroke(red ? 2.2f : 1.3f));
            g.drawLine((int) parent.x, (int) (parent.y - NODE_H / 2.0),
                    (int) child.x, (int) (child.y + NODE_H / 2.0));
            g.setStroke(new BasicStroke(1.0f));
        }
    }

    private static Color colorOf(RedBlackBST.HlKind kind) {
        switch (kind) {
            case ROTATE:    return H_ROTATE;
            case FLIP:      return H_FLIP;
            case INSERT:    return H_INSERT;
            case DELETE:    return H_DELETE;
            case TARGET:    return H_TARGET;
            case SUCCESSOR: return H_SUCCESSOR;
            case ROOT:      return H_ROOT;
            default:        return Color.LIGHT_GRAY;
        }
    }

    private static Color textColorFor(Color bg) {
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return lum < 150 ? Color.WHITE : Color.BLACK;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
                // 用默认外观即可
            }
            new RedBlackBSTVisualizer().setVisible(true);
        });
    }
}
