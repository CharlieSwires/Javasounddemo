# Java Sound Demo

A Java 17 desktop application demonstrating sampled audio, MIDI synthesis, recording and rhythm sequencing. The demos share a Swing interface with tabs for **Juke Box**, **Capture/Playback**, **MIDI Synthesizer** and **Groove Box**.

The sample keyboard detects a sample’s fundamental frequency and changes playback speed to match the selected piano key. A separate grayscale sliding FFT display can be opened when needed.

## Requirements

- **Java 17 or newer** to run the supplied Java 17 build.
- A **JDK 17 or newer** to build the source or run the tests.
- An available audio output device; a microphone/input device for recording.
- Bash, such as **Git Bash on Windows**, for the shell scripts.

Run commands from the project root. If `JAVA_HOME` is set, the Bash scripts use that JDK; otherwise they use `java` from `PATH`.

## Quick start

Run the complete tabbed application:

```bash
java -jar JavaSoundDemo.jar
```

Or launch just the sample keyboard and recorder:

```bash
bash run-sampler.sh
```

The full application uses the `audio` directory by default. To select another sample directory:

```bash
java -jar JavaSoundDemo.jar "path/to/audio"
```

## Build and test

Build the application:

```bash
bash build.sh
```

The build compiles `src/*.java` using `--release 17` and UTF-8, clears the generated `build` directory and creates `JavaSoundDemo.jar`.

Run the automated checks:

```bash
bash test.sh
```

The test script rebuilds the application, compiles the tests and runs them in headless mode. Checks cover pitch detection, audio formats, silence, empty samples, interpolation, spectrogram bounds and FFT display behaviour. Test classes are excluded from the application JAR.

Audible playback and microphone recording still need to be checked on your computer. Legacy unchecked-collection notes may appear during compilation.

### Eclipse

1. Set the project’s JRE and compiler compliance to **Java 17**.
2. Refresh the project after copying over updated files.
3. Select **Project → Clean** and rebuild.
4. Run `JavaSound` for the full application, or `CapturePlayback` for the sampler.

## Demos

| Demo | Features |
| --- | --- |
| **Juke Box** | Plays supported sampled audio and MIDI files, with playback progress, seek, pan and volume controls. File support depends on the Java Sound providers installed. |
| **Capture/Playback** | Records, loads, plays and saves samples; provides a sample-driven piano keyboard, waveform/spectrum view and optional sliding FFT display. |
| **MIDI Synthesizer** | Demonstrates MIDI instruments and controllers, with a piano keyboard and recording/playback of MIDI note events. |
| **Groove Box** | Builds a sequence of short events and provides a tempo dial to change beats per minute. |

### Capture/Playback: sample keyboard

1. Click **Load…** to open a WAV, AIFF or AU sample, or click **Record** and stop recording when finished.
2. Use a sample containing a single, steady pitched note for the best results.
3. Play the on-screen piano immediately. You do not need to press **Play** or select **FFT** first.
4. Use **Time/FFT** to switch the embedded display between the waveform and spectrum.
5. Click **Sliding FFT…** to open the separate grayscale spectrogram.

The sliding FFT window opens only when requested. Closing it leaves the application running. Clicking the button again raises an existing window or opens a new one after it has closed.

Captured samples can be saved as **WAVE**, **AU** or **AIFF**.

### Pitch detection and playback

Fundamental detection uses normalised autocorrelation over an energetic section of the actual PCM audio, with a detection range of approximately **40–2,000 Hz**. It does not analyse the reduced waveform drawn on screen.

The playback-speed ratio is:

```text
playback speed = selected key frequency / detected fundamental frequency
```

Playback interpolates each channel and supports mono and stereo samples. It is **monophonic**: starting another note replaces the current voice. Changing playback speed changes both pitch and duration.

Silence and samples without a reliable detected pitch are rejected for keyboard playback. Chords, changing pitch and some harmonic sounds may produce ambiguous estimates. Samples are held in memory, so use short samples.

### Sliding FFT display

The grayscale spectrogram uses:

- A **2,048-sample Hann window**.
- A **512-sample hop** between windows.
- A fixed **80 dB** grayscale range.
- At most **1,200 time columns** to bound the image size.

Time increases to the right; frequency increases upwards to the Nyquist frequency, half the sample rate. Brighter pixels represent stronger frequency components.

Sliding FFT analysis runs in a background worker. Repainting draws a cached image and does not repeat the analysis or create additional windows.

## Run individual demos

```bash
java -cp JavaSoundDemo.jar Juke
java -cp JavaSoundDemo.jar CapturePlayback
java -cp JavaSoundDemo.jar MidiSynth
java -cp JavaSoundDemo.jar Groove
```

## Project files

| Path | Purpose |
| --- | --- |
| `src/` | Java source files. |
| `audio/` | Sample media for the demos. |
| `tests/SampleAudioTest.java` | Headless regression checks. |
| `build.sh` | Builds the Java 17 application JAR. |
| `test.sh` | Builds and runs the automated checks. |
| `run-sampler.sh` | Launches Capture/Playback. |
| `JavaSoundDemo.jar` | Runnable desktop application. |
| `README-FIXES.md` | Additional implementation and update notes. |

## Desktop application and legacy applet files

This version runs as a **desktop Swing application** and does not depend on `JApplet`. The `JavaSoundApplet` class name is retained as a desktop launcher that delegates to `JavaSound.main`.

The old HTML applet pages are historical and do not launch this desktop build. The former applet `.java.policy` configuration is no longer required. Compiling for Java 8 would not restore browser applet support.

## Original demo and licensing

The application is based on the Sun Microsystems Java Sound demonstration code. Original copyright and licence notices remain in the source files; consult those notices for redistribution terms.
