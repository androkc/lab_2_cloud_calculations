package github.androkc.lab_2_cloud_calculations.service;

import github.androkc.lab_2_cloud_calculations.model.AppConstants;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.util.List;

public class DirectoryStructureService {
    
    private final FTPService ftpService;
    
    public DirectoryStructureService(FTPService ftpService) {
        this.ftpService = ftpService;
    }

    public boolean createFullStructure() throws IOException {
        String rootFolder = AppConstants.ROOT_FOLDER_NAME;
        FTPClient ftp = ftpService.getFTPClient();
        
        System.out.println("[LOG] createFullStructure: starting");

        String savedDir = ftp.printWorkingDirectory();
        System.out.println("[LOG] createFullStructure: current dir = " + savedDir);

        ftp.changeToParentDirectory();
        while (!ftp.printWorkingDirectory().equals("/")) {
            ftp.changeToParentDirectory();
        }
        System.out.println("[LOG] createFullStructure: moved to root = " + ftp.printWorkingDirectory());
        
        try {
            if (!ftpService.fileExists(rootFolder)) {
                System.out.println("[LOG] createFullStructure: creating root folder " + rootFolder);
                if (!ftpService.createDirectory(rootFolder)) {
                    System.out.println("[LOG] createFullStructure: FAILED to create root folder");
                    return false;
                }
            } else {
                System.out.println("[LOG] createFullStructure: root folder already exists");
            }

            ftpService.changeDirectory(rootFolder);
            System.out.println("[LOG] createFullStructure: changed to " + rootFolder);

            for (String month : AppConstants.MONTHS) {
                if (!ftpService.fileExists(month)) {
                    System.out.println("[LOG] createFullStructure: creating month " + month);
                    if (!ftpService.createDirectory(month)) {
                        System.out.println("[LOG] createFullStructure: FAILED to create month " + month);
                        return false;
                    }
                }

                ftpService.changeDirectory(month);

                for (String letter : AppConstants.THIRD_LEVEL_FOLDERS) {
                    if (!ftpService.fileExists(letter)) {
                        if (!ftpService.createDirectory(letter)) {
                            System.out.println("[LOG] createFullStructure: FAILED to create letter " + letter);
                            return false;
                        }
                    }
                }

                ftpService.changeDirectory("..");
            }
            
            System.out.println("[LOG] createFullStructure: SUCCESS");
            return true;
        } finally {
            ftp.changeWorkingDirectory(savedDir);
            System.out.println("[LOG] createFullStructure: returned to = " + ftp.printWorkingDirectory());
        }
    }

    public boolean clearStructure() throws IOException {
        String rootFolder = AppConstants.ROOT_FOLDER_NAME;
        FTPClient ftp = ftpService.getFTPClient();
        
        System.out.println("[LOG] clearStructure: starting");
        System.out.println("[LOG] clearStructure: rootFolder = " + rootFolder);

        String savedDir = ftp.printWorkingDirectory();
        System.out.println("[LOG] clearStructure: current dir = " + savedDir);

        ftp.changeToParentDirectory();
        while (!ftp.printWorkingDirectory().equals("/")) {
            ftp.changeToParentDirectory();
        }
        System.out.println("[LOG] clearStructure: moved to root = " + ftp.printWorkingDirectory());
        
        boolean success = false;
        
        try {
            if (!ftpService.fileExists(rootFolder)) {
                System.out.println("[LOG] clearStructure: root folder does not exist, nothing to delete");
                success = true;
                return true;
            }
            
            System.out.println("[LOG] clearStructure: root folder exists, proceeding...");

            boolean changed = ftp.changeWorkingDirectory(rootFolder);
            System.out.println("[LOG] clearStructure: changed to rootFolder = " + changed + ", new path = " + ftp.printWorkingDirectory());

            deleteAllContentsRecursively();

            System.out.println("[LOG] clearStructure: all contents deleted, current path = " + ftp.printWorkingDirectory());

            boolean parentChanged = ftp.changeToParentDirectory();
            System.out.println("[LOG] clearStructure: changed to parent = " + parentChanged + ", new path = " + ftp.printWorkingDirectory());

            boolean removed = ftp.removeDirectory(rootFolder);
            System.out.println("[LOG] clearStructure: removeDirectory(" + rootFolder + ") = " + removed);
            System.out.println("[LOG] clearStructure: reply code = " + ftp.getReplyCode());
            System.out.println("[LOG] clearStructure: reply string = " + ftp.getReplyString());
            
            success = true;
            System.out.println("[LOG] clearStructure: done");
        } finally {
            ftp.changeWorkingDirectory(savedDir);
            System.out.println("[LOG] clearStructure: returned to = " + ftp.printWorkingDirectory());
        }
        
        return success;
    }

    private void deleteAllContentsRecursively() throws IOException {
        FTPClient ftp = ftpService.getFTPClient();
        String currentDir = ftp.printWorkingDirectory();
        System.out.println("[LOG] deleteAllContentsRecursively: in directory " + currentDir);
        
        FTPFile[] files = ftp.listFiles();
        System.out.println("[LOG] deleteAllContentsRecursively: found " + files.length + " items");
        
        int deletedFiles = 0;
        int deletedDirs = 0;
        
        for (FTPFile file : files) {
            if (file.getName().equals(".") || file.getName().equals("..")) {
                continue;
            }
            
            if (file.isDirectory()) {
                System.out.println("[LOG] deleteAllContentsRecursively: entering dir " + file.getName());
                ftp.changeWorkingDirectory(file.getName());
                deleteAllContentsRecursively();
                ftp.changeToParentDirectory();
                boolean removed = ftp.removeDirectory(file.getName());
                System.out.println("[LOG] deleteAllContentsRecursively: removed dir " + file.getName() + " = " + removed);
                deletedDirs++;
            } else {
                boolean deleted = ftp.deleteFile(file.getName());
                System.out.println("[LOG] deleteAllContentsRecursively: deleted file " + file.getName() + " = " + deleted);
                deletedFiles++;
            }
        }
        
        System.out.println("[LOG] deleteAllContentsRecursively: done, deleted " + deletedFiles + " files, " + deletedDirs + " dirs");
    }
    

    public boolean recreateStructure() throws IOException {
        System.out.println("[LOG] recreateStructure: starting");
        return createFullStructure();
    }
    

    public boolean structureExists() throws IOException {
        return ftpService.fileExists(AppConstants.ROOT_FOLDER_NAME);
    }

    public boolean isStructureEmpty() throws IOException {
        String rootFolder = AppConstants.ROOT_FOLDER_NAME;
        
        if (!ftpService.fileExists(rootFolder)) {
            return true;
        }
        
        String currentPath = ftpService.getCurrentPath();
        
        try {
            ftpService.changeDirectory(rootFolder);
            
            var files = ftpService.listFiles();
            for (var file : files) {
                if (!file.isDirectory()) {
                    return false;
                }
                ftpService.changeDirectory(file.getName());
                if (!ftpService.listFiles().isEmpty()) {
                    ftpService.changeDirectory("..");
                    return false;
                }
                ftpService.changeDirectory("..");
            }
            
            return true;
        } finally {
            ftpService.changeDirectory(currentPath);
        }
    }
}
