package github.androkc.lab_2_cloud_calculations;

import github.androkc.lab_2_cloud_calculations.model.AppConstants;
import github.androkc.lab_2_cloud_calculations.model.ConnectionConfig;
import github.androkc.lab_2_cloud_calculations.service.DirectoryStructureService;
import github.androkc.lab_2_cloud_calculations.service.FTPService;
import github.androkc.lab_2_cloud_calculations.ui.ConnectionDialog;
import github.androkc.lab_2_cloud_calculations.ui.MainFrame;

import javax.swing.*;
import java.io.IOException;

public class Main {
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            showConnectionDialog(frame);
        });
    }

    private static void showConnectionDialog(MainFrame frame) {
        while (true) {
            ConnectionDialog dialog = new ConnectionDialog(frame);
            dialog.setVisible(true);
            
            if (!dialog.isConfirmed()) {
                int result = JOptionPane.showConfirmDialog(frame,
                    "Выйти из приложения?",
                    "Подтверждение", JOptionPane.YES_NO_OPTION);
                
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
                continue;
            }
            
            ConnectionConfig config = dialog.getConnectionConfig();
            if (config == null) {
                continue;
            }

            FTPService ftpService = new FTPService();
            
            try {
                ftpService.connect(config);
                DirectoryStructureService dirService = new DirectoryStructureService(ftpService);

                if (!dirService.structureExists()) {
                    // Создаём структуру
                    if (!dirService.createFullStructure()) {
                        JOptionPane.showMessageDialog(frame,
                            "Не удалось создать структуру каталогов",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                        ftpService.disconnect();
                        continue;
                    }
                }

                ftpService.changeDirectory(AppConstants.ROOT_FOLDER_NAME);

                if (frame.connect(config)) {
                    return;
                }
                
            } catch (IOException e) {
                String errorMessage = e.getMessage();
                
                if (errorMessage != null && errorMessage.contains("530")) {
                    JOptionPane.showMessageDialog(frame,
                        "Ошибка аутентификации: неверное имя пользователя или пароль",
                        "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
                } else if (errorMessage != null && errorMessage.contains("Connection refused")) {
                    JOptionPane.showMessageDialog(frame,
                        "Не удалось подключиться к серверу. Проверьте адрес сервера.",
                        "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
                } else if (errorMessage != null && errorMessage.contains("Unknown host")) {
                    JOptionPane.showMessageDialog(frame,
                        "Сервер не найден. Проверьте адрес сервера.",
                        "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame,
                        "Ошибка подключения: " + errorMessage,
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
                
                ftpService.disconnect();
            }
        }
    }
}
