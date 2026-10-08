import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** Cached grayscale spectrogram. Painting never performs audio analysis. */
public class FftLong extends JPanel {
    private final BufferedImage image;
    public FftLong(BufferedImage image) { this.image=image; setBackground(Color.BLACK); }
    public static BufferedImage render(SampleAudio audio) {
        int n=2048, hop=512;
        int windows=Math.max(1,1+(Math.max(0,audio.mono.length-n)+hop-1)/hop);
        int width=Math.min(1200,windows), height=n/2;
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
        for(int column=0;column<width;column++) {
            int window=width==1?0:(int)((long)column*(windows-1)/(width-1));
            double[] spectrum=SampleAudio.magnitudes(audio.mono,Math.min(audio.mono.length,window*hop),n);
            for(int bin=0;bin<height;bin++) {
                // Fixed 80 dB range, referenced to a full-scale Hann-windowed sine.
                double db=20*Math.log10(Math.max(1e-12,spectrum[bin]/(n/4.0)));
                int gray=(int)Math.round(255*Math.max(0,Math.min(1,(db+80)/80)));
                result.getRaster().setSample(column,height-1-bin,0,gray);
            }
        }
        return result;
    }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image,45,10,Math.max(1,getWidth()-55),Math.max(1,getHeight()-40),null);
        g.setColor(Color.WHITE);
        g.drawString("Time → (whole sample)",50,getHeight()-10);
        g.drawString("Hz ↑",5,25);
    }
}
