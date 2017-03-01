package au.com.mtechconnect.equalizergraphtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by Clinton Padgett on 28/02/2017.
 * This class will play white noise sound.
 */

class SoundPlayer extends Thread {
    private static final int SAMPLE_COUNT = 44100;
    private boolean soundPlaying = false;
    private byte noise[] = new byte[SAMPLE_COUNT];
    private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
            SAMPLE_COUNT, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_8BIT, SAMPLE_COUNT,
            AudioTrack.MODE_STREAM);

    /**
     * Generates a buffer of white noise into @{@link #noise}.
     */
    private void genWhiteNoise() {
        for (int i = 0; i < SAMPLE_COUNT; ++i) {
            double ran = (Math.random() * 2) - 1;
            noise[i] = (byte) (ran * 127);
        }
    }

    /**
     * Check if the noise sound is currently playing.
     *
     * @return True if sound is playing. False is paused or has not been started yet.
     */
    boolean isSoundPlaying() {
        return (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
    }

    /**
     * Pause the noise sound. Can be resumed using {@code playSound}
     */
    void pauseSound() {
        soundPlaying = false;
        audioTrack.pause();
    }

    /**
     * Start the noise playing.
     */
    void playSound() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                soundPlaying = true;
                audioTrack.play();
                while (soundPlaying) {
                    genWhiteNoise();
                    audioTrack.write(noise, 0, SAMPLE_COUNT);
                }

            }
        });
        thread.start();
    }

    /**
     * Get the session ID for the AudioTrack object {@link AudioTrack#getAudioSessionId()}
     *
     * @return the ID of the audio session this AudioTrack belongs to.
     */
    int getAudioSessionId() {
        return audioTrack.getAudioSessionId();
    }
}
