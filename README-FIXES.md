# Sample keyboard and sliding FFT fixes

Based on GitHub commit 73154d621af8b5c385520535e40502aa05fec319.

Copy the contents of this ZIP into the Javasounddemo project root, replacing files.
The ZIP includes updated src and bin classes, a rebuilt JavaSoundDemo.jar, tests,
and Windows scripts. Existing sample audio and Eclipse configuration are retained.
Use Java 17. In Eclipse refresh the project and clean/rebuild it; run CapturePlayback.
Alternatively double-click run-sampler.bat. To rebuild with a JDK run build.bat.
Run test.bat for the headless regression checks.

1. Load a WAV, AIFF or AU sample, or record a note and stop recording.
2. Use the on-screen piano immediately; no need to press Play or select FFT first.
3. Time/FFT toggles the embedded waveform/spectrum view.
4. Sliding FFT... explicitly opens the grayscale spectrogram in a separate window.
   Closing that window leaves the application running. Another click raises an
   existing window, or creates a fresh one after it has closed.

The spectrogram uses real PCM audio, a 2048-sample Hann window, 512-sample hop,
a fixed 80 dB grayscale range and at most 1200 time columns. Time increases to
 the right; frequency increases upwards to Nyquist. It runs in a background
worker and painting only draws the cached image.

Fundamental detection uses normalised autocorrelation over an energetic section
of the sample (40–2000 Hz). Silence and unreliable noise are rejected. It is
intended for single, steady notes; chords, changing pitch and some harmonic
sounds may yield ambiguous estimates. Playback uses target-note frequency /
detected fundamental, interpolates each channel, and remains monophonic.
Changing playback speed changes both pitch and duration, as requested.

PCM conversion handles supported mono/stereo 8/16-bit signed/unsigned and endian
formats through Java Sound. Audio samples are held in memory: use short samples.
Pitch/spectrum analysis happens on load/record completion, never inside painting.

Validation: Java 17 compilation and headless checks covering 8/22.05/44.1/48 kHz,
mono/stereo, a stronger second harmonic, unsigned 8-bit, big-endian PCM, silence,
empty data, interpolation, image bounds, explicit button and repeated FFT repaint.
Audible playback and live microphone recording require verification on your PC.
The Java 17 update removes deprecated applet, boxed-constructor and Window.show calls.
Legacy raw collection warnings remain.

## Bash / Java 17 update

From Git Bash in the project root:

```bash
bash build.sh
bash test.sh
bash run-sampler.sh
# Full tabbed desktop application:
java -jar JavaSoundDemo.jar
```

Install/use a JDK 17 or newer. The build explicitly targets Java 17 with
--release 17 and UTF-8. JAVA_HOME, when set, selects the JDK rather than PATH.
The build cleans only the generated build directory and recreates the JAR.
Tests use the same release target and are compiled after packaging, so they
are not included in the application JAR.

The old JavaSoundApplet class is now a desktop launcher that delegates to
JavaSound.main; it no longer extends JApplet. Existing HTML applet pages are
historical and cannot launch this desktop build. Compiling for Java 8 would
not restore browser applet support. Use the desktop application instead.

Deprecated boxed constructors and Window.show calls have been replaced.
Complex.hashCode retains its original array hash behaviour without the varargs
warning. Older raw Vector collections still produce an unchecked-operation
note; these have not been hidden by warning suppression.

In Eclipse set the project JRE and compiler compliance to Java 17, refresh,
then clean/rebuild. Do not rely on old compiled classes in bin.
