import java.lang.ProcessBuilder;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher; import java.util.regex.Pattern;

public class DownloadManager
//Holds as much of the download handelling as feasable
{
    private Process activeProcess;

    public void Download(String commandLine, DownloadListener listener)
    {
    
        List<String> command = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(commandLine);
        while (m.find()) {
            if (m.group(1) != null) {
                command.add(m.group(1)); // Adds text inside quotes (without the quotes)
            } else {
                command.add(m.group(2)); // Adds normal words
            }
        }
        
        // Debugging print to prove the string was chopped up correctly
        if (listener != null) {
            listener.onOutput("Parsed Executable: " + command.get(0));
            listener.onOutput("Total Arguments: " + (command.size() - 1));
        }


        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);

        try {
            activeProcess = processBuilder.start();

            new Thread(() -> {
                try (BufferedReader stderr = new BufferedReader(new InputStreamReader(activeProcess.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = stderr.readLine()) != null) {
                        if(listener != null)
                        {
                            listener.onOutput("yt-dlp Log/Error: " + commandLine);
                        }
                    }
                } catch (Exception e) {
                    if (listener != null) listener.onOutput("Thread Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            

            BufferedReader stdout = new BufferedReader(new InputStreamReader(activeProcess.getInputStream()));
            String outputLine;
            while ((outputLine = stdout.readLine()) != null) {
                listener.onOutput(outputLine);

                int precent = calculateProgress(outputLine);
                String objective = calcutateObjective(outputLine); 
                if (listener != null) {
                    listener.onProgress(precent);
                    listener.onMessage(objective);
                }
            }

            int exitCode = activeProcess.waitFor();
            if (exitCode == 0) {
                System.out.println("Download completed successfully.");
                if (listener != null) {
                    listener.onOutput("Download completed successfully");
                    listener.onComplete(true);
                }
            } else {
                System.out.println("Download failed with exit code: " + exitCode);
                if (listener != null) {
                    listener.onOutput("Download failed with exit code: " + exitCode);
                    listener.onComplete(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void abortDownload()
    {
        if (activeProcess != null && activeProcess.isAlive())
        {
            activeProcess.destroy();
        }
    }


    public int calculateProgress(String ytDlpOutput)
    {
        // This regex looks for a number (which can have a decimal point) followed by a percent sign.
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)%");
        Matcher matcher = pattern.matcher(ytDlpOutput);
        if (matcher.find()) 
        {
            
            try {
                String match = matcher.group(1);

                return (int) Double.parseDouble(match);
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }
        return -1;//Return -1 instead of 0 when no percentage is found
    }
    public String calcutateObjective(String ytDlpOutput)
    {
        // this regex looks for any charecters inbetween [] 
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(ytDlpOutput); 
        if (matcher.find()) 
        {
            try
            {
                return matcher.group(1);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        
        return null;
    }
}
