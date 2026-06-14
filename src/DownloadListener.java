public interface DownloadListener {
    default void onProgress(int progress) {}
    default void onError(String errorMessage) {}
    default void onComplete(boolean success) {}
    default void onOutput(String output) {}
    default void onMessage(String message) {}
}