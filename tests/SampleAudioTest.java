import javax.sound.sampled.*;
import java.io.*;
public class SampleAudioTest {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static SampleAudio tone(int rate,int channels,int bits,boolean big,boolean signed,double hz) throws Exception {
        int frames=rate/2, stride=channels*bits/8; byte[] bytes=new byte[frames*stride];
        for(int i=0;i<frames;i++) for(int c=0;c<channels;c++) {
            double t=2*Math.PI*hz*i/rate;
            // Louder second harmonic must not become the fundamental.
            double v=0.15*Math.sin(t)+0.5*Math.sin(2*t);
            int offset=i*stride+c*bits/8;
            if(bits==8) bytes[offset]=(byte)((int)(v*127)+(signed?0:128));
            else { int value=(int)(v*32767); bytes[offset]=(byte)(big?value>>8:value); bytes[offset+1]=(byte)(big?value:value>>8); }
        }
        AudioFormat f=new AudioFormat(rate,bits,channels,signed,big);
        return SampleAudio.read(new AudioInputStream(new ByteArrayInputStream(bytes),f,frames));
    }
    public static void main(String[] args) throws Exception {
        for(int rate:new int[]{8000,22050,44100,48000}) for(int channels:new int[]{1,2}) {
            SampleAudio a=tone(rate,channels,16,false,true,440);
            check(Math.abs(a.pitch-440)<2,"Pitch at "+rate+"/"+channels+": "+a.pitch);
            byte[] frame=new byte[a.format.getFrameSize()]; a.interpolate(10.5,frame,0);
            check(a.frames()==rate/2,"Frame count");
            check(FftLong.render(a).getWidth()<=1200,"Bounded FFT image");
        }
        check(Math.abs(tone(44100,1,8,false,false,220).pitch-220)<2,"Unsigned 8-bit");
        check(Math.abs(tone(44100,2,16,true,true,330).pitch-330)<2,"Big endian");
        AudioFormat f=new AudioFormat(44100,16,1,true,false);
        SampleAudio silence=SampleAudio.read(new AudioInputStream(new ByteArrayInputStream(new byte[20000]),f,10000));
        check(silence.pitch==0,"Silence rejected"); FftLong.render(silence);
        SampleAudio empty=SampleAudio.read(new AudioInputStream(new ByteArrayInputStream(new byte[0]),f,0));
        check(empty.pitch==0,"Empty rejected"); FftLong.render(empty);
        CapturePlayback panel=new CapturePlayback();
        check(panel.slidingFrame==null,"No automatic FFT frame");
        check(panel.slidingFftB.getText().equals("Sliding FFT..."),"Explicit button");
        panel.sample=tone(44100,2,16,false,true,440);
        panel.audioInputStream=new AudioInputStream(new ByteArrayInputStream(panel.sample.bytes),panel.sample.format,panel.sample.frames());
        panel.samplingGraph.setSize(504,160); panel.fft=true;
        java.awt.image.BufferedImage canvas=new java.awt.image.BufferedImage(504,160,1);
        java.awt.Graphics graphics=canvas.getGraphics();
        panel.samplingGraph.paint(graphics); panel.samplingGraph.paint(graphics); graphics.dispose();
        check(panel.slidingFrame==null,"Repainting FFT never opens a frame");
        System.out.println("PASS: pitch, formats, interpolation, silence, empty sample, bounded spectrogram and FFT button");
    }
}
