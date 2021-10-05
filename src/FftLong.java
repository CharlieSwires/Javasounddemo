import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class FftLong extends JPanel{
    /**
     * 
     */
    private static final long serialVersionUID = 4007747056706891244L;
    static JButton go = new JButton("go");
    static JFrame fftjfrm = new JFrame("Sliding Fast Fourier Transform");
    static int width = 1000;
    static int height = 65536/100;
    boolean start = false;
    static FftLong fftLong;
    static int linesEnd;
    static int linesStart;
    static Vector lines = null;
    static PaintDemo pd = null;
    
    public FftLong(Vector l) {
        fftLong = this;
        lines = l;
        linesStart = 0;
        linesEnd = lines.size();
        pd = new PaintDemo();
        
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int height = getHeight();
        int width = getWidth();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        int fftSize=(int)Math.pow(2,(int)15);
        for (int m= 0; m < linesEnd; m+=1) {
            linesStart = m;

            Complex[] x = new Complex[fftSize];
            double tv = 0.0;
            for (int i = 0, j=linesStart; i < fftSize; i++,j++) {
                if (j >= linesEnd-1) {
                    x[i] = new Complex((double)tv,0.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(linesEnd)).getP1()).getX());
                } else {
                    x[i] = new Complex((double)(((Line2D.Double) lines.get(j)).getP1()).getY()-23.0,0.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(i+linesStart)).getP1()).getX());
                    ////System.out.println(x[i].toString()); 
                    tv = (((Line2D.Double) lines.get(j)).getP1()).getY()-23.0;
                }
            }
            Complex[] y = FFT.fft(x);
            for (int k = 0; k < fftSize; k+= 100) {
                double sum = 0.0;
                for (int n= 0; n< 100 && (k+n)<fftSize;n++) {
                    sum += y[k+n].abs();
                }
                double ave = (sum / 100.0);
                ave = ave < 256.0 ? ave : 255.0;
                Color c = new Color(((int)(ave)),((int)(ave)),((int)(ave)));
                g.setColor(c);
                g.drawLine(m/1, k/100, m/1, k/100+1);
                
            }
        }
    }

    public class PaintDemo{

        PaintDemo(){
            fftjfrm.setSize(width, height);
            fftjfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


            fftjfrm.setLayout(new BorderLayout());

            JPanel temp = new JPanel();
            //            go.addActionListener(new ActionListener() {
            //
            // 
            //                @Override
            //                public void actionPerformed(ActionEvent e) {
            //                    start = !start;   
            //                    
            //                }
            //
            //            });
            temp.add(go);

            fftjfrm.add(temp, BorderLayout.NORTH);
            fftjfrm.add(fftLong, BorderLayout.CENTER);

            fftjfrm.setVisible(true);
        }
    }
}