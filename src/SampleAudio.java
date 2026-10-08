import javax.sound.sampled.*;
import java.io.*;

/** Immutable signed 16-bit little-endian PCM used for playback and analysis. */
public final class SampleAudio {
    public final byte[] bytes;
    public final AudioFormat format;
    public final double[] mono, spectrum;
    public final double pitch;
    private SampleAudio(byte[] bytes, AudioFormat format) {
        this.bytes = bytes; this.format = format;
        mono = new double[frames()];
        for (int i=0; i<mono.length; i++) {
            for (int c=0; c<format.getChannels(); c++) mono[i] += value(i,c)/32768.0;
            mono[i] /= format.getChannels();
        }
        int start = energeticStart(mono, 8192);
        spectrum = magnitudes(mono, start, 8192);
        pitch = detectPitch(mono, start, format.getSampleRate());
    }
    public static SampleAudio read(AudioInputStream source) throws Exception {
        AudioFormat input = source.getFormat();
        AudioFormat target = new AudioFormat(input.getSampleRate(),16,input.getChannels(),true,false);
        try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, source)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192 * target.getFrameSize()];
            int n;
            while ((n=pcm.read(buffer))!=-1) if(n>0) out.write(buffer,0,n);
            return new SampleAudio(out.toByteArray(),target);
        }
    }
    public int frames() { return bytes.length / format.getFrameSize(); }
    private int value(int frame,int channel) {
        int i=frame*format.getFrameSize()+channel*2;
        return (short)((bytes[i]&255) | (bytes[i+1]<<8));
    }
    public void interpolate(double position, byte[] output,int offset) {
        int a=(int)position, b=Math.min(a+1,frames()-1);
        double fraction=position-a;
        for(int c=0;c<format.getChannels();c++) {
            int v=(int)Math.round(value(a,c)*(1-fraction)+value(b,c)*fraction);
            output[offset+c*2]=(byte)v; output[offset+c*2+1]=(byte)(v>>8);
        }
    }
    static int energeticStart(double[] data,int window) {
        int best=0; double max=0;
        for(int start=0;start<data.length;start+=window) {
            double energy=0;
            for(int i=start;i<Math.min(data.length,start+window);i++) energy+=data[i]*data[i];
            if(energy>max) { max=energy; best=start; }
        }
        return best;
    }
    static double[] magnitudes(double[] data,int start,int n) {
        Complex[] x=new Complex[n];
        double mean=0; int count=Math.min(n,data.length-start);
        for(int i=0;i<count;i++) mean+=data[start+i];
        mean/=Math.max(1,count);
        for(int i=0;i<n;i++) x[i]=new Complex(i<count ? (data[start+i]-mean)*(0.5-0.5*Math.cos(2*Math.PI*i/(n-1))) : 0,0);
        Complex[] y=FFT.fft(x); double[] result=new double[n/2];
        for(int i=0;i<result.length;i++) result[i]=y[i].abs();
        return result;
    }
    /** Normalised autocorrelation: picks the earliest strong period, not the loudest harmonic. */
    static double detectPitch(double[] data,int start,double rate) {
        int count=Math.min(8192,data.length-start);
        int min=Math.max(2,(int)(rate/2000)), max=Math.min(count/2,(int)(rate/40));
        if(max<=min) return 0;
        double mean=0,energy=0;
        for(int i=0;i<count;i++) mean+=data[start+i];
        mean/=count;
        double[] x=new double[count];
        for(int i=0;i<count;i++) { x[i]=data[start+i]-mean; energy+=x[i]*x[i]; }
        if(energy/count<1e-8) return 0;
        double[] corr=new double[max+1]; double best=-1;
        for(int lag=min;lag<=max;lag++) {
            double xy=0,xx=0,yy=0;
            for(int i=0;i<count-lag;i++) { xy+=x[i]*x[i+lag]; xx+=x[i]*x[i]; yy+=x[i+lag]*x[i+lag]; }
            corr[lag]=xy/Math.sqrt(Math.max(1e-30,xx*yy)); best=Math.max(best,corr[lag]);
        }
        if(best<0.65) return 0;
        for(int lag=min+1;lag<max;lag++) {
            if(corr[lag]>=Math.max(0.65,best*0.95) && corr[lag]>corr[lag-1] && corr[lag]>=corr[lag+1]) {
                double denominator=corr[lag-1]-2*corr[lag]+corr[lag+1];
                double shift=denominator==0?0:0.5*(corr[lag-1]-corr[lag+1])/denominator;
                return rate/(lag+shift);
            }
        }
        return 0;
    }
}
