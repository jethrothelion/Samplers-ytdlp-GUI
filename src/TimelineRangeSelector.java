import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TimelineRangeSelector extends JPanel {
    
    // The absolute percentages (0 to 100) are the default value
    private double leftPercent = 0.0;
    private double rightPercent = 100.0;
    
    // Dimensions for the draggable boxes
    private final int boxWidth = 8;
    private final int boxHeight = 20;

    // Dimensions for the static grey bars at the ends
    private final int endBarWidth = 4;
    private final int endBarHeight = 24;

    private int videoDuration = -1; // -1 means unknown duration
    
    // Tracks which box is being dragged (0 = none, 1 = left, 2 = right)
    private int activeBox = 0;

    // Remembers where inside the box the user clicked to prevent cursor snapping
    private int dragOffset = 0;

    public TimelineRangeSelector() {
        setPreferredSize(new Dimension(500, 60));
        
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mouseX = e.getX();
                int lX = getLeftBoxX();
                int rX = getRightBoxX();
                
                if (mouseX >= lX && mouseX <= lX + boxWidth) {
                    activeBox = 1;
                    dragOffset = mouseX - lX;
                } else if (mouseX >= rX && mouseX <= rX + boxWidth) {
                    activeBox = 2;
                    dragOffset = mouseX - rX;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                activeBox = 0; 
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (activeBox == 0) return; 

                int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
                if (trackW <= 0) return;

                int targetPixel = e.getX() - dragOffset;
                
                if (activeBox == 1) {
                    int minPixel = endBarWidth;
                    int maxPixel = getRightBoxX() - boxWidth;
                    int boundedPixel = Math.max(minPixel, Math.min(targetPixel, maxPixel));
                    leftPercent = ((boundedPixel - endBarWidth) / (double) trackW) * 100.0;
                } else if (activeBox == 2) {
                    int minPixel = getLeftBoxX() + boxWidth;
                    int maxPixel = getWidth() - endBarWidth - boxWidth;
                    int boundedPixel = Math.max(minPixel, Math.min(targetPixel, maxPixel));
                    rightPercent = ((boundedPixel - endBarWidth - boxWidth) / (double) trackW) * 100.0;
                }
                
                repaint(); 

                TimelineRangeSelector.this.firePropertyChange("rangeChanged", false, true);
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public boolean isFullRangeSelected() 
    {
        if(leftPercent == 0.0 && rightPercent == 100.0)
        {   
            return true;
        }
        else return false;
    }

    public double getStartTime() {
        if (videoDuration <= 0)
        {
            return leftPercent; //fallback when no url/duration
        }
        return (int) (leftPercent / 100.0) * videoDuration;
    }
    
    public double getEndTime() {
        if (videoDuration <= 0)
        {
            return rightPercent;
        }
        return (int) (rightPercent / 100.0) * videoDuration;
    }

    private int getLeftBoxX() {
        int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
        if (trackW <= 0) return endBarWidth;
        return endBarWidth + (int) ((leftPercent / 100) * trackW);
    }

    private int getRightBoxX() {
        int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
        if (trackW <= 0) return endBarWidth + boxWidth;
        return endBarWidth + (int) ((rightPercent / 100) * trackW) + boxWidth;
    }

    private String formatTime(double percent)
    {
        if (videoDuration <= 0)
        {
            // Fallback to percentage if the duration hasn't loaded yet
            return String.format("%.0f%%", percent);
        }
         // Calculate the actual seconds based on the slider's percentage
        int totalSeconds = (int) ((percent / 100.0) * videoDuration);

        int hours = totalSeconds / 3600;

        int minutes = (totalSeconds % 3600)/60;

        int seconds = totalSeconds % 60;

        if (hours > 0 )
        {
            return String.format("%d:$02d:%02d", hours, minutes, seconds); 
        }
        else
        {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (getWidth() <= 0) return;

        // Turn on Anti-Aliasing for smooth shapes and text
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int lX = getLeftBoxX();
        int rX = getRightBoxX();
        
        int centerY = (getHeight() - boxHeight) / 2;
        int endBarY = (getHeight() - endBarHeight) / 2;
        int trackHeight = 10;
        int trackY = (getHeight() - trackHeight) / 2;

        // 1. Draw the static grey bars at the extreme edges
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, endBarY, endBarWidth, endBarHeight);
        g2d.fillRect(getWidth() - endBarWidth, endBarY, endBarWidth, endBarHeight);

        // 2. Draw the background timeline track (Light Gray) offset by endBarWidth
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(endBarWidth, trackY, getWidth() - (2 * endBarWidth), trackHeight);

        // 3. Draw the highlighted "selected" range between the two boxes (Blue)
        g2d.setColor(new Color(100, 150, 255));
        int selectionStartX = lX + boxWidth;
        int selectionWidth = rX - selectionStartX;
        g2d.fillRect(selectionStartX, trackY, selectionWidth, trackHeight);

        // 4. Draw the two draggable boxes (Dark Gray)
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(lX, centerY, boxWidth, boxHeight);
        g2d.fillRect(rX, centerY, boxWidth, boxHeight);

        // 5. Draw the dynamic text above each box
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        
        String leftText = formatTime(leftPercent);
        String rightText = formatTime(rightPercent);
        
        int leftTextWidth = fm.stringWidth(leftText);
        int rightTextWidth = fm.stringWidth(rightText);
        
        int leftTextX = lX + (boxWidth - leftTextWidth) / 2;
        leftTextX = Math.max(0, leftTextX); 
        
        int rightTextX = rX + (boxWidth - rightTextWidth) / 2;
        rightTextX = Math.min(getWidth() - rightTextWidth, rightTextX); 
        
        int leftTextY = centerY - 5; 
        int rightTextY = centerY - 5; 

        // Shift the right box's text below the timeline if they overlap
        if (rightTextX - leftTextX < 30) {
            // Push text below the box, but leave a few pixels of breathing room from the component edge
            rightTextY = centerY + boxHeight + fm.getAscent() + 2; 
        }

        g2d.drawString(leftText, leftTextX, leftTextY);
        g2d.drawString(rightText, rightTextX, rightTextY);
    }
    
    public void setVideoDuration(double duration)
    {
        System.out.println("Grabbed video duration = " + duration);

        this.videoDuration = (int) duration;
        repaint();
    }
}