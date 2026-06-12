package github.androkc.lab_2_cloud_calculations.ui;

import github.androkc.lab_2_cloud_calculations.model.AppConstants;
import github.androkc.lab_2_cloud_calculations.service.FTPService;
import org.apache.commons.net.ftp.FTPFile;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ServerFilePanel extends JPanel {
    
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JLabel currentPathLabel;
    private JButton upButton;
    private JButton refreshButton;
    private JButton selectFolderButton;
    
    private FTPService ftpService;
    private String rootFolderName;
    private String currentServerPath;
    
    public ServerFilePanel() {
        this.rootFolderName = AppConstants.ROOT_FOLDER_NAME;
        this.currentServerPath = "/" + rootFolderName;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Файлы на сервере"));

        // Верхняя панель с кнопками навигации
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        currentPathLabel = new JLabel("/" + rootFolderName);
        currentPathLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        upButton = new JButton("↑");
        upButton.setToolTipText("На уровень вверх");
        upButton.setPreferredSize(new Dimension(40, 25));
        upButton.addActionListener(e -> goUp());
        
        refreshButton = new JButton("↻");
        refreshButton.setToolTipText("Обновить");
        refreshButton.setPreferredSize(new Dimension(40, 25));
        refreshButton.addActionListener(e -> refreshCurrentFolder());
        
        selectFolderButton = new JButton("📁 Выбрать папку");
        selectFolderButton.setToolTipText("Выбрать папку на сервере");
        selectFolderButton.addActionListener(e -> selectFolder());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(upButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(selectFolderButton);
        
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(currentPathLabel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("/");
        treeModel = new DefaultTreeModel(rootNode);
        
        fileTree = new JTree(treeModel);
        fileTree.setShowsRootHandles(true);
        fileTree.setRootVisible(true);
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        fileTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onDoubleClick();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileTree);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void setFTPService(FTPService ftpService) {
        this.ftpService = ftpService;
    }

    public void loadStructure() {
        if (ftpService == null || !ftpService.isConnected()) {
            return;
        }
        
        try {
            rootNode.removeAllChildren();
            currentServerPath = "/" + rootFolderName;

            String currentPath = ftpService.getCurrentPath();
            
            try {
                // Переходим в корневую папку и показываем её содержимое
                ftpService.changeDirectory("/");
                List<FTPFile> rootFiles = ftpService.listFiles();
                
                for (FTPFile file : rootFiles) {
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getName());

                    if (file.isDirectory()) {
                        fileNode.add(new DefaultMutableTreeNode(""));
                    }
                    
                    rootNode.add(fileNode);
                }
                
                treeModel.reload();
                fileTree.expandRow(0);
                currentPathLabel.setText("/");
                
            } finally {
                if (currentPath != null) {
                    ftpService.changeDirectory(currentPath);
                }
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка загрузки структуры: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshCurrentFolder() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        if (selectedNode == null || selectedNode.isLeaf()) {
            return;
        }
        
        try {
            String currentPath = ftpService.getCurrentPath();
            
            try {
                String path = getNodePath(selectedNode);
                if (path != null) {
                    ftpService.changeDirectory(path);
                }

                List<FTPFile> files = ftpService.listFiles();
                selectedNode.removeAllChildren();

                for (FTPFile file : files) {
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.getName());
                    if (file.isDirectory()) {
                        fileNode.add(new DefaultMutableTreeNode(""));
                    }
                    selectedNode.add(fileNode);
                }
                
                treeModel.reload(selectedNode);
                
            } finally {
                ftpService.changeDirectory(currentPath);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка обновления: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void goUp() {
        // Навигация вверх в дереве - просто поднимаем выделение
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null || selectedPath.getPathCount() <= 1) {
            return;
        }
        
        TreePath parentPath = selectedPath.getParentPath();
        if (parentPath != null) {
            fileTree.setSelectionPath(parentPath);
            fileTree.scrollPathToVisible(parentPath);
        }
    }
    
    public void selectFolder() {
        if (ftpService == null || !ftpService.isConnected()) {
            return;
        }
        
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            JOptionPane.showMessageDialog(this,
                "Выберите папку в дереве",
                "Внимание", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        if (selectedNode.isLeaf()) {
            JOptionPane.showMessageDialog(this,
                "Выберите папку, а не файл",
                "Внимание", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String newPath = getNodePath(selectedNode);
        String rootLimit = "/" + rootFolderName;
        
        // Проверяем, что выбранная папка внутри корневой
        if (!newPath.startsWith(rootLimit) && !newPath.equals("/")) {
            JOptionPane.showMessageDialog(this,
                "Можно выбрать только папку внутри '" + rootFolderName + "'",
                "Доступ запрещён", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        currentServerPath = newPath;
        currentPathLabel.setText(currentServerPath);
        refreshCurrentFolder();
    }

    private String getNodePath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder();
        
        TreePath treePath = new TreePath(treeModel.getPathToRoot(node));
        for (int i = 1; i < treePath.getPathCount(); i++) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) treePath.getPath()[i];
            path.append("/").append(n.toString());
        }
        
        return path.toString();
    }

    public String getSelectedPath() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            return "";
        }
        
        StringBuilder path = new StringBuilder();
        // Включаем все элементы (путь к выбранной папке)
        for (int i = 1; i < selectedPath.getPathCount(); i++) {
            path.append("/").append(selectedPath.getPath()[i].toString());
        }
        
        return path.toString();
    }

    public String getSelectedFileName() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null || selectedPath.getPathCount() < 2) {
            return null;
        }
        
        return selectedPath.getLastPathComponent().toString();
    }

    public boolean isFolderSelected() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            return false;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        return node != null && !node.isLeaf();
    }

    public boolean isInsideRootFolder() {
        String selectedPath = getSelectedPath();
        if (selectedPath.isEmpty()) {
            return true; // Корень - всегда можно
        }
        return selectedPath.startsWith("/" + rootFolderName);
    }

    public boolean isRootFolderSelected() {
        String selectedPath = getSelectedPath();
        return selectedPath.equals("/" + rootFolderName) || selectedPath.isEmpty();
    }
    
    public String getCurrentServerPath() {
        return getSelectedPath();
    }
    
    private void onDoubleClick() {
        refreshCurrentFolder();
    }

    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            
            if (node.isLeaf() && !node.toString().isEmpty()) {
                setIcon(UIManager.getIcon("FileView.fileIcon"));
            } else if (expanded) {
                setIcon(UIManager.getIcon("FileView.openFolderIcon"));
            } else {
                setIcon(UIManager.getIcon("FileView.folderIcon"));
            }
            
            return this;
        }
    }
}
