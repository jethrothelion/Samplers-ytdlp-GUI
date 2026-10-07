public class Main 
{

    public static void main(String[] args)
    {
        ThemeManager.getInstance().applyTheme(); // before any window so it picks up the colors
        DownloadGUI GUI = new DownloadGUI();
        GUI.initialization();
    }
}
