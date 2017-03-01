package au.com.mtechconnect.equalizergraphtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * This is a test application to show basic usage of the @{@link EqualizerGraph} class.
 * Written by Clinton Padgett 2017/02/28
 */
public class MainActivity extends AppCompatActivity {
    /**
     * A class which runs a @{@link AudioTrack} in another thread and generates a test tone.
     */
    private static SoundPlayer soundPlayer = new SoundPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the SoundPlayer thread running.
        if (soundPlayer.getState() == Thread.State.NEW) {
            soundPlayer.start();
        }

        /**
         * Initialize the EqualizerGraph by calling attachToAudioTrack
         * with the AudioTrack SessionID. See @{@link AudioTrack#getAudioSessionId}
         */
        EqualizerGraph eq = (EqualizerGraph) findViewById(R.id.equalizer);
        eq.attachToAudioTrack(soundPlayer.getAudioSessionId());

        // Setup button to start and stop the soundPlayer test tone.
        Button b = (Button) findViewById(R.id.buttonPlay);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundPlayer.isSoundPlaying()) {
                    soundPlayer.pauseSound();
                } else {
                    soundPlayer.playSound();
                }
            }
        });
    }

}
