package com.zipeg;

import javax.sound.sampled.*;
import javax.sound.midi.*;
import java.io.*;
import java.awt.*;
import java.net.MalformedURLException;

public class Sound implements LineListener, MetaEventListener {

    private Clip clip;
    private Sequencer sequencer;

    public Sound(InputStream in, boolean midi) {
        Throwable x = null;
        try {
            if (midi) {
                playMidi(in);
            } else {
                playSound(in);
            }
        } catch (IOException e) {
            x = e;
        } catch (UnsupportedAudioFileException e) {
            x = e;
        } catch (LineUnavailableException e) {
            x = e;
        } catch (Throwable e) {
            x = e;
        }
        if (x != null) {
            Debug.traceln("WARNING: failed to play sound " + x.getMessage());
            stop();            
        }
    }

    public boolean isPlaying() {
        return clip != null || sequencer != null;
    }

    private void playSound(InputStream in) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {
        in = new BufferedInputStream(in);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(in);
        AudioFormat srcFormat = audioIn.getFormat();
        AudioFormat.Encoding pcmEncoding = AudioFormat.Encoding.PCM_SIGNED;
        AudioFormat tgtFormat;
        if (srcFormat.getEncoding().equals(pcmEncoding)) {
            tgtFormat = srcFormat;
        } else {
            tgtFormat = new AudioFormat(pcmEncoding,
                                        srcFormat.getSampleRate(),
                                        srcFormat.getSampleSizeInBits(),
                                        srcFormat.getChannels(),
                                        srcFormat.getFrameSize(),
                                        srcFormat.getFrameRate(),
                                        srcFormat.isBigEndian());
        }
        DataLine.Info info = new DataLine.Info(Clip.class, tgtFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new UnsupportedAudioFileException("unsupported: " + info);
        }
        clip = (Clip)AudioSystem.getLine(info);
        AudioInputStream ain = srcFormat.getEncoding().equals(pcmEncoding) ? audioIn :
                AudioSystem.getAudioInputStream(pcmEncoding, audioIn);
        try {
            clip.addLineListener(this);
            clip.open(ain);
            clip.start();
        } catch (LineUnavailableException e) {
            clip.removeLineListener(this);
            // noinspection UnusedAssignment
            clip = null;
            Flags.removeFlag(Flags.PLAY_SOUNDS);
            throw e;
        } catch (OutOfMemoryError e) {
            // ignore
            clip.removeLineListener(this);
            // noinspection UnusedAssignment
            clip = null;
            Flags.removeFlag(Flags.PLAY_SOUNDS);
        }
    }

    public void update(LineEvent e) {
        // uk.co.mandolane.oemamixer on Mac calls update from init on
        // dispatch thread
//      assert !IdlingEventQueue.isDispatchThread();
        if (LineEvent.Type.STOP.equals(e.getType())) {
            EventQueue.invokeLater(new Runnable(){
                public void run() {
                    if (clip != null) {
                        clip.close();
                    }
                }
            });
        } else if (LineEvent.Type.CLOSE.equals(e.getType())) {
            EventQueue.invokeLater(new Runnable(){
                public void run() {
                    if (clip != null) {
                        clip.removeLineListener(Sound.this);
                        clip = null;
                    }
                }
            });
        }
    }

    public void playMidi(InputStream in) throws Exception {
        Exception x = null;
        in = new BufferedInputStream(in);
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.addMetaEventListener(this);
            sequencer.open();
            sequencer.setSequence(MidiSystem.getSequence(in));
            sequencer.start();
        }
        catch (MalformedURLException e) { x = e; }
        catch (IOException e) { x = e; }
        catch (MidiUnavailableException e) { x = e; }
        catch (InvalidMidiDataException e) { x = e; }
        if (x != null) {
            closeSequencer();
            throw x;
        }
    }

    public void meta(MetaMessage e) {
//      assert !IdlingEventQueue.isDispatchThread();
        if (e.getType() == 47) { // Sequencer is done playing
            EventQueue.invokeLater(new Runnable(){
                public void run() { closeSequencer(); }
            });
        }
    }

    private void closeSequencer() {
//      assert IdlingEventQueue.isDispatchThread();
        if (sequencer != null) {
            sequencer.stop();
            sequencer.close();
            sequencer.removeMetaEventListener(this);
            sequencer = null;
        }
    }

    public void stop() {
        closeSequencer();
        if (clip != null) {
            clip.stop();
            clip.close();
            clip.removeLineListener(this);
            clip = null;
        }
    }

}
