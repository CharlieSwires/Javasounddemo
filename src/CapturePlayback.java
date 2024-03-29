/*
 * @(#)CapturePlayback.java	1.11	99/12/03
 *
 * Copyright (c) 1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */


import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;






/**
 *  Capture/Playback sample.  Record audio in different formats
 *  and then playback the recorded audio.  The captured audio can 
 *  be saved either as a WAVE, AU or AIFF.  Or load an audio file
 *  for streaming playback.
 *
 * @version @(#)CapturePlayback.java	1.11	99/12/03
 * @author Brian Lichtenwalter  
 */
public class CapturePlayback extends JPanel implements ActionListener, ControlContext {

    final int bufSize = 16384;
    final Color jfcBlue = new Color(204, 204, 255);
    final Color pink = new Color(255, 175, 175);
    boolean record;
    final int PROGRAM = 192;
    final int NOTEON = 144;
    final int NOTEOFF = 128;
    final int SUSTAIN = 64;
    final int REVERB = 91;
    final int ON = 0, OFF = 1;
    JTable table;
    Object o = new Object();

    FormatControls formatControls = new FormatControls();
    Capture capture = new Capture();
    Playback playback = new Playback();
    Playback2 playback2 = new Playback2();

    AudioInputStream audioInputStream;
    AudioInputStream audioInputStream2;
    SamplingGraph samplingGraph;

    JButton playB, captB, pausB, loadB, fftB;
    JButton auB, aiffB, waveB;
    JTextField textField;

    String fileName = "untitled";
    String errStr;
    double duration, seconds;
    File file;
    Vector lines = new Vector();
    Vector keys = new Vector();
    Vector whiteKeys = new Vector();
    ChannelData cc;    // current channel
    JCheckBox mouseOverCB = new JCheckBox("mouseOver", true);
    JSlider veloS, presS, bendS, revbS;
    JCheckBox soloCB, monoCB, muteCB, sustCB; 
    Track track;
    long startTime;
    Sequence sequence;
    Piano piano;
    boolean fft = false;


    public CapturePlayback() {
        setLayout(new BorderLayout());
        EmptyBorder eb = new EmptyBorder(5,5,5,5);
        SoftBevelBorder sbb = new SoftBevelBorder(SoftBevelBorder.LOWERED);
        setBorder(new EmptyBorder(5,5,5,5));

        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p1.add(formatControls);

        JPanel p2 = new JPanel();
        p2.setBorder(sbb);
        p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(10,0,5,0));
        playB = addButton("Play", buttonsPanel, false);
        captB = addButton("Record", buttonsPanel, true);
        pausB = addButton("Pause", buttonsPanel, false);
        loadB = addButton("Load...", buttonsPanel, true);
        fftB = addButton("Time", buttonsPanel, true);
        p2.add(buttonsPanel);

        JPanel samplingPanel = new JPanel(new BorderLayout());
        eb = new EmptyBorder(10,20,20,20);
        samplingPanel.setBorder(new CompoundBorder(eb, sbb));
        samplingPanel.add(samplingGraph = new SamplingGraph());
        p2.add(samplingPanel);

        JPanel savePanel = new JPanel();
        savePanel.setLayout(new BoxLayout(savePanel, BoxLayout.Y_AXIS));

        JPanel saveTFpanel = new JPanel();
        saveTFpanel.add(new JLabel("File to save:  "));
        saveTFpanel.add(textField = new JTextField(fileName));
        textField.setPreferredSize(new Dimension(140,25));
        savePanel.add(saveTFpanel);

        JPanel saveBpanel = new JPanel();
        auB = addButton("Save AU", saveBpanel, false);
        aiffB = addButton("Save AIFF", saveBpanel, false);
        waveB = addButton("Save WAVE", saveBpanel, false);
        savePanel.add(saveBpanel);

        p2.add(savePanel);
        p2.add(piano = new Piano());

        p1.add(p2);
        add(p1);
    }
    static byte[] data2;
    /**
     * given 120 bpm:
     *   (120 bpm) / (60 seconds per minute) = 2 beats per second
     *   2 / 1000 beats per millisecond
     *   (2 * resolution) ticks per second
     *   (2 * resolution)/1000 ticks per millisecond, or 
     *      (resolution / 500) ticks per millisecond
     *   ticks = milliseconds * resolution / 500
     */
    static int bufferLengthInFrames = 0;
    public void createShortEvent(int type, int num) {
        //        //System.out.println("type="+(type==NOTEON)+" num="+num);
        //System.out.println("bufferLengthInFrames="+bufferLengthInFrames);
        playback.stop();
        if (bufferLengthInFrames > 0) { 
            if (type==NOTEON) {
                synchronized(o) {
                    double fdest = Math.pow(2, (num-69)/12.0)*440.0;
                    double relativeSampleRate = (fdest/f0);
                    System.out.println("fdest="+fdest+" f0="+f0);
                    AudioFormat format = formatControls.getFormat();
                    //System.out.println("getFrameSize="+format.getFrameSize());
                    List<Byte> everything = new ArrayList<Byte>();
                    int frameSizeInBytes = format.getFrameSize();
                    int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
                    byte[] data = new byte[bufferLengthInBytes];
                    if (file != null) {
                        createAudioInputStream(file, false);
                    }
                    try {
                        audioInputStream.reset();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    byte[] audioBytes = new byte[
                                                 (int) (audioInputStream.getFrameLength() 
                                                         * format.getFrameSize())];
                    try {
                        audioInputStream.read(audioBytes);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    for(byte b: audioBytes) {
                        everything.add(b);
                    }

                    //            List<Byte> trimmed = new ArrayList<Byte>();
                    //            //System.out.println("linesStart="+linesStart+" linesEnd="+linesEnd);
                    //            for(int i = linesStart; i < linesEnd;i++) {
                    //                trimmed.add(everything.get(i*4));
                    //                trimmed.add(everything.get(i*4+1));
                    //                trimmed.add(everything.get(i*4+2));
                    //                trimmed.add(everything.get(i*4+3));
                    //            }
                    //            //System.out.println("trimmed.size()="+trimmed.size());
                    List<Byte> squished = new ArrayList<Byte>();
                    double loci =0.0;
                    for(; loci < everything.size()/4; loci += relativeSampleRate) {
                        int i = (int)loci * 4;
                        squished.add(everything.get(i));
                        squished.add(everything.get(i+1));
                        squished.add(everything.get(i+2));
                        squished.add(everything.get(i+3));

                    }

                    data2 = new byte[squished.size()];
                    for (int i = 0; i < squished.size(); i++) {
                        data2[i]=squished.get(i);
                    }

                    //System.out.println("data2.length="+data2.length);
                }
                playback2.start();
            } else {
                playback2.stop();
            }
        }
    }
    /**
     * Write data to the OutputChannel.
     */
    public class Playback2 implements Runnable {

        SourceDataLine line;
        Thread thread;
        int ii = 0;

        public void start() {
            errStr = null;
            thread = new Thread(this);
            thread.setName("Playback2");
            thread.start();
        }

        public void stop() {
            thread = null;
        }

        private void shutDown(String message) {
            if ((errStr = message) != null) {
                System.err.println(errStr);
                samplingGraph.repaint();
            }
            if (thread != null) {
                thread = null;
                samplingGraph.stop();
                captB.setEnabled(true);
                pausB.setEnabled(false);
                playB.setText("Play");
            } 
        }

        public void run() {

            //            // get an AudioInputStream of the desired format for playback
            AudioFormat format = formatControls.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, 
                    format);
            int retries = 0;
            // get and open the source data line for playback.
            while (thread != null && retries++ < 5) {
                ii = 0;
                try {
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format, bufSize);
                } catch (LineUnavailableException ex) { 
                    shutDown("Unable to open the line: " + ex);
                    return;
                }

                // play back the captured audio data
                int frameSizeInBytes = format.getFrameSize();
                int bufferLengthInBytes;
                byte[] data = null;
                int numBytesRead;

                if (line != null) {
                    bufferLengthInFrames = line.getBufferSize() / 8;

                    bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
                    data = new byte[bufferLengthInBytes];
                    numBytesRead = 0;

                    // start the source data line
                    line.start();
                }
                while (thread != null && line != null) {
                    try {
                        if ((numBytesRead = read(data)) == -1) {
                            break;
                        }
                        int numBytesRemaining = numBytesRead;
                        while (numBytesRemaining > 0 && line != null && data != null) {
                            numBytesRemaining -= line.write(data, 0, numBytesRemaining);
                            retries = 10;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        shutDown("Error during playback: " + e);
                        break;
                    }
                }
            }
            // we reached the end of the stream.  let the data play out, then
            // stop and close the line.
            if (thread != null && line != null) {
                line.drain();
            }
            if (line != null) {
                line.stop();
            }
            if (line != null) {
                line.close();    
            }

            line = null;
            shutDown(null);
        }

        private int read(byte[] data) {
            if (ii< data2.length) {
                int i=0;
                for (; i< data.length && ii< data2.length; i++,ii++) {
                    data[i] = data2[ii];
                }
                return i;

            } else {
                return -1;
            }
        }
    }
    /**
     * Stores MidiChannel information.
     */
    class ChannelData {

        MidiChannel channel;
        boolean solo, mono, mute, sustain;
        int velocity, pressure, bend, reverb;
        int row, col, num;

        public ChannelData(MidiChannel channel, int num) {
            this.channel = channel;
            this.num = num;
            velocity = pressure = bend = reverb = 64;
        }

        public void setComponentStates() {
            table.setRowSelectionInterval(row, row);
            table.setColumnSelectionInterval(col, col);

            soloCB.setSelected(solo);
            monoCB.setSelected(mono);
            muteCB.setSelected(mute);
            //sustCB.setSelected(sustain);

            JSlider slider[] = { veloS, presS, bendS, revbS };
            int v[] = { velocity, pressure, bend, reverb };
            for (int i = 0; i < slider.length; i++) {
                TitledBorder tb = (TitledBorder) slider[i].getBorder();
                String s = tb.getTitle();
                tb.setTitle(s.substring(0, s.indexOf('=')+1)+s.valueOf(v[i]));
                slider[i].repaint();
            }
        }
    } // End class ChannelData

    /**
     * Black and white keys or notes on the piano.
     */
    class Key extends Rectangle {
        int noteState = OFF;
        int kNum;
        public Key(int x, int y, int width, int height, int num) {
            super(x, y, width, height);
            kNum = num;
        }
        public boolean isNoteOn() {
            return noteState == ON;
        }
        public void on() {
            setNoteState(ON);
            //            cc.channel.noteOn(kNum, cc.velocity);
            //            if (record) {
            createShortEvent(NOTEON, kNum);
            //            }
        }
        public void off() {
            setNoteState(OFF);
            //            cc.channel.noteOff(kNum, cc.velocity);
            //            if (record) {
            createShortEvent(NOTEOFF, kNum);
            //            }
        }
        public void setNoteState(int state) {
            noteState = state;
        }
    } // End class Key

    /**
     * Piano renders black & white keys and plays the notes for a MIDI 
     * channel.  
     */
    class Piano extends JPanel implements MouseListener {

        Vector blackKeys = new Vector();
        Key prevKey;
        final int kw = 16, kh = 80;


        public Piano() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(42*kw, kh+1));
            int transpose = 24;  
            int whiteIDs[] = { 0, 2, 4, 5, 7, 9, 11 }; 

            for (int i = 0, x = 0; i < 6; i++) {
                for (int j = 0; j < 7; j++, x += kw) {
                    int keyNum = i * 12 + whiteIDs[j] + transpose;
                    whiteKeys.add(new Key(x, 0, kw, kh, keyNum));
                }
            }
            for (int i = 0, x = 0; i < 6; i++, x += kw) {
                int keyNum = i * 12 + transpose;
                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+1));
                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+3));
                x += kw;
                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+6));
                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+8));
                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+10));
            }
            keys.addAll(blackKeys);
            keys.addAll(whiteKeys);

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    if (mouseOverCB.isSelected()) {
                        Key key = getKey(e.getPoint());
                        if (prevKey != null && prevKey != key) {
                            prevKey.off();
                        } 
                        if (key != null && prevKey != key) {
                            key.on();
                        }
                        prevKey = key;
                        repaint();
                    }
                }
            });
            addMouseListener(this);
        }

        public void mousePressed(MouseEvent e) { 
            prevKey = getKey(e.getPoint());
            if (prevKey != null) {
                prevKey.on();
                repaint();
            }
        }
        public void mouseReleased(MouseEvent e) { 
            if (prevKey != null) {
                prevKey.off();
                repaint();
            }
        }
        public void mouseExited(MouseEvent e) { 
            if (prevKey != null) {
                prevKey.off();
                repaint();
                prevKey = null;
            }
        }
        public void mouseClicked(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }


        public Key getKey(Point point) {
            for (int i = 0; i < keys.size(); i++) {
                if (((Key) keys.get(i)).contains(point)) {
                    return (Key) keys.get(i);
                }
            }
            return null;
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Dimension d = getSize();

            g2.setBackground(getBackground());
            g2.clearRect(0, 0, d.width, d.height);

            g2.setColor(Color.white);
            g2.fillRect(0, 0, 42*kw, kh);

            for (int i = 0; i < whiteKeys.size(); i++) {
                Key key = (Key) whiteKeys.get(i);
                if (key.isNoteOn()) {
                    g2.setColor(record ? pink : jfcBlue);
                    g2.fill(key);
                }
                g2.setColor(Color.black);
                g2.draw(key);
            }
            for (int i = 0; i < blackKeys.size(); i++) {
                Key key = (Key) blackKeys.get(i);
                if (key.isNoteOn()) {
                    g2.setColor(record ? pink : jfcBlue);
                    g2.fill(key);
                    g2.setColor(Color.black);
                    g2.draw(key);
                } else {
                    g2.setColor(Color.black);
                    g2.fill(key);
                }
            }
        }
    } // End class Piano

    public void open() { }


    public void close() {
        if (playback.thread != null) {
            playB.doClick(0);
        }
        if (capture.thread != null) {
            captB.doClick(0);
        }
    }


    private JButton addButton(String name, JPanel p, boolean state) {
        JButton b = new JButton(name);
        b.addActionListener(this);
        b.setEnabled(state);
        p.add(b);
        return b;
    }


    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if (obj.equals(auB)) {
            saveToFile(textField.getText().trim(), AudioFileFormat.Type.AU);
        } else if (obj.equals(aiffB)) {
            saveToFile(textField.getText().trim(), AudioFileFormat.Type.AIFF);
        } else if (obj.equals(waveB)) {
            saveToFile(textField.getText().trim(), AudioFileFormat.Type.WAVE);
        } else if (obj.equals(playB)) {
            if (playB.getText().startsWith("Play")) {
                playback.start();
                samplingGraph.start();
                captB.setEnabled(false);
                pausB.setEnabled(true);
                playB.setText("Stop");
            } else {
                playback.stop();
                samplingGraph.stop();
                captB.setEnabled(true);
                pausB.setEnabled(false);
                playB.setText("Play");
            }
        } else if (obj.equals(captB)) {
            if (captB.getText().startsWith("Record")) {
                file = null;
                capture.start();
                fileName = "untitled";
                samplingGraph.start();
                loadB.setEnabled(false);
                playB.setEnabled(false);
                pausB.setEnabled(true);
                auB.setEnabled(false);
                aiffB.setEnabled(false);
                waveB.setEnabled(false);
                captB.setText("Stop");
            } else {
                lines.removeAllElements();  
                capture.stop();
                samplingGraph.stop();
                loadB.setEnabled(true);
                playB.setEnabled(true);
                pausB.setEnabled(false);
                auB.setEnabled(true);
                aiffB.setEnabled(true);
                waveB.setEnabled(true);
                captB.setText("Record");
            }
        } else if (obj.equals(pausB)) {
            if (pausB.getText().startsWith("Pause")) {
                if (capture.thread != null) {
                    capture.line.stop();
                } else {
                    if (playback.thread != null) {
                        playback.line.stop();
                    }
                }
                pausB.setText("Resume");
            } else {
                if (capture.thread != null) {
                    capture.line.start();
                } else {
                    if (playback.thread != null) {
                        playback.line.start();
                    }
                }
                pausB.setText("Pause");
            }
        } else if (obj.equals(loadB)) {
            try {
                File file = new File(System.getProperty("user.dir"));
                JFileChooser fc = new JFileChooser(file);
                fc.setFileFilter(new javax.swing.filechooser.FileFilter () {
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        String name = f.getName();
                        if (name.endsWith(".au") || name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".aif")) {
                            return true;
                        }
                        return false;
                    }
                    public String getDescription() {
                        return ".au, .wav, .aif";
                    }
                });

                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    createAudioInputStream(fc.getSelectedFile(), true);
                }
            } catch (SecurityException ex) { 
                JavaSound.showInfoDialog();
                ex.printStackTrace();
            } catch (Exception ex) { 
                ex.printStackTrace();
            } 
        }else if (obj.equals(fftB)) {
            fft = !fft;
            fftB.setText(fft ? "FFT" : "Time");
        }
    }


    public void createAudioInputStream(File file, boolean updateComponents) {
        if (file != null && file.isFile()) {
            try {
                this.file = file;
                errStr = null;
                audioInputStream = AudioSystem.getAudioInputStream(file);
                playB.setEnabled(true);
                fileName = file.getName();
                long milliseconds = (long)((audioInputStream.getFrameLength() * 1000) / audioInputStream.getFormat().getFrameRate());
                duration = milliseconds / 1000.0;
                auB.setEnabled(true);
                aiffB.setEnabled(true);
                waveB.setEnabled(true);
                if (updateComponents) {
                    formatControls.setFormat(audioInputStream.getFormat());
                    samplingGraph.createWaveForm(null);
                }
            } catch (Exception ex) { 
                reportStatus(ex.toString());
            }
        } else {
            reportStatus("Audio file required.");
        }
    }


    public void saveToFile(String name, AudioFileFormat.Type fileType) {

        if (audioInputStream == null) {
            reportStatus("No loaded audio to save");
            return;
        } else if (file != null) {
            createAudioInputStream(file, false);
        }

        // reset to the beginnning of the captured data
        try {
            audioInputStream.reset();
        } catch (Exception e) { 
            reportStatus("Unable to reset stream " + e);
            return;
        }

        File file = new File(fileName = name);
        try {
            if (AudioSystem.write(audioInputStream, fileType, file) == -1) {
                throw new IOException("Problems writing to file");
            }
        } catch (Exception ex) { reportStatus(ex.toString()); }
        samplingGraph.repaint();
    }


    private void reportStatus(String msg) {
        if ((errStr = msg) != null) {
            //System.out.println(errStr);
            samplingGraph.repaint();
        }
    }


    /**
     * Write data to the OutputChannel.
     */
    public class Playback implements Runnable {

        SourceDataLine line;
        Thread thread;

        public void start() {
            errStr = null;
            thread = new Thread(this);
            thread.setName("Playback");
            thread.start();
        }

        public void stop() {
            thread = null;
        }

        private void shutDown(String message) {
            if ((errStr = message) != null) {
                System.err.println(errStr);
                samplingGraph.repaint();
            }
            if (thread != null) {
                thread = null;
                samplingGraph.stop();
                captB.setEnabled(true);
                pausB.setEnabled(false);
                playB.setText("Play");
            } 
        }

        public void run() {

            // reload the file if loaded by file
            if (file != null) {
                createAudioInputStream(file, false);
            }

            // make sure we have something to play
            if (audioInputStream == null) {
                shutDown("No loaded audio to play back");
                return;
            }
            // reset to the beginnning of the stream
            try {
                audioInputStream.reset();
            } catch (Exception e) {
                shutDown("Unable to reset the stream\n" + e);
                return;
            }

            // get an AudioInputStream of the desired format for playback
            AudioFormat format = formatControls.getFormat();
            AudioInputStream playbackInputStream = AudioSystem.getAudioInputStream(format, audioInputStream);

            if (playbackInputStream == null) {
                shutDown("Unable to convert stream of format " + audioInputStream + " to format " + format);
                return;
            }

            // define the required attributes for our line, 
            // and make sure a compatible line is supported.

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, 
                    format);
            if (!AudioSystem.isLineSupported(info)) {
                shutDown("Line matching " + info + " not supported.");
                return;
            }

            // get and open the source data line for playback.

            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, bufSize);
            } catch (LineUnavailableException ex) { 
                shutDown("Unable to open the line: " + ex);
                return;
            }

            // play back the captured audio data

            int frameSizeInBytes = format.getFrameSize();
            bufferLengthInFrames = line.getBufferSize() / 8;
            int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            byte[] data = new byte[bufferLengthInBytes];
            int numBytesRead = 0;

            // start the source data line
            line.start();

            while (thread != null) {
                try {
                    if ((numBytesRead = playbackInputStream.read(data)) == -1) {
                        break;
                    }
                    int numBytesRemaining = numBytesRead;
                    while (numBytesRemaining > 0 ) {
                        numBytesRemaining -= line.write(data, 0, numBytesRemaining);
                    }
                } catch (Exception e) {
                    shutDown("Error during playback: " + e);
                    break;
                }
            }
            // we reached the end of the stream.  let the data play out, then
            // stop and close the line.
            if (thread != null) {
                line.drain();
            }
            line.stop();
            line.close();
            line = null;
            shutDown(null);
        }
    } // End class Playback


    /** 
     * Reads data from the input channel and writes to the output stream
     */
    class Capture implements Runnable {

        TargetDataLine line;
        Thread thread;

        public void start() {
            errStr = null;
            thread = new Thread(this);
            thread.setName("Capture");
            thread.start();
        }

        public void stop() {
            thread = null;
        }

        private void shutDown(String message) {
            if ((errStr = message) != null && thread != null) {
                thread = null;
                samplingGraph.stop();
                loadB.setEnabled(true);
                playB.setEnabled(true);
                pausB.setEnabled(false);
                auB.setEnabled(true);
                aiffB.setEnabled(true);
                waveB.setEnabled(true);
                captB.setText("Record");
                System.err.println(errStr);
                samplingGraph.repaint();
            }
        }

        public void run() {

            duration = 0;
            audioInputStream = null;

            // define the required attributes for our line, 
            // and make sure a compatible line is supported.

            AudioFormat format = formatControls.getFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, 
                    format);

            if (!AudioSystem.isLineSupported(info)) {
                shutDown("Line matching " + info + " not supported.");
                return;
            }

            // get and open the target data line for capture.

            try {
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format, line.getBufferSize());
            } catch (LineUnavailableException ex) { 
                shutDown("Unable to open the line: " + ex);
                return;
            } catch (SecurityException ex) { 
                shutDown(ex.toString());
                JavaSound.showInfoDialog();
                return;
            } catch (Exception ex) { 
                shutDown(ex.toString());
                return;
            }

            // play back the captured audio data
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int frameSizeInBytes = format.getFrameSize();
            bufferLengthInFrames = line.getBufferSize() / 8;
            int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            byte[] data = new byte[bufferLengthInBytes];
            int numBytesRead;

            line.start();

            while (thread != null) {
                if((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                    break;
                }
                out.write(data, 0, numBytesRead);
            }

            // we reached the end of the stream.  stop and close the line.
            line.stop();
            line.close();
            line = null;

            // stop and close the output stream
            try {
                out.flush();
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // load bytes into the audio input stream for playback

            byte audioBytes[] = out.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
            audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

            long milliseconds = (long)((audioInputStream.getFrameLength() * 1000) / format.getFrameRate());
            duration = milliseconds / 1000.0;

            try {
                audioInputStream.reset();
            } catch (Exception ex) { 
                ex.printStackTrace(); 
                return;
            }

            samplingGraph.createWaveForm(audioBytes);
        }
    } // End class Capture


    /**
     * Controls for the AudioFormat.
     */
    class FormatControls extends JPanel {

        Vector groups = new Vector();
        JToggleButton linrB, ulawB, alawB, rate8B, rate11B, rate16B, rate22B, rate44B;
        JToggleButton size8B, size16B, signB, unsignB, litB, bigB, monoB,sterB;

        public FormatControls() {
            setLayout(new GridLayout(0,1));
            EmptyBorder eb = new EmptyBorder(0,0,0,5);
            BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
            CompoundBorder cb = new CompoundBorder(eb, bb);
            setBorder(new CompoundBorder(cb, new EmptyBorder(8,5,5,5)));
            JPanel p1 = new JPanel();
            ButtonGroup encodingGroup = new ButtonGroup();
            linrB = addToggleButton(p1, encodingGroup, "linear", true);
            ulawB = addToggleButton(p1, encodingGroup, "ulaw", false);
            alawB = addToggleButton(p1, encodingGroup, "alaw", false);
            add(p1);
            groups.addElement(encodingGroup);

            JPanel p2 = new JPanel();
            JPanel p2b = new JPanel();
            ButtonGroup sampleRateGroup = new ButtonGroup();
            rate8B = addToggleButton(p2, sampleRateGroup, "8000", false);
            rate11B = addToggleButton(p2, sampleRateGroup, "11025", false);
            rate16B = addToggleButton(p2b, sampleRateGroup, "16000", false);
            rate22B = addToggleButton(p2b, sampleRateGroup, "22050", false);
            rate44B = addToggleButton(p2b, sampleRateGroup, "44100", true);
            add(p2);
            add(p2b);
            groups.addElement(sampleRateGroup);

            JPanel p3 = new JPanel();
            ButtonGroup sampleSizeInBitsGroup = new ButtonGroup();
            size8B = addToggleButton(p3, sampleSizeInBitsGroup, "8", false);
            size16B = addToggleButton(p3, sampleSizeInBitsGroup, "16", true);
            add(p3);
            groups.addElement(sampleSizeInBitsGroup);

            JPanel p4 = new JPanel();
            ButtonGroup signGroup = new ButtonGroup();
            signB = addToggleButton(p4, signGroup, "signed", true);
            unsignB = addToggleButton(p4, signGroup, "unsigned", false);
            add(p4);
            groups.addElement(signGroup);

            JPanel p5 = new JPanel();
            ButtonGroup endianGroup = new ButtonGroup();
            litB = addToggleButton(p5, endianGroup, "little endian", false);
            bigB = addToggleButton(p5, endianGroup, "big endian", true);
            add(p5);
            groups.addElement(endianGroup);

            JPanel p6 = new JPanel();
            ButtonGroup channelsGroup = new ButtonGroup();
            monoB = addToggleButton(p6, channelsGroup, "mono", false);
            sterB = addToggleButton(p6, channelsGroup, "stereo", true);
            add(p6);
            groups.addElement(channelsGroup);
        }

        private JToggleButton addToggleButton(JPanel p, ButtonGroup g, 
                String name, boolean state) {
            JToggleButton b = new JToggleButton(name, state);
            p.add(b);
            g.add(b);
            return b;
        }

        public AudioFormat getFormat() {

            Vector v = new Vector(groups.size());
            for (int i = 0; i < groups.size(); i++) {
                ButtonGroup g = (ButtonGroup) groups.get(i);
                for (Enumeration e = g.getElements();e.hasMoreElements();) {
                    AbstractButton b = (AbstractButton) e.nextElement();
                    if (b.isSelected()) {
                        v.add(b.getText());
                        break;
                    }
                }
            }

            AudioFormat.Encoding encoding = AudioFormat.Encoding.ULAW;
            String encString = (String) v.get(0);
            float rate = Float.valueOf((String) v.get(1)).floatValue();
            int sampleSize = Integer.valueOf((String) v.get(2)).intValue();
            String signedString = (String) v.get(3);
            boolean bigEndian = ((String) v.get(4)).startsWith("big");
            int channels = ((String) v.get(5)).equals("mono") ? 1 : 2;

            if (encString.equals("linear")) {
                if (signedString.equals("signed")) {
                    encoding = AudioFormat.Encoding.PCM_SIGNED;
                } else {
                    encoding = AudioFormat.Encoding.PCM_UNSIGNED;
                }
            } else if (encString.equals("alaw")) {
                encoding = AudioFormat.Encoding.ALAW;
            }
            return new AudioFormat(encoding, rate, sampleSize, 
                    channels, (sampleSize/8)*channels, rate, bigEndian);
        }


        public void setFormat(AudioFormat format) {
            AudioFormat.Encoding type = format.getEncoding();
            if (type == AudioFormat.Encoding.ULAW) {
                ulawB.doClick();
            } else if (type == AudioFormat.Encoding.ALAW) {
                alawB.doClick();
            } else if (type == AudioFormat.Encoding.PCM_SIGNED) {
                linrB.doClick(); signB.doClick(); 
            } else if (type == AudioFormat.Encoding.PCM_UNSIGNED) {
                linrB.doClick(); unsignB.doClick(); 
            }
            float rate = format.getFrameRate();
            if (rate == 8000) {
                rate8B.doClick();
            } else if (rate == 11025) {
                rate11B.doClick();
            } else if (rate == 16000) {
                rate16B.doClick();
            } else if (rate == 22050) {
                rate22B.doClick();
            } else if (rate == 44100) {
                rate44B.doClick();
            }
            switch (format.getSampleSizeInBits()) {
            case 8  : size8B.doClick(); break;
            case 16 : size16B.doClick(); break;
            }
            if (format.isBigEndian()) {
                bigB.doClick(); 
            } else { 
                litB.doClick();
            }
            if (format.getChannels() == 1) {
                monoB.doClick(); 
            } else { 
                sterB.doClick();
            }
        }
    } // End class FormatControls

    double f0 = 0.0;
    int linesStart;
    int linesEnd;
    /**
     * Render a WaveForm.
     */
    class SamplingGraph extends JPanel implements Runnable {

        private Thread thread;
        private Font font10 = new Font("serif", Font.PLAIN, 10);
        private Font font12 = new Font("serif", Font.PLAIN, 12);
        Color jfcBlue = new Color(204, 204, 255);
        Color pink = new Color(255, 175, 175);


        public SamplingGraph() {
            setBackground(new Color(20, 20, 20));
        }


        public void createWaveForm(byte[] audioBytes) {

            lines.removeAllElements();  // clear the old vector

            AudioFormat format = audioInputStream.getFormat();
            if (audioBytes == null) {
                try {
                    audioBytes = new byte[
                                          (int) (audioInputStream.getFrameLength() 
                                                  * format.getFrameSize())];
                    audioInputStream.read(audioBytes);
                } catch (Exception ex) { 
                    reportStatus(ex.toString());
                    return; 
                }
            }

            Dimension d = getSize();
            int w = d.width;
            int h = d.height-15;
            int[] audioData = null;
            if (format.getSampleSizeInBits() == 16) {
                int nlengthInSamples = audioBytes.length / 2;
                audioData = new int[nlengthInSamples];
                if (format.isBigEndian()) {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        /* First byte is MSB (high order) */
                        int MSB = (int) audioBytes[2*i];
                        /* Second byte is LSB (low order) */
                        int LSB = (int) audioBytes[2*i+1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                } else {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        /* First byte is LSB (low order) */
                        int LSB = (int) audioBytes[2*i];
                        /* Second byte is MSB (high order) */
                        int MSB = (int) audioBytes[2*i+1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                }
            } else if (format.getSampleSizeInBits() == 8) {
                int nlengthInSamples = audioBytes.length;
                audioData = new int[nlengthInSamples];
                if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i];
                    }
                } else {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i] - 128;
                    }
                }
            }

            int frames_per_pixel = audioBytes.length / format.getFrameSize()/w;
            byte my_byte = 0;
            double y_last = 0;
            int numChannels = format.getChannels();
            for (double x = 0; x < w && audioData != null; x++) {
                int idx = (int) (frames_per_pixel * numChannels * x);
                if (format.getSampleSizeInBits() == 8) {
                    my_byte = (byte) audioData[idx];
                } else {
                    my_byte = (byte) (128 * audioData[idx] / 32768 );
                }
                double y_new = (double) (h * (128 - my_byte) / 256);
                lines.add(new Line2D.Double(x, y_last, x, y_new));
                y_last = y_new;
            }

            repaint();
        }


        public void paint(Graphics g) {

            Dimension d = getSize();
            int w = d.width;
            int h = d.height;
            int INFOPAD = 15;

            Graphics2D g2 = (Graphics2D) g;
            g2.setBackground(getBackground());
            g2.clearRect(0, 0, w, h);
            g2.setColor(Color.white);
            g2.fillRect(0, h-INFOPAD, w, INFOPAD);

            if (errStr != null) {
                g2.setColor(jfcBlue);
                g2.setFont(new Font("serif", Font.BOLD, 18));
                g2.drawString("ERROR", 5, 20);
                AttributedString as = new AttributedString(errStr);
                as.addAttribute(TextAttribute.FONT, font12, 0, errStr.length());
                AttributedCharacterIterator aci = as.getIterator();
                FontRenderContext frc = g2.getFontRenderContext();
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                float x = 5, y = 25;
                lbm.setPosition(0);
                while (lbm.getPosition() < errStr.length()) {
                    TextLayout tl = lbm.nextLayout(w-x-5);
                    if (!tl.isLeftToRight()) {
                        x = w - tl.getAdvance();
                    }
                    tl.draw(g2, x, y += tl.getAscent());
                    y += tl.getDescent() + tl.getLeading();
                }
            } else if (capture.thread != null) {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("Length: " + String.valueOf(seconds), 3, h-4);
            } else {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("File: " + fileName + "  Length: " + String.valueOf(duration) + "  Position: " + String.valueOf(seconds), 3, h-4);

                if (audioInputStream != null && !fft) {
                    // .. render sampling graph ..
                    g2.setColor(jfcBlue);
                    for (int i = 1; i < lines.size(); i++) {
                        g2.draw((Line2D) lines.get(i));
                    }

                    // .. draw current position ..
                    if (seconds != 0) {
                        double loc = seconds/duration*w;
                        g2.setColor(pink);
                        g2.setStroke(new BasicStroke(3));
                        g2.draw(new Line2D.Double(loc, 0, loc, h-INFOPAD-2));
                    }
                } else if (audioInputStream != null && fft) {
                    linesStart = 0;
                    linesEnd = lines.size();
                    //                    for (int i = 1; i < lines.size(); i++) {
                    //                        if ((((Line2D.Double) lines.get(i)).getP1()).getY() > 25.0 ||
                    //                                (((Line2D.Double) lines.get(i)).getP1()).getY() < 21.0) {
                    //                            linesStart = i;
                    //                            break;
                    //                        }
                    //                    }
                    //                    for (int i = linesEnd-1; i >= linesStart; i--) {
                    //                        if ((((Line2D.Double) lines.get(i)).getP1()).getY() > 25.0 ||
                    //                                (((Line2D.Double) lines.get(i)).getP1()).getY() < 21.0) {
                    //                            linesEnd = i;
                    //                            break;
                    //                        }
                    //                    }
                    //                    if (linesEnd == 0) {
                    //                        linesEnd = lines.size();
                    //                    }
                    int fftSize=(int)Math.pow(2,(int)(Math.log10((double)(linesEnd - linesStart))/Math.log10(2.0))+1);
                    Complex[] x = new Complex[fftSize];
                    double newDuration = fftSize / lines.size();
                    for (int i = 0, j=linesStart; i < fftSize; i++,j++) {
                        if (j >= linesEnd-1) {
                            x[i] = new Complex( (double)(((Line2D.Double) lines.get(linesEnd-1)).getP1()).getY()-23.0,0.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(linesEnd)).getP1()).getX());
                        } else {
                            x[i] = new Complex( (double)(((Line2D.Double) lines.get(j)).getP1()).getY()-23.0,0.0);//newDuration*i/(linesEnd-linesStart));//(double)(((Line2D.Double) lines.get(i+linesStart)).getP1()).getX());
                            ////System.out.println(x[i].toString()); 

                        }
                    }
                    Complex[] y = FFT.fft(x);
                    Line2D.Double[] fftLines = new Line2D.Double[fftSize];
                    //System.out.println("fftSize="+fftSize);
                    double min = 8000000000000.0;
                    int mini = (int)Math.round((300.0/44100.0)*fftSize);
                    for (int i = (int)Math.round((300.0/44100.0)*fftSize);
                            i < (int)Math.round((600.0/44100.0)*fftSize); i++) {
                        if (min > y[i].abs()) {
                            min = y[i].abs();
                            mini = i;
                        }
                    }
                    //System.out.println("min="+min+" f0=44100.0*"+mini+"/"+fftSize+"="+44100.0*mini/fftSize);
                    f0 =44100.0*mini/fftSize;
                    for (int i = 1; i < fftSize-2; i++) {

                        fftLines[i] = new Line2D.Double(w*(i+1)/fftSize,(-h+INFOPAD+2)*y[i+1].abs()/1000+23,
                                w*(i+2)/fftSize,(-h+INFOPAD+2)*y[i+2].abs()/1000+23);
                        ////System.out.println("x="+w*(i+1)/fftSize+" y="+((-h+INFOPAD+2))*y[i+1].abs()/max+23); 

                    }
                    // .. render sampling graph ..
                    g2.setColor(Color.RED);
                    for (int i = 1; i < fftSize-2; i++) {
                        g2.draw((Line2D) fftLines[i]);
                    }
                    fftLines = new Line2D.Double[fftSize];
                    for (int i = 1; i < fftSize-2; i++) {

                        fftLines[i] = new Line2D.Double(w*(i+1)/fftSize,(-h+INFOPAD+2)*x[i+1].abs()/30+23,
                                w*(i+2)/fftSize,(-h+INFOPAD+2)*x[i+2].abs()/30+23);
                        //Sxstem.out.println("x="+w*(i+1)/fftSize+" x="+((-h+INFOPAD+2))*x[i+1].abs()/max+23); 

                    }

                    g2.setColor(Color.WHITE);
                    for (int i = 1; i < linesEnd; i++) {
                        g2.draw((Line2D) lines.get(i));
                    }
                    
                    FftLong fftlong = new FftLong(lines);
                    fftlong.repaint();
                    fftlong.setVisible(true);
                }
            }
        }

        public void start() {
            thread = new Thread(this);
            thread.setName("SamplingGraph");
            thread.start();
            seconds = 0;
        }

        public void stop() {
            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
        }

        public void run() {
            seconds = 0;
            while (thread != null) {
                if ((playback.line != null) && (playback.line.isOpen()) ) {

                    long milliseconds = (long)(playback.line.getMicrosecondPosition() / 1000);
                    seconds =  milliseconds / 1000.0;
                } else if ( (capture.line != null) && (capture.line.isActive()) ) {

                    long milliseconds = (long)(capture.line.getMicrosecondPosition() / 1000);
                    seconds =  milliseconds / 1000.0;
                }

                try { thread.sleep(100); } catch (Exception e) { break; }

                repaint();

                while ((capture.line != null && !capture.line.isActive()) ||
                        (playback.line != null && !playback.line.isOpen())) 
                {
                    try { thread.sleep(10); } catch (Exception e) { break; }
                }
            }
            seconds = 0;
            repaint();
        }
    } // End class SamplingGraph




    public static void main(String s[]) {
        CapturePlayback capturePlayback = new CapturePlayback();
        capturePlayback.open();
        JFrame f = new JFrame("Capture/Playback");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        f.getContentPane().add("Center", capturePlayback);
        f.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = 1000;
        int h = 410;
        f.setLocation(screenSize.width/2 - w/2, screenSize.height/2 - h/2);
        f.setSize(w, h);
        f.show();
    }
} 
