public interface downloadListener {
    void onProgress(int progress);
    void onError(String errorMessage);
    void onComplete(boolean success);
    void onMessage(String message);
    
    
}