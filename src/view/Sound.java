package view;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Background music and sound effects (bonus feature).
 * All sounds are generated in code, so no audio files are needed.
 * If the computer has no audio device, the game simply stays silent.
 */
final class Sound {
    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final Map<String, Clip> effects = new HashMap<>();
    private static Clip music;
    private static boolean muted;

    private Sound() { }

    static boolean isMuted() { return muted; }

    static void setMuted(boolean value) {
        muted = value;
        if (muted) stopMusic();
        else startMusic();
    }

    static void startMusic() {
        if (muted) return;
        try {
            if (music == null) music = open(marimbaLoop());
            if (!music.isRunning()) {
                music.setFramePosition(0);
                music.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Throwable ignored) { /* no audio device: stay silent */ }
    }

    static void stopMusic() {
        try { if (music != null) music.stop(); } catch (Throwable ignored) { }
    }

    static void roll() { play("roll"); }
    static void hop() { play("hop"); }
    static void capture() { play("capture"); }
    static void correct() { play("correct"); }
    static void wrong() { play("wrong"); }
    static void surprise() { play("surprise"); }
    static void win() { play("win"); }

    private static void play(String name) {
        if (muted) return;
        try {
            Clip clip = effects.get(name);
            if (clip == null) {
                clip = open(effect(name));
                if (clip == null) return;
                effects.put(name, clip);
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Throwable ignored) { /* no audio device: stay silent */ }
    }

    private static Clip open(short[] samples) throws Exception {
        byte[] data = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            data[2 * i] = (byte) samples[i];
            data[2 * i + 1] = (byte) (samples[i] >> 8);
        }
        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, data, 0, data.length);
        return clip;
    }

    private static short[] effect(String name) {
        switch (name) {
            case "roll": return rattle();
            case "hop": return tones(new double[][]{{520, 780, 45}}, .35);
            case "capture": return tones(new double[][]{{660, 220, 220}}, .45);
            case "correct": return tones(new double[][]{{523, 523, 110}, {659, 659, 110}, {784, 784, 220}}, .4);
            case "wrong": return tones(new double[][]{{330, 330, 160}, {247, 196, 320}}, .4);
            case "surprise": return tones(new double[][]{{784, 784, 70}, {988, 988, 70}, {1175, 1175, 70}, {1568, 1568, 160}}, .35);
            default: return tones(new double[][]{{523, 523, 150}, {659, 659, 150}, {784, 784, 150}, {1047, 1047, 450}}, .45);
        }
    }

    /** Short clicks with silence between them, like a die tumbling on a table. */
    private static short[] rattle() {
        Random random = new Random(7);
        int length = (int) (RATE * .32);
        short[] out = new short[length];
        int position = 0;
        while (position < length) {
            int click = (int) (RATE * .012);
            for (int i = 0; i < click && position + i < length; i++) {
                double envelope = 1 - (double) i / click;
                out[position + i] = (short) ((random.nextDouble() * 2 - 1) * envelope * 9000);
            }
            position += click + (int) (RATE * (.015 + random.nextDouble() * .03));
        }
        return out;
    }

    /** Notes given as {start Hz, end Hz, milliseconds}, played one after another. */
    private static short[] tones(double[][] notes, double volume) {
        int total = 0;
        for (double[] note : notes) total += (int) (RATE * note[2] / 1000);
        short[] out = new short[total];
        int position = 0;
        double phase = 0;
        for (double[] note : notes) {
            int length = (int) (RATE * note[2] / 1000);
            for (int i = 0; i < length; i++) {
                double t = (double) i / length;
                double frequency = note[0] + (note[1] - note[0]) * t;
                phase += 2 * Math.PI * frequency / RATE;
                double envelope = Math.min(1, i / (RATE * .005)) * (1 - t);
                double wave = Math.sin(phase) * .8 + Math.sin(2 * phase) * .2;
                out[position + i] = (short) (wave * envelope * volume * 32767);
            }
            position += length;
        }
        return out;
    }

    private static double midi(int note) { return 440 * Math.pow(2, (note - 69) / 12.0); }

    /** An original, playful 17-second marimba loop over F - C - Dm - Bb (112 bpm), kept quiet. */
    private static short[] marimbaLoop() {
        int[] bass = {41, 36, 38, 34};
        int[] melody = {72,69,72,74, 76,74,72,69, 67,69,72,67, 64,67,69,72,
                        74,72,69,72, 69,67,65,67, 65,67,69,70, 72,-1,69,-1};
        double eighth = 60 / 112.0 / 2;
        double[] mix = new double[(int) (RATE * eighth * 64)];
        for (int e = 0; e < 64; e++) {
            int start = (int) (e * eighth * RATE);
            int note = melody[e / 2];
            if (e % 2 == 0 && note > 0) marimbaHit(mix, start, midi(note), 1.2, .16, 4.0);
            if (e % 4 == 3) marimbaHit(mix, start, midi(note > 0 ? note - 12 : 60), .5, .06, 6.0);
            int root = bass[e / 16];
            if (e % 4 == 0) bassPluck(mix, start, midi(root), .45, .2);
            if (e % 4 == 2) bassPluck(mix, start, midi(root + 7), .3, .12);
        }
        short[] out = new short[mix.length];
        for (int i = 0; i < mix.length; i++)
            out[i] = (short) Math.max(-32767, Math.min(32767, mix[i] * .6 * 32767));
        return out;
    }

    /** A struck bar: a sine with a short bright overtone, fading out. */
    private static void marimbaHit(double[] mix, int start, double frequency, double seconds, double volume, double decay) {
        for (int i = 0; i < RATE * seconds && start + i < mix.length; i++) {
            double t = i / RATE;
            double envelope = Math.exp(-decay * t) * Math.min(1, i / (RATE * .003));
            double tone = Math.sin(2 * Math.PI * frequency * t)
                    + .25 * Math.sin(2 * Math.PI * frequency * 4 * t) * Math.exp(-20 * t);
            mix[start + i] += tone * envelope * volume;
        }
    }

    private static void bassPluck(double[] mix, int start, double frequency, double seconds, double volume) {
        for (int i = 0; i < RATE * seconds && start + i < mix.length; i++) {
            double t = i / RATE;
            double envelope = Math.exp(-7 * t) * Math.min(1, i / (RATE * .004));
            mix[start + i] += Math.sin(2 * Math.PI * frequency * t) * envelope * volume;
        }
    }
}
