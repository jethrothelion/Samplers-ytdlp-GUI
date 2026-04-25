public interface DownloadListener {
    void onProgress(int progress);
    void onError(String errorMessage);
    void onComplete(boolean success);
    void onOutput(String output);
    void onMessage(String message);
    
    
}