import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.util.Arrays;
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
		int fftSize=(int)Math.pow(2,(int)11);
		Complex[][] y = new Complex[linesEnd][];
		for (int m= 0; m < linesEnd; m+=1) {
			linesStart = m;

			Complex[] x = new Complex[fftSize];
			double tv = 0.0;
			for (int i = 0, j=linesStart; i < fftSize; i++,j++) {
				if (j >= linesEnd-1) {
					x[i] = new Complex((double)tv, i / 44100.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(linesEnd)).getP1()).getX());
				} else {
					x[i] = new Complex((double)(((Line2D.Double) lines.get(j)).getP1()).getY()-23.0, i / 44100.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(i+linesStart)).getP1()).getX());
					////System.out.println(x[i].toString()); 
					tv = (((Line2D.Double) lines.get(j)).getP1()).getY()-23.0;
				}
			}
			y[m] = FFT.fft(x);
		}
		double minimum = Double.MAX_VALUE;
		double maximum = Double.MIN_VALUE;
		// work out max and min
		for (int l = 0; l < y.length; l++) {
			for (int k = 0; k < y[0].length; k+= 1) {
				if (minimum > y[l][k].abs()) {
					minimum = y[l][k].abs();
				}
				if (maximum < y[l][k].abs()) {
					maximum = y[l][k].abs();
				}
			}

		}
		System.out.println("max: "+maximum+", min: "+minimum);
		// work out histogram
		long histogram[] = new long[100001];
		long all = 0;
		for (int l = 0; l < y.length; l++) {
			for (int k = 0; k < y[0].length; k+= 1) {
				double normalisedValue = (y[l][k].abs() - minimum)/(maximum - minimum);
				histogram[(int) Math.round(normalisedValue * (histogram.length-1))] = histogram[(int) Math.round(normalisedValue * (histogram.length-1))] + 1;
				all++;
			}
		}
		//System.out.println("histogram: "+Arrays.toString(histogram));
		//integrate and work out 95% spot
		long top = (long) (all * (95.0/100.0));
		System.out.println("all: "+all+", top: "+top);

		long sum = 0;
		int l = 0;
		for (; l < histogram.length; l++) {
			sum += histogram[l];
			if (sum >= top ) {
				break;
			}
		}
		System.out.println("l: "+l);

		//draw
		for (int j = 0; j < y.length; j++) {
			for (int k = 0; k < y[0].length; k+= 1) {
				double scaledValue = 255 * (histogram.length-1) * (y[j][k].abs() - minimum)/((maximum - minimum) * (double)(l));
				//System.out.print("sv: "+scaledValue+", ");
				scaledValue = (scaledValue > 255.0) ? 255.0 : scaledValue;
				scaledValue = (scaledValue < 0.0) ? 0.0 : scaledValue;
				Color c = new Color(0xff&(int)Math.floor(scaledValue), 0xff&(int)Math.floor(scaledValue), 0xff&(int)Math.floor(scaledValue));
				g.setColor(c);
				g.drawLine(k, j, k+1, j+1);
			}
		}
		System.out.println("finished drawing!!");
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