import java.lang.ProcessBuilder;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher; import java.util.regex.Pattern;

public class downloadManager
{


    public void Download(String commandLine, downloadListener listener)
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
        System.out.println("Parsed Executable: " + command.get(0));
        System.out.println("Total Arguments: " + (command.size() - 1));


        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);

        try {
            Process process = processBuilder.start();

            new Thread(() -> {
                try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = stderr.readLine()) != null) {
                        System.err.println("yt-dlp Error: " + errorLine);

                        if (errorLine.toLowerCase().contains("error") && listener!=null)
                        {
                            listener.onError(errorLine);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            

            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String outputLine;
            while ((outputLine = stdout.readLine()) != null) {
                System.out.println("yt-dlp Output: " + outputLine);
                int precent = calculateProgress(outputLine);
                String objective = calcutateObjective(outputLine); 
                if (listener != null) {
                    listener.onProgress(precent);
                    listener.onMessage(objective);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Download completed successfully.");
                if (listener != null) {
                    listener.onComplete(true);
                }
            } else {
                System.out.println("Download failed with exit code: " + exitCode);
                if (listener != null) {
                    listener.onComplete(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
