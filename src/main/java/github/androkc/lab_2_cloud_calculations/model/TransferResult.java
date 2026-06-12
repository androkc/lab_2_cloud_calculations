package github.androkc.lab_2_cloud_calculations.model;

public class TransferResult {
    
    public enum Status {
        SUCCESS,
        FILE_EXISTS,
        ERROR,
        ACCESS_DENIED,
        NO_SPACE
    }
    
    private Status status;
    private String message;
    private String fileName;
    
    public TransferResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public TransferResult(Status status, String message, String fileName) {
        this.status = status;
        this.message = message;
        this.fileName = fileName;
    }
    
    public static TransferResult success(String fileName) {
        return new TransferResult(Status.SUCCESS, "Файл успешно скопирован: " + fileName, fileName);
    }
    
    public static TransferResult fileExists(String fileName) {
        return new TransferResult(Status.FILE_EXISTS, "Файл уже существует: " + fileName, fileName);
    }
    
    public static TransferResult error(String message) {
        return new TransferResult(Status.ERROR, message);
    }
    
    public static TransferResult accessDenied(String fileName) {
        return new TransferResult(Status.ACCESS_DENIED, "Доступ запрещён: " + fileName, fileName);
    }
    
    public static TransferResult noSpace(String fileName) {
        return new TransferResult(Status.NO_SPACE, "Недостаточно места: " + fileName, fileName);
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean isFileExists() {
        return status == Status.FILE_EXISTS;
    }
    
    public boolean isError() {
        return status == Status.ERROR || status == Status.ACCESS_DENIED || status == Status.NO_SPACE;
    }
}
