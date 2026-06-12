package github.androkc.lab_2_cloud_calculations.ui;

import github.androkc.lab_2_cloud_calculations.model.ConnectionConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ConnectionDialog extends JDialog {
    
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton cancelButton;
    
    private ConnectionConfig result;
    private boolean confirmed = false;
    
    public ConnectionDialog(Frame parent) {
        super(parent, "Подключение к FTP серверу", true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 220);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Сервер:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        hostField = new JTextField("example", 20);
        inputPanel.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Логин:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        usernameField = new JTextField("example", 20);
        inputPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Пароль:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.7;
        passwordField = new JPasswordField("example", 20);
        inputPanel.add(passwordField, gbc);
        
        add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        connectButton = new JButton("Подключиться");
        connectButton.setPreferredSize(new Dimension(120, 30));
        connectButton.addActionListener(e -> onConnect());
        
        cancelButton = new JButton("Отмена");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> onCancel());
        
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(connectButton);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        ActionMap am = getRootPane().getActionMap();
        am.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
    }
    
    private void onConnect() {
        String host = hostField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (host.isEmpty()) {
            showError("Введите адрес сервера");
            hostField.requestFocus();
            return;
        }
        
        if (username.isEmpty()) {
            showError("Введите имя пользователя");
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Введите пароль");
            passwordField.requestFocus();
            return;
        }
        
        result = new ConnectionConfig(host, username, password);
        confirmed = true;
        dispose();
    }
    
    private void onCancel() {
        confirmed = false;
        dispose();
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
    
    public ConnectionConfig getConnectionConfig() {
        return result;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public void showErrorMessage(String message) {
        showError(message);
    }
}
