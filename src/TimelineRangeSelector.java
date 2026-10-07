import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TimelineRangeSelector extends JPanel
// Handles the formation of the timeline and grabs values from it.
{
    
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

    // The portion of the video (0 to 100 percent) currently shown on the track, shrinks when zoomed
    private double viewStart = 0.0;
    private double viewEnd = 100.0;

    // Holding a box still for 1 second zooms in, holdAnchorX is where the box was last held still
    private final Timer holdTimer = new Timer(1000, e -> zoomIn());
    private int holdAnchorX = 0;

    private ThemeManager theme = ThemeManager.getInstance();

    public TimelineRangeSelector() {
        setPreferredSize(new Dimension(500, 60));
        holdTimer.setRepeats(false);

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

                if (activeBox != 0)
                {
                    holdAnchorX = mouseX;
                    holdTimer.restart();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                activeBox = 0;
                holdTimer.stop();
                viewStart = 0.0;
                viewEnd = 100.0;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (activeBox == 0) return;

                // Moving more than a few pixels counts as not holding still, so restart the zoom countdown
                if (Math.abs(e.getX() - holdAnchorX) > 3)
                {
                    holdAnchorX = e.getX();
                    holdTimer.restart();
                }

                int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
                if (trackW <= 0) return;

                int targetPixel = e.getX() - dragOffset;

                // Clamp in percent space since the other box may be off screen while zoomed
                if (activeBox == 1) {
                    double fraction = Math.max(0.0, Math.min(1.0, (targetPixel - endBarWidth) / (double) trackW));
                    leftPercent = Math.min(viewStart + fraction * (viewEnd - viewStart), rightPercent);
                } else if (activeBox == 2) {
                    double fraction = Math.max(0.0, Math.min(1.0, (targetPixel - endBarWidth - boxWidth) / (double) trackW));
                    rightPercent = Math.max(viewStart + fraction * (viewEnd - viewStart), leftPercent);
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
        return (int) ((leftPercent / 100.0) * videoDuration);
    }
    
    public double getEndTime() {
        if (videoDuration <= 0)
        {
            return rightPercent;
        }
        return (int) ((rightPercent / 100.0) * videoDuration);
    }

    private int getLeftBoxX() {
        int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
        if (trackW <= 0) return endBarWidth;
        return endBarWidth + (int) (((leftPercent - viewStart) / (viewEnd - viewStart)) * trackW);
    }

    private int getRightBoxX() {
        int trackW = getWidth() - (2 * endBarWidth) - (2 * boxWidth);
        if (trackW <= 0) return endBarWidth + boxWidth;
        return endBarWidth + (int) (((rightPercent - viewStart) / (viewEnd - viewStart)) * trackW) + boxWidth;
    }

    private boolean isInView(double percent)
    {
        return percent >= viewStart && percent <= viewEnd;
    }

    // Zooms the track in 10x while keeping the held box under the cursor
    private void zoomIn()
    {
        if (activeBox == 0) return;

        double span = viewEnd - viewStart;

        // Stop zooming once about 10 seconds of video are showing
        double minSpan = 0.5; // fallback when no url/duration
        if (videoDuration > 0)
        {
            minSpan = (10.0 / videoDuration) * 100.0;
        }

        double newSpan = Math.max(span / 10.0, minSpan);
        if (newSpan >= span) return; // Already zoomed in as far as allowed

        double heldPercent = leftPercent;
        if (activeBox == 2)
        {
            heldPercent = rightPercent;
        }
        double fraction = (heldPercent - viewStart) / span;

        viewStart = heldPercent - fraction * newSpan;
        viewStart = Math.max(0.0, Math.min(viewStart, 100.0 - newSpan));
        viewEnd = viewStart + newSpan;

        repaint();
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
            return String.format("%d:%02d:%02d", hours, minutes, seconds); 
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

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int lX = getLeftBoxX();
        int rX = getRightBoxX();
        
        int centerY = (getHeight() - boxHeight) / 2;
        int endBarY = (getHeight() - endBarHeight) / 2;
        int trackHeight = 10;
        int trackY = (getHeight() - trackHeight) / 2;

        // Draw the static grey bars at the extreme edges
        g2d.setColor(theme.getColor("timelineEndBar"));
        g2d.fillRect(0, endBarY, endBarWidth, endBarHeight);
        g2d.fillRect(getWidth() - endBarWidth, endBarY, endBarWidth, endBarHeight);

        boolean zoomed = (viewEnd - viewStart) < 100.0;
        boolean leftVisible = isInView(leftPercent);
        boolean rightVisible = isInView(rightPercent);

        // Draw the background timeline track (Light Gray, Light Red when zoomed) offset by endBarWidth
        if (zoomed)
        {
            g2d.setColor(theme.getColor("timelineTrackZoomed"));
        }
        else
        {
            g2d.setColor(theme.getColor("timelineTrack"));
        }
        g2d.fillRect(endBarWidth, trackY, getWidth() - (2 * endBarWidth), trackHeight);

        // Draw the highlighted "selected" range between the two boxes (Blue), clipped to the visible track
        g2d.setColor(theme.getColor("timelineSelection"));
        int selectionStartX = Math.max(lX + boxWidth, endBarWidth);
        int selectionEndX = Math.min(rX, getWidth() - endBarWidth);
        if (selectionEndX > selectionStartX)
        {
            g2d.fillRect(selectionStartX, trackY, selectionEndX - selectionStartX, trackHeight);
        }

        // Draw the two draggable boxes (Dark Gray), skipping any zoomed out of view
        g2d.setColor(theme.getColor("timelineHandle"));
        if (leftVisible) g2d.fillRect(lX, centerY, boxWidth, boxHeight);
        if (rightVisible) g2d.fillRect(rX, centerY, boxWidth, boxHeight);

        // Draw the dynamic text above each box
        g2d.setColor(theme.getColor("timelineText"));
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // While zoomed, show the start and end time of the visible slice under the track ends
        if (zoomed)
        {
            String viewEndText = formatTime(viewEnd);
            int viewLabelY = getHeight() - 2;
            g2d.drawString(formatTime(viewStart), 0, viewLabelY);
            g2d.drawString(viewEndText, getWidth() - fm.stringWidth(viewEndText), viewLabelY);
        }

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

        if (leftVisible) g2d.drawString(leftText, leftTextX, leftTextY);
        if (rightVisible) g2d.drawString(rightText, rightTextX, rightTextY);
    }
    
    public void setVideoDuration(double duration)
    {
        System.out.println("Grabbed video duration = " + duration + " seconds");

        this.videoDuration = (int) duration;
        repaint();
    }
}