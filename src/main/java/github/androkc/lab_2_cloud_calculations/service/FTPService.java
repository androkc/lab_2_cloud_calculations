package github.androkc.lab_2_cloud_calculations.service;

import github.androkc.lab_2_cloud_calculations.model.AppConstants;
import github.androkc.lab_2_cloud_calculations.model.ConnectionConfig;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FTPService {
    
    private FTPClient ftpClient;
    private ConnectionConfig config;
    private String rootPath;
    private boolean isConnected;
    
    public FTPService() {
        this.ftpClient = new FTPClient();
        this.isConnected = false;
    }

    public boolean connect(ConnectionConfig config) throws IOException {
        this.config = config;
        
        try {
            ftpClient.connect(config.getHost(), config.getPort());
            ftpClient.setConnectTimeout(AppConstants.FTP_TIMEOUT);
            ftpClient.setDataTimeout(AppConstants.FTP_TIMEOUT);
            
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("Сервер отклонил подключение. Код: " + reply);
            }
            
            boolean success = ftpClient.login(config.getUsername(), config.getPassword());
            if (!success) {
                throw new IOException("Неверное имя пользователя или пароль");
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            this.rootPath = "/";
            this.isConnected = true;
            
            return true;
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            // Игнорируем ошибки при отключении
        } finally {
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected && ftpClient.isConnected();
    }

    public FTPClient getFTPClient() {
        return ftpClient;
    }

    public boolean changeDirectory(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        boolean success = ftpClient.changeWorkingDirectory(path);
        if (success) {
            this.rootPath = path;
        }
        return success;
    }

    public boolean changeToParentDirectory() throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        return ftpClient.changeToParentDirectory();
    }

    public boolean createDirectory(String dirName) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        return ftpClient.makeDirectory(dirName);
    }

    public boolean createDirectoryPath(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        String currentDir = ftpClient.printWorkingDirectory();
        
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            if (!ftpClient.changeWorkingDirectory(part)) {
                if (!ftpClient.makeDirectory(part)) {
                    return false;
                }
                ftpClient.changeWorkingDirectory(part);
            }
        }
        
        ftpClient.changeWorkingDirectory(currentDir);
        return true;
    }

    public boolean deleteDirectory(String dirName) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        return ftpClient.removeDirectory(dirName);
    }

    public List<FTPFile> listFiles() throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        FTPFile[] files = ftpClient.listFiles();
        List<FTPFile> result = new ArrayList<>();
        
        for (FTPFile file : files) {
            if (!file.getName().equals(".") && !file.getName().equals("..")) {
                result.add(file);
            }
        }
        
        return result;
    }

    public List<FTPFile> listFiles(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        String currentDir = ftpClient.printWorkingDirectory();
        
        try {
            ftpClient.changeWorkingDirectory(path);
            List<FTPFile> files = listFiles();
            return files;
        } finally {
            ftpClient.changeWorkingDirectory(currentDir);
        }
    }

    public boolean fileExists(String fileName) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }

        String currentDir = ftpClient.printWorkingDirectory();
        
        try {
            FTPFile[] files = ftpClient.listFiles(fileName);
            return files.length > 0;
        } finally {
            ftpClient.changeWorkingDirectory(currentDir);
        }
    }

    public boolean fileExistsOnServer(String fileName) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        String currentDir = ftpClient.printWorkingDirectory();
        
        try {
            FTPFile[] files = ftpClient.listFiles(fileName);
            return files.length > 0;
        } finally {
            ftpClient.changeWorkingDirectory(currentDir);
        }
    }

    public boolean uploadFile(File localFile, String remotePath) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        String currentDir = ftpClient.printWorkingDirectory();
        
        try {
            // Переходим в корневую папку, затем в нужную подпапку
            ftpClient.changeWorkingDirectory("/");
            if (!remotePath.isEmpty()) {
                // remotePath может быть вида "/cloud_files/January/A"
                // Убираем начальный слэш и переходим по частям
                String[] parts = remotePath.substring(1).split("/");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        ftpClient.changeWorkingDirectory(part);
                    }
                }
            }
            
            try (InputStream input = new FileInputStream(localFile)) {
                return ftpClient.storeFile(localFile.getName(), input);
            }
        } finally {
            ftpClient.changeWorkingDirectory(currentDir);
        }
    }

    public boolean uploadFile(File localFile, String remotePath, ProgressListener listener) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        String currentDir = ftpClient.printWorkingDirectory();
        
        try {
            if (!remotePath.isEmpty()) {
                ftpClient.changeWorkingDirectory(remotePath);
            }
            
            long fileSize = localFile.length();
            long transferred = 0;
            
            try (InputStream input = new FileInputStream(localFile)) {
                try (OutputStream output = ftpClient.storeFileStream(localFile.getName())) {
                    byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                        transferred += bytesRead;
                        if (listener != null) {
                            listener.onProgress(transferred, fileSize);
                        }
                    }
                }
            }
            
            ftpClient.completePendingCommand();
            return true;
        } finally {
            ftpClient.changeWorkingDirectory(currentDir);
        }
    }

    public boolean downloadFile(String remoteFileName, File localFile) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        try (OutputStream output = new FileOutputStream(localFile)) {
            return ftpClient.retrieveFile(remoteFileName, output);
        }
    }

    public boolean downloadFile(String remoteFileName, File localFile, ProgressListener listener) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }

        FTPFile[] files = ftpClient.listFiles(remoteFileName);
        long fileSize = files.length > 0 ? files[0].getSize() : 0;
        long transferred = 0;
        
        try (OutputStream output = new FileOutputStream(localFile)) {
            try (InputStream input = ftpClient.retrieveFileStream(remoteFileName)) {
                if (input == null) {
                    return false;
                }
                
                byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    transferred += bytesRead;
                    if (listener != null) {
                        listener.onProgress(transferred, fileSize);
                    }
                }
            }
        }
        
        return ftpClient.completePendingCommand();
    }

    public boolean deleteFile(String fileName) throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        return ftpClient.deleteFile(fileName);
    }

    public String getCurrentPath() throws IOException {
        if (!isConnected()) {
            throw new IOException("Нет подключения к серверу");
        }
        
        return ftpClient.printWorkingDirectory();
    }

    public boolean isWithinRootPath(String path) {
        if (rootPath == null || rootPath.equals("/")) {
            return true;
        }
        
        String normalizedPath = path;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        
        String normalizedRoot = rootPath;
        if (!normalizedRoot.startsWith("/")) {
            normalizedRoot = "/" + normalizedRoot;
        }
        
        return normalizedPath.startsWith(normalizedRoot) || normalizedPath.equals(normalizedRoot);
    }

    public interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
