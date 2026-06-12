package github.androkc.lab_2_cloud_calculations.ui;

import github.androkc.lab_2_cloud_calculations.model.AppConstants;
import github.androkc.lab_2_cloud_calculations.model.ConnectionConfig;
import github.androkc.lab_2_cloud_calculations.service.DirectoryStructureService;
import github.androkc.lab_2_cloud_calculations.service.FTPService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    
    private FTPService ftpService;
    private DirectoryStructureService directoryService;
    
    private ServerFilePanel serverFilePanel;
    private LocalFilePanel localFilePanel;
    private JLabel statusLabel;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton clearButton;
    private JButton recreateButton;
    private JButton disconnectButton;
    private JButton exitButton;
    
    public MainFrame() {
        initComponents();
        setTitle("FTP Cloud Storage Client - " + AppConstants.ROOT_FOLDER_NAME);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onExit();
            }
        });
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = createActionPanel();
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        
        serverFilePanel = new ServerFilePanel();
        localFilePanel = new LocalFilePanel();
        
        splitPane.setLeftComponent(serverFilePanel);
        splitPane.setRightComponent(localFilePanel);
        
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = createStatusPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        uploadButton = new JButton("↑ Загрузить на сервер");
        uploadButton.addActionListener(e -> uploadFile());
        
        downloadButton = new JButton("↓ Скачать с сервера");
        downloadButton.addActionListener(e -> downloadFile());
        
        clearButton = new JButton("🗑 Очистить каталог");
        clearButton.addActionListener(e -> clearDirectory());
        
        recreateButton = new JButton("🔄 Пересоздать структуру");
        recreateButton.addActionListener(e -> recreateStructure());
        
        disconnectButton = new JButton("Отключиться");
        disconnectButton.addActionListener(e -> disconnect());
        
        exitButton = new JButton("Выход");
        exitButton.addActionListener(e -> onExit());
        
        panel.add(uploadButton);
        panel.add(downloadButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(clearButton);
        panel.add(recreateButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(disconnectButton);
        panel.add(exitButton);
        
        setButtonsEnabled(false);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Не подключено");
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        panel.add(statusLabel, BorderLayout.WEST);
        
        return panel;
    }

    public boolean connect(ConnectionConfig config) {
        ftpService = new FTPService();
        
        try {
            setStatus("Подключение к " + config.getHost() + "...");
            
            ftpService.connect(config);

            directoryService = new DirectoryStructureService(ftpService);

            ftpService.changeDirectory(AppConstants.ROOT_FOLDER_NAME);

            serverFilePanel.setFTPService(ftpService);
            serverFilePanel.loadStructure();
            
            setStatus("Подключено к " + config.getHost() + " | " + AppConstants.ROOT_FOLDER_NAME);
            setButtonsEnabled(true);
            
            return true;
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка подключения: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            setStatus("Ошибка подключения");
            return false;
        }
    }

    private void uploadFile() {
        File localFile = localFilePanel.getSelectedFile();
        
        if (localFile == null || !localFile.isFile()) {
            JOptionPane.showMessageDialog(this,
                "Выберите файл для загрузки",
                "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String remotePath = serverFilePanel.getSelectedPath();

        if (!serverFilePanel.isInsideRootFolder() && !serverFilePanel.isRootFolderSelected()) {
            JOptionPane.showMessageDialog(this,
                "Операции с файлами разрешены только внутри папки '" + AppConstants.ROOT_FOLDER_NAME + "'!",
                "Доступ запрещён", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String currentPath = ftpService.getCurrentPath();
            
            try {
                if (!remotePath.isEmpty()) {
                    ftpService.changeDirectory(remotePath);
                }
                
                if (ftpService.fileExistsOnServer(localFile.getName())) {
                    int result = JOptionPane.showConfirmDialog(this,
                        "Файл '" + localFile.getName() + "' уже существует на сервере.\nПерезаписать?",
                        "Подтверждение", JOptionPane.YES_NO_OPTION);
                    
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                
            } finally {
                ftpService.changeDirectory(currentPath);
            }

            setStatus("Загрузка файла: " + localFile.getName() + "...");
            
            // uploadFile сам переходит в remotePath
            boolean success = ftpService.uploadFile(localFile, remotePath);
            
            if (success) {
                setStatus("Файл загружен: " + localFile.getName());
                JOptionPane.showMessageDialog(this,
                    "Файл успешно загружен!",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
                serverFilePanel.refreshCurrentFolder();
            } else {
                setStatus("Ошибка загрузки файла");
                JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки файла",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка загрузки: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            setStatus("Ошибка загрузки");
        }
    }

    private void downloadFile() {
        String remoteFileName = serverFilePanel.getSelectedFileName();
        
        if (remoteFileName == null) {
            JOptionPane.showMessageDialog(this,
                "Выберите файл для скачивания",
                "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (serverFilePanel.isFolderSelected()) {
            JOptionPane.showMessageDialog(this,
                "Выберите файл, а не папку",
                "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!serverFilePanel.isInsideRootFolder() && !serverFilePanel.isRootFolderSelected()) {
            JOptionPane.showMessageDialog(this,
                "Операции с файлами разрешены только внутри папки '" + AppConstants.ROOT_FOLDER_NAME + "'!",
                "Доступ запрещён", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(localFilePanel.getCurrentDirectory());
        
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File localDir = chooser.getSelectedFile();
        
        // Проверяем что директория существует
        if (!localDir.exists() || !localDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                "Указанная директория не существует",
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File localFile = new File(localDir, remoteFileName);

        if (localFile.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                "Файл '" + localFile.getName() + "' уже существует.\nПерезаписать?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
            
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            // Получаем путь к родительской папке, а не к файлу
            String remotePath = serverFilePanel.getSelectedPath();
            
            setStatus("Скачивание файла: " + remoteFileName + "...");
            
            String currentPath = ftpService.getCurrentPath();
            
            try {
                if (!remotePath.isEmpty()) {
                    // Переходим в родительскую папку (исключаем имя файла)
                    String[] parts = remotePath.substring(1).split("/");
                    ftpService.changeDirectory("/");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (!parts[i].isEmpty()) {
                            ftpService.changeDirectory(parts[i]);
                        }
                    }
                }
                
                boolean success = ftpService.downloadFile(remoteFileName, localFile);
                
                if (success) {
                    setStatus("Файл скачан: " + remoteFileName);
                    JOptionPane.showMessageDialog(this,
                        "Файл успешно скачан!",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
                    localFilePanel.refresh();
                } else {
                    setStatus("Ошибка скачивания файла");
                    JOptionPane.showMessageDialog(this,
                        "Ошибка скачивания файла. Возможно файл не найден на сервере.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
                
            } finally {
                ftpService.changeDirectory(currentPath);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка скачивания: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            setStatus("Ошибка скачивания");
        }
    }

    private void clearDirectory() {
        int result = JOptionPane.showConfirmDialog(this,
            "ВНИМАНИЕ! Каталог '" + AppConstants.ROOT_FOLDER_NAME + "' будет удалён полностью!\n\nПродолжить?",
            "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            setStatus("Удаление каталога...");
            
            boolean success = directoryService.clearStructure();
            
            if (success) {
                setStatus("Каталог удалён");
                JOptionPane.showMessageDialog(this,
                    "Каталог '" + AppConstants.ROOT_FOLDER_NAME + "' успешно удалён!",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
                serverFilePanel.loadStructure();
            } else {
                setStatus("Ошибка удаления каталога");
                JOptionPane.showMessageDialog(this,
                    "Ошибка удаления каталога",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка удаления: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            setStatus("Ошибка удаления");
        }
    }

    private void recreateStructure() {
        int result = JOptionPane.showConfirmDialog(this,
            "Структура каталогов будет создана заново.\nПродолжить?",
            "Подтверждение", JOptionPane.YES_NO_OPTION);
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            setStatus("Создание структуры...");
            
            boolean success = directoryService.recreateStructure();
            
            if (success) {
                setStatus("Структура создана");
                JOptionPane.showMessageDialog(this,
                    "Структура каталогов успешно создана!",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
                serverFilePanel.loadStructure();
            } else {
                setStatus("Ошибка создания структуры");
                JOptionPane.showMessageDialog(this,
                    "Ошибка создания структуры",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            setStatus("Ошибка создания");
        }
    }

    private void disconnect() {
        int result = JOptionPane.showConfirmDialog(this,
            "Отключиться от сервера?",
            "Подтверждение", JOptionPane.YES_NO_OPTION);
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        if (ftpService != null) {
            ftpService.disconnect();
        }
        
        setStatus("Отключено");
        setButtonsEnabled(false);
        
        JOptionPane.showMessageDialog(this, 
            "Соединение закрыто",
            "Информация", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onExit() {
        int result = JOptionPane.showConfirmDialog(this,
            "Завершить работу приложения?",
            "Подтверждение", JOptionPane.YES_NO_OPTION);
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        if (ftpService != null) {
            ftpService.disconnect();
        }
        
        System.exit(0);
    }
    
    private void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    private void setButtonsEnabled(boolean enabled) {
        uploadButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        recreateButton.setEnabled(enabled);
        disconnectButton.setEnabled(enabled);
    }
}
