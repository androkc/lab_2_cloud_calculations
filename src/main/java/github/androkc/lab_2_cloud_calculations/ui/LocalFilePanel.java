package github.androkc.lab_2_cloud_calculations.ui;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;

public class LocalFilePanel extends JPanel {
    
    private JList<File> fileList;
    private DefaultListModel<File> listModel;
    private JLabel currentPathLabel;
    private JButton upButton;
    private JButton refreshButton;
    private JButton selectFileButton;
    
    private File currentDirectory;
    private FileSystemView fileSystemView;
    
    public LocalFilePanel() {
        fileSystemView = FileSystemView.getFileSystemView();
        initComponents();
        loadDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Локальные файлы"));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        currentPathLabel = new JLabel();
        currentPathLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        upButton = new JButton("↑");
        upButton.setToolTipText("На уровень вверх");
        upButton.setPreferredSize(new Dimension(40, 25));
        upButton.addActionListener(e -> goUp());
        
        refreshButton = new JButton("↻");
        refreshButton.setToolTipText("Обновить");
        refreshButton.setPreferredSize(new Dimension(40, 25));
        refreshButton.addActionListener(e -> refresh());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(upButton);
        buttonPanel.add(refreshButton);
        
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(currentPathLabel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setCellRenderer(new FileListCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onDoubleClick();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        add(scrollPane, BorderLayout.CENTER);

        selectFileButton = new JButton("Выбрать файл");
        selectFileButton.addActionListener(e -> selectFile());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(selectFileButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        this.currentDirectory = directory;
        currentPathLabel.setText(directory.getAbsolutePath());
        
        listModel.clear();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        
        for (File file : files) {
            if (!file.isHidden()) {
                listModel.addElement(file);
            }
        }
    }

    public void goUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            loadDirectory(parent);
        }
    }

    public void refresh() {
        loadDirectory(currentDirectory);
    }

    private void onDoubleClick() {
        File selected = fileList.getSelectedValue();
        if (selected != null && selected.isDirectory()) {
            loadDirectory(selected);
        }
    }

    public void selectFile() {
        File selected = fileList.getSelectedValue();
        if (selected != null && selected.isFile()) {
            // Файл выбран
        }
    }

    public File getSelectedFile() {
        return fileList.getSelectedValue();
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    private class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            File file = (File) value;
            setText(file.getName());
            
            if (file.isDirectory()) {
                setIcon(fileSystemView.getSystemIcon(file));
            } else {
                setIcon(fileSystemView.getSystemIcon(file));
            }
            
            return this;
        }
    }
}
