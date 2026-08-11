package cn.exercise.algs4.datastructure.btree;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BPlusTreeVisualizer extends JFrame {

    private BPlusTree tree;
    private int t;
    private final TreePanel treePanel;
    private final JLabel statusLabel;
    private final JLabel sizeLabel;
    private final JLabel heightLabel;
    private final JTextField keyField;
    private final JSpinner speedSpinner;
    private List<BPlusTree.TraceStep> currentTrace;
    private int currentStep;
    private javax.swing.Timer animTimer;

    private static final Color INTERNAL_COLOR = new Color(180, 210, 240);
    private static final Color LEAF_COLOR = new Color(190, 230, 190);
    private static final Color INTERNAL_BORDER = new Color(70, 120, 180);
    private static final Color LEAF_BORDER = new Color(60, 140, 60);
    private static final Color TARGET_COLOR = new Color(255, 200, 50);
    private static final Color MODIFIED_COLOR = new Color(255, 140, 0);
    private static final Color DELETED_COLOR = new Color(220, 50, 50);
    private static final Color MOVED_COLOR = new Color(180, 80, 200);
    private static final Color INSERTED_COLOR = new Color(30, 160, 60);
    private static final Color SPLITTING_COLOR = new Color(255, 100, 30);
    private static final Color LEAF_CHAIN_COLOR = new Color(80, 80, 80);

    public BPlusTreeVisualizer(int t) {
        this.t = t;
        this.tree = new BPlusTree(t);
        this.currentTrace = null;
        this.currentStep = 0;

        setTitle("B+ 树可视化 (t=" + t + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        treePanel = new TreePanel();
        treePanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(treePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(10, 5));
        controlPanel.setBorder(new EmptyBorder(8, 12, 4, 12));
        controlPanel.setBackground(new Color(245, 245, 245));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setBackground(new Color(245, 245, 245));

        keyField = new JTextField(6);
        keyField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        keyField.addActionListener(e -> handleKeyInput());
        leftPanel.add(new JLabel("键:"));
        leftPanel.add(keyField);

        JButton insertBtn = new JButton("插入");
        insertBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        insertBtn.addActionListener(e -> {
            String text = keyField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    int key = Integer.parseInt(text);
                    insertAnimated(key);
                } catch (NumberFormatException ex) {
                    showStatus("请输入合法整数");
                }
            }
        });
        leftPanel.add(insertBtn);

        JButton deleteBtn = new JButton("删除");
        deleteBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        deleteBtn.addActionListener(e -> {
            String text = keyField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    int key = Integer.parseInt(text);
                    deleteAnimated(key);
                } catch (NumberFormatException ex) {
                    showStatus("请输入合法整数");
                }
            }
        });
        leftPanel.add(deleteBtn);

        JButton searchBtn = new JButton("查找");
        searchBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchBtn.addActionListener(e -> {
            String text = keyField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    int key = Integer.parseInt(text);
                    searchAnimated(key);
                } catch (NumberFormatException ex) {
                    showStatus("请输入合法整数");
                }
            }
        });
        leftPanel.add(searchBtn);

        JButton batchBtn = new JButton("批量演示");
        batchBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        batchBtn.addActionListener(e -> batchDemo());
        leftPanel.add(batchBtn);

        JButton clearBtn = new JButton("清空");
        clearBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        clearBtn.addActionListener(e -> {
            stopAnimation();
            tree = new BPlusTree(t);
            currentTrace = null;
            currentStep = 0;
            treePanel.updateTree(tree.snapshot(), null);
            updateInfoLabels();
            showStatus("树已清空");
        });
        leftPanel.add(clearBtn);

        controlPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setBackground(new Color(245, 245, 245));
        rightPanel.add(new JLabel("速度(ms):"));
        speedSpinner = new JSpinner(new SpinnerNumberModel(600, 50, 3000, 50));
        speedSpinner.setPreferredSize(new Dimension(70, 28));
        rightPanel.add(speedSpinner);

        JButton prevBtn = new JButton("◀ 上一步");
        prevBtn.addActionListener(e -> stepPrev());
        rightPanel.add(prevBtn);

        JButton playBtn = new JButton("▶ 播放");
        playBtn.addActionListener(e -> playAnimation());
        rightPanel.add(playBtn);

        JButton nextBtn = new JButton("下一步 ▶");
        nextBtn.addActionListener(e -> stepNext());
        rightPanel.add(nextBtn);

        controlPanel.add(rightPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(4, 12, 8, 12));
        statusPanel.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("就绪。请输入键值后点击 插入/删除/查找");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(50, 50, 50));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.setBackground(new Color(240, 240, 240));
        sizeLabel = new JLabel("size: 0");
        sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPanel.add(sizeLabel);
        heightLabel = new JLabel("height: -1");
        heightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPanel.add(heightLabel);
        JLabel tLabel = new JLabel("t: " + t + "  maxKeys: " + (2 * t - 1));
        tLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPanel.add(tLabel);
        statusPanel.add(infoPanel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);

        treePanel.updateTree(tree.snapshot(), null);
        updateInfoLabels();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                treePanel.repaint();
            }
        });
    }

    private void handleKeyInput() {
        String text = keyField.getText().trim();
        if (text.isEmpty()) return;
        try {
            int key = Integer.parseInt(text);
            insertAnimated(key);
        } catch (NumberFormatException ex) {
            showStatus("请输入合法整数");
        }
    }

    private void insertAnimated(int key) {
        stopAnimation();
        currentTrace = tree.insertTraced(key);
        currentStep = 0;
        playAnimation();
    }

    private void deleteAnimated(int key) {
        stopAnimation();
        currentTrace = tree.removeTraced(key);
        currentStep = 0;
        playAnimation();
    }

    private void searchAnimated(int key) {
        stopAnimation();
        currentTrace = tree.getTraced(key);
        currentStep = 0;
        playAnimation();
    }

    private void batchDemo() {
        stopAnimation();
        tree = new BPlusTree(t);
        int[] seq = {1, 2,10,11,12, 13, 19, 20,21, 22, 23, 24, 25, 26, 27, 28, 29, 30,31, 32, 33, 34, 15, 25, 5, 35, 40,41, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 18, 12, 50, 3, 8, 28, 33};
        List<BPlusTree.TraceStep> fullTrace = new ArrayList<>();
        for (int k : seq) {
            List<BPlusTree.TraceStep> one = tree.insertTraced(k);
            fullTrace.addAll(one);
        }
        currentTrace = fullTrace;
        currentStep = 0;
        playAnimation();
    }

    private void playAnimation() {
        if (currentTrace == null || currentTrace.isEmpty()) {
            showStatus("没有可播放的步骤");
            treePanel.updateTree(tree.snapshot(), null);
            return;
        }
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        int delay = (Integer) speedSpinner.getValue();
        animTimer = new javax.swing.Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep < currentTrace.size()) {
                    showStep(currentStep);
                    currentStep++;
                } else {
                    animTimer.stop();
                }
            }
        });
        animTimer.start();
    }

    private void stopAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
    }

    private void stepPrev() {
        stopAnimation();
        if (currentTrace == null || currentTrace.isEmpty()) return;
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    }

    private void stepNext() {
        stopAnimation();
        if (currentTrace == null || currentTrace.isEmpty()) {
            treePanel.updateTree(tree.snapshot(), null);
            return;
        }
        if (currentStep < currentTrace.size()) {
            showStep(currentStep);
            currentStep++;
        }
    }

    private void showStep(int index) {
        BPlusTree.TraceStep step = currentTrace.get(index);
        treePanel.updateTree(step.root, step.hl);
        updateInfoLabelsFromSnapshot(step.root);
        showStatus("步骤 " + (index + 1) + "/" + currentTrace.size() + ": " + step.desc);
    }

    private void showStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void updateInfoLabels() {
        updateInfoLabelsFromSnapshot(tree.snapshot());
    }

    private void updateInfoLabelsFromSnapshot(BPlusTree.NodeView root) {
        int size = 0;
        int height = -1;
        if (root != null) {
            height = 0;
            BPlusTree.NodeView cur = root;
            while (cur != null && !cur.isLeaf) {
                height++;
                cur = cur.children[0];
            }
            height++;
            size = countKeys(root);
        }
        sizeLabel.setText("size: " + size);
        heightLabel.setText("height: " + height);
    }

    private int countKeys(BPlusTree.NodeView node) {
        if (node == null) return 0;
        int count = node.keys.length;
        if (!node.isLeaf) {
            for (BPlusTree.NodeView c : node.children) {
                count += countKeys(c);
            }
        }
        return count;
    }

    private class TreePanel extends JPanel {
        private BPlusTree.NodeView rootView;
        private List<BPlusTree.Hl> highlights;
        private Map<BPlusTree.NodeView, NodeLayout> layoutMap;
        private Dimension preferredSize;

        public TreePanel() {
            setDoubleBuffered(true);
        }

        @Override
        public Dimension getPreferredSize() {
            if (preferredSize != null) {
                return preferredSize;
            }
            return super.getPreferredSize();
        }

        public void updateTree(BPlusTree.NodeView root, List<BPlusTree.Hl> hl) {
            this.rootView = root;
            this.highlights = hl != null ? hl : new ArrayList<>();
            this.layoutMap = null;
            if (root == null) {
                preferredSize = null;
                revalidate();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (rootView == null) {
                drawEmptyTree(g2);
                return;
            }

            computeLayout();
            drawTree(g2);
        }

        private void drawEmptyTree(Graphics2D g2) {
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "空 B+ 树 (请输入键值开始构建)";
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = getHeight() / 2;
            g2.drawString(msg, x, y);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(new Color(150, 150, 150));
            String tip = "提示:t=" + t + ", 内部节点≤" + (2 * t - 1) + "键,叶子≤" + (2 * t - 1) + "键";
            fm = g2.getFontMetrics();
            x = (getWidth() - fm.stringWidth(tip)) / 2;
            y += 30;
            g2.drawString(tip, x, y);
        }

        private void computeLayout() {
            layoutMap = new HashMap<>();
            int treeHeight = getHeight(rootView);
            int minLevelHeight = 50;
            int topMargin = 40;
            int bottomMargin = 120;
            int totalHeight = treeHeight * minLevelHeight + topMargin + bottomMargin;

            int minWidth = computeMinWidth(rootView);
            int layoutWidth = Math.max(getWidth(), minWidth);
            int layoutHeight = Math.max(getHeight(), totalHeight);

            preferredSize = new Dimension(layoutWidth, layoutHeight);
            revalidate();

            int levelHeight = Math.max(minLevelHeight, treeHeight > 0
                    ? (layoutHeight - topMargin - bottomMargin) / treeHeight
                    : minLevelHeight);

            layoutRecursive(rootView, 0, layoutWidth, 0, topMargin, levelHeight);
        }

        private int computeMinWidth(BPlusTree.NodeView node) {
            int keyCount = node.keys.length;
            int nodeMinWidth = keyCount * 36;

            if (node.isLeaf) {
                return nodeMinWidth;
            }

            int childCount = node.children.length;
            int maxChildMinWidth = 0;
            for (BPlusTree.NodeView child : node.children) {
                maxChildMinWidth = Math.max(maxChildMinWidth, computeMinWidth(child));
            }

            return Math.max(nodeMinWidth, childCount * maxChildMinWidth);
        }

        private int getHeight(BPlusTree.NodeView node) {
            if (node.isLeaf) return 1;
            int childHeight = 0;
            for (BPlusTree.NodeView c : node.children) {
                childHeight = Math.max(childHeight, getHeight(c));
            }
            return 1 + childHeight;
        }

        private void layoutRecursive(BPlusTree.NodeView node, int depth, int width, int xLeft, int y, int levelHeight) {
            NodeLayout layout = new NodeLayout();
            layout.y = y;
            layout.height = 36;
            int keyCount = node.keys.length;
            int cellWidth = Math.max(36, Math.min(70, (width - 20) / Math.max(keyCount, 3)));
            int totalWidth = keyCount * cellWidth;
            layout.width = totalWidth;
            layout.x = xLeft + (width - totalWidth) / 2;
            layout.cellWidth = cellWidth;
            layoutMap.put(node, layout);

            if (!node.isLeaf) {
                int childY = y + levelHeight;
                int totalChildWidth = width;
                int childCount = node.children.length;
                int childWidth = totalChildWidth / childCount;
                for (int i = 0; i < childCount; i++) {
                    int childLeft = xLeft + i * childWidth;
                    layoutRecursive(node.children[i], depth + 1, childWidth, childLeft, childY, levelHeight);
                }
            }
        }

        private void drawTree(Graphics2D g2) {
            if (rootView == null) return;

            Map<BPlusTree.NodeView, NodeLayout> lm = layoutMap;

            List<BPlusTree.NodeView> allNodes = new ArrayList<>();
            collectAllNodes(rootView, allNodes);

            List<BPlusTree.NodeView> leaves = new ArrayList<>();
            collectLeaves(rootView, leaves);

            for (BPlusTree.NodeView node : allNodes) {
                NodeLayout l = lm.get(node);
                if (l == null) continue;
                if (!node.isLeaf && node.children != null) {
                    for (int i = 0; i < node.children.length; i++) {
                        BPlusTree.NodeView child = node.children[i];
                        NodeLayout cl = lm.get(child);
                        if (cl == null) continue;
                        int parentBottom = l.y + l.height;
                        int childTop = cl.y;
                        int childCenterX = cl.x + cl.width / 2;
                        int anchorX;
                        if (i == 0) {
                            anchorX = l.x;
                        } else if (i == node.children.length - 1) {
                            anchorX = l.x + l.width;
                        } else {
                            int keyRight = l.x + i * l.cellWidth;
                            anchorX = keyRight;
                        }
                        g2.setColor(new Color(120, 120, 120));
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawLine(anchorX, parentBottom, childCenterX, childTop);
                    }
                }
            }

            if (leaves.size() > 1) {
                g2.setColor(LEAF_CHAIN_COLOR);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6, 4}, 0));
                for (int i = 0; i + 1 < leaves.size(); i++) {
                    NodeLayout l1 = lm.get(leaves.get(i));
                    NodeLayout l2 = lm.get(leaves.get(i + 1));
                    if (l1 == null || l2 == null) continue;
                    int x1 = l1.x + l1.width;
                    int y1 = l1.y + l1.height / 2;
                    int x2 = l2.x;
                    int y2 = l2.y + l2.height / 2;
                    g2.drawLine(x1, y1, x2, y2);
                    drawArrowHead(g2, x2, y2, x1, y1, LEAF_CHAIN_COLOR);
                }
                g2.setStroke(new BasicStroke(1.2f));
            }

            for (BPlusTree.NodeView node : allNodes) {
                NodeLayout l = lm.get(node);
                if (l == null) continue;
                drawNode(g2, node, l);
            }

            drawLegend(g2);
        }

        private void drawArrowHead(Graphics2D g2, int tipX, int tipY, int fromX, int fromY, Color color) {
            double angle = Math.atan2(tipY - fromY, tipX - fromX);
            int arrowSize = 7;
            Polygon arrow = new Polygon();
            arrow.addPoint(tipX, tipY);
            arrow.addPoint((int) (tipX - arrowSize * Math.cos(angle - Math.PI / 6)),
                    (int) (tipY - arrowSize * Math.sin(angle - Math.PI / 6)));
            arrow.addPoint((int) (tipX - arrowSize * Math.cos(angle + Math.PI / 6)),
                    (int) (tipY - arrowSize * Math.sin(angle + Math.PI / 6)));
            g2.setColor(color);
            g2.fillPolygon(arrow);
        }

        private void drawNode(Graphics2D g2, BPlusTree.NodeView node, NodeLayout l) {
            boolean isLeaf = node.isLeaf;
            int x = l.x, y = l.y, w = l.width, h = l.height;

            BPlusTree.HlKind nodeHlKind = getHighlightKind(node);
            boolean isTarget = nodeHlKind == BPlusTree.HlKind.TARGET;
            boolean isModified = nodeHlKind == BPlusTree.HlKind.MODIFIED;
            boolean isSplitting = nodeHlKind == BPlusTree.HlKind.SPLITTING;
            boolean isDeleted = nodeHlKind == BPlusTree.HlKind.DELETED;
            boolean isInserted = nodeHlKind == BPlusTree.HlKind.INSERTED;
            boolean isMoved = nodeHlKind == BPlusTree.HlKind.MOVED;

            Color bgColor = isLeaf ? LEAF_COLOR : INTERNAL_COLOR;
            Color borderColor = isLeaf ? LEAF_BORDER : INTERNAL_BORDER;
            int borderWidth = 2;

            if (isTarget) {
                borderColor = TARGET_COLOR;
                borderWidth = 3;
            }
            if (isSplitting) {
                bgColor = new Color(255, 220, 180);
                borderColor = SPLITTING_COLOR;
                borderWidth = 3;
            }
            if (isModified) {
                borderColor = MODIFIED_COLOR;
                borderWidth = 3;
            }

            g2.setColor(bgColor);
            g2.setStroke(new BasicStroke(borderWidth));
            if (isLeaf) {
                g2.drawRoundRect(x, y, w, h, 8, 8);
                g2.fillRoundRect(x, y, w, h, 8, 8);
            } else {
                g2.drawRect(x, y, w, h);
                g2.fillRect(x, y, w, h);
            }

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(borderWidth));
            if (isLeaf) {
                g2.drawRoundRect(x, y, w, h, 8, 8);
            } else {
                g2.drawRect(x, y, w, h);
            }

            int keyCount = node.keys.length;
            int cellW = l.cellWidth;
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < keyCount; i++) {
                int kx = x + i * cellW;
                int ky = y;

                if (i > 0) {
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawLine(kx, ky + 2, kx, ky + h - 2);
                }

                int key = node.keys[i];
                BPlusTree.HlKind keyHlKind = getKeyHighlightKind(node, i);

                Color textColor = Color.BLACK;
                boolean strikethrough = false;
                if (keyHlKind == BPlusTree.HlKind.DELETED || isDeleted) {
                    textColor = DELETED_COLOR;
                    strikethrough = true;
                } else if (keyHlKind == BPlusTree.HlKind.INSERTED || isInserted) {
                    textColor = INSERTED_COLOR;
                } else if (keyHlKind == BPlusTree.HlKind.MOVED || isMoved) {
                    textColor = MOVED_COLOR;
                } else if (keyHlKind == BPlusTree.HlKind.TARGET || isTarget) {
                    textColor = new Color(180, 120, 0);
                } else if (keyHlKind == BPlusTree.HlKind.SPLITTING || isSplitting) {
                    textColor = SPLITTING_COLOR;
                }

                String keyText = String.valueOf(key);
                int textWidth = fm.stringWidth(keyText);
                int textX = kx + (cellW - textWidth) / 2;
                int textY = ky + (h - fm.getHeight()) / 2 + fm.getAscent();

                if (strikethrough) {
                    g2.setColor(textColor);
                    g2.drawLine(textX - 2, textY - fm.getAscent() + fm.getDescent() + 2,
                            textX + textWidth + 2, textY - fm.getAscent() + fm.getDescent() + 2);
                }

                g2.setColor(textColor);
                g2.drawString(keyText, textX, textY);
            }

            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String typeLabel = isLeaf ? "L" : "I";
            g2.drawString(typeLabel, x + 4, y + h - 4);
        }

        private BPlusTree.HlKind getHighlightKind(BPlusTree.NodeView node) {
            for (BPlusTree.Hl hl : highlights) {
                if (hl.slot == -1 && pathMatches(hl.path, node)) {
                    return hl.kind;
                }
            }
            return null;
        }

        private BPlusTree.HlKind getKeyHighlightKind(BPlusTree.NodeView node, int slot) {
            for (BPlusTree.Hl hl : highlights) {
                if (hl.slot == slot && pathMatches(hl.path, node)) {
                    return hl.kind;
                }
            }
            return null;
        }

        private boolean pathMatches(int[] path, BPlusTree.NodeView node) {
            return findNodeByPath(rootView, path) == node;
        }

        private BPlusTree.NodeView findNodeByPath(BPlusTree.NodeView current, int[] path) {
            if (path == null || path.length == 0) {
                return current;
            }
            BPlusTree.NodeView node = current;
            for (int i = 0; i < path.length; i++) {
                if (node == null || node.isLeaf) return null;
                int idx = path[i];
                if (idx < 0 || idx >= node.children.length) return null;
                node = node.children[idx];
            }
            return node;
        }

        private void collectAllNodes(BPlusTree.NodeView node, List<BPlusTree.NodeView> out) {
            if (node == null) return;
            out.add(node);
            if (!node.isLeaf && node.children != null) {
                for (BPlusTree.NodeView c : node.children) {
                    collectAllNodes(c, out);
                }
            }
        }

        private void collectLeaves(BPlusTree.NodeView node, List<BPlusTree.NodeView> out) {
            if (node == null) return;
            if (node.isLeaf) {
                out.add(node);
            } else if (node.children != null) {
                for (BPlusTree.NodeView c : node.children) {
                    collectLeaves(c, out);
                }
            }
        }

        private void drawLegend(Graphics2D g2) {
            int x = 15;
            int y = getHeight() - 60;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

            drawLegendItem(g2, x, y, TARGET_COLOR, "TARGET 下潜目标");
            drawLegendItem(g2, x + 130, y, MODIFIED_COLOR, "MODIFIED 被修改");
            drawLegendItem(g2, x + 260, y, DELETED_COLOR, "DELETED 被删除");
            drawLegendItem(g2, x + 390, y, MOVED_COLOR, "MOVED 移动");
            drawLegendItem(g2, x + 520, y, INSERTED_COLOR, "INSERTED 新插入");
            drawLegendItem(g2, x + 650, y, SPLITTING_COLOR, "SPLITTING 分裂中");

            y += 18;
            int bx = x;
            g2.setColor(INTERNAL_COLOR);
            g2.fillRect(bx, y, 20, 14);
            g2.setColor(INTERNAL_BORDER);
            g2.drawRect(bx, y, 20, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("内部节点 (分隔键)", bx + 26, y + 11);

            bx += 120;
            g2.setColor(LEAF_COLOR);
            g2.fillRoundRect(bx, y, 20, 14, 4, 4);
            g2.setColor(LEAF_BORDER);
            g2.drawRoundRect(bx, y, 20, 14, 4, 4);
            g2.setColor(Color.BLACK);
            g2.drawString("叶子节点 (数据)", bx + 26, y + 11);

            bx += 120;
            g2.setColor(LEAF_CHAIN_COLOR);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4, 3}, 0));
            g2.drawLine(bx, y + 7, bx + 25, y + 7);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(Color.BLACK);
            g2.drawString("叶子链表", bx + 32, y + 11);
        }

        private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, 14, 14, 3, 3);
            g2.fillRoundRect(x, y, 14, 14, 3, 3);
            g2.setColor(Color.BLACK);
            g2.drawString(label, x + 20, y + 12);
        }
    }

    private static class NodeLayout {
        int x, y, width, height, cellWidth;
    }

    public static void main(String[] args) {
        int t = 2;
        if (args.length > 0) {
            try {
                t = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        final int finalT = t;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new BPlusTreeVisualizer(finalT).setVisible(true);
            }
        });
    }
}