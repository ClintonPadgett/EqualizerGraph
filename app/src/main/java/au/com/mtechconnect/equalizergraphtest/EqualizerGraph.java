package au.com.mtechconnect.equalizergraphtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Created by Clinton Padgett on 13/12/2016.
 * This will show a draggable style graph which controls an equalizer attached to the given
 * SessionId {@link AudioTrack#getAudioSessionId()}.
 * You can save and load multiple settings by giving then a name as a String.
 */

public class EqualizerGraph extends RelativeLayout {
    //private static final String TAG = "EqualizerGraph";
    private static final short GAIN_DIVIDER = 100;
    private static final float GRAPH_DOT_SIZE = 15f;
    private static final int TOP_BOTTOM_GRAPH_PADDING = 25;
    private static final int GRAPH_LINE_WIDTH = 20;
    private final Paint paint = new Paint();
    private final Path path = new Path();
    private Context context;
    private int[] values = null;
    private VerticalSeekBar[] seekBars = null;
    private int sessionId;
    public Equalizer equalizer;
    private short bandLevelRangeMax;
    private short bandLevelRangeMin;
    private SharedPreferences sharedPrefNames;
    private SharedPreferences sharedPrefValues;
    private SharedPreferences sharedPrefGeneral;
    private OnSettingsChangedListener mOnSettingsChangedListener;

    public EqualizerGraph(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        sharedPrefNames = c.getSharedPreferences("EqualizerGraphNames", Context.MODE_PRIVATE);
        sharedPrefValues = c.getSharedPreferences("EqualizerGraphValues", Context.MODE_PRIVATE);
        sharedPrefGeneral = c.getSharedPreferences("EqualizerGraphGeneral", Context.MODE_PRIVATE);
        inflate(getContext(), R.layout.equlaizer_graph, this);
        setBackgroundColor(Color.WHITE);

        // Setup paint and path objects ready for onPaint method
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(GRAPH_LINE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    /**
     * Before using the control it needs to be attached to the AudioTrack using the AudioTrack
     * session ID. See @{@link AudioTrack#getAudioSessionId()}
     *
     * @param sessionId The session ID for the @{@link AudioTrack} the equalizer is
     *                  to be attached to.
     */
    @SuppressWarnings("unused")
    public void attachToAudioTrack(int sessionId) {
        // If the session ID has not changed there is no need to initialize again.
        // Also if sessionId = 0 then it is not valid.
        if (this.sessionId == sessionId || sessionId == 0) return;
        this.sessionId = sessionId;
        equalizer = new Equalizer(0, this.sessionId);
        equalizer.setEnabled(true);
        // Get number of equalizer bands and band levels
        int bands = equalizer.getNumberOfBands();
        bandLevelRangeMin = equalizer.getBandLevelRange()[0];
        bandLevelRangeMax = equalizer.getBandLevelRange()[1];
        // Initialize arrays
        if (values == null) {
            values = new int[bands];
            // Get stored value or set to mid way if none found.
            for (int i = 0; i < bands; i++) {
                values[i] = sharedPrefValues.getInt(String.valueOf(i), 50);
            }
            seekBars = new VerticalSeekBar[bands];
        }

        // Update text views and add seek bars
        updateLabels();
        initializeVerticalSeekBars();
    }

    private void updateLabels() {
        // Set label for max gain
        TextView tv = (TextView) findViewById(R.id.textViewGainMax);
        tv.setText(String.valueOf(Math.round(bandLevelRangeMax / GAIN_DIVIDER)));

        // Set label for mid gain
        tv = (TextView) findViewById(R.id.textViewGainMid);
        tv.setText(String.valueOf(Math.round((((bandLevelRangeMax - bandLevelRangeMin) / 2)
                + bandLevelRangeMin) / GAIN_DIVIDER)));

        // Set label for min gain
        tv = (TextView) findViewById(R.id.textViewGainMin);
        tv.setText(String.valueOf(Math.round(bandLevelRangeMin / GAIN_DIVIDER)));

        // Set label for min frequency
        tv = (TextView) findViewById(R.id.textViewFrequencyMin);
        int firstBandMin = equalizer.getBandFreqRange((short) 0)[0];
        firstBandMin = round(firstBandMin / 1000, 10);
        tv.setText(context.getString(R.string.frequency, firstBandMin));

        // Set label for max frequency
        tv = (TextView) findViewById(R.id.textViewFrequencyMax);
        short lastBand = (short) (equalizer.getNumberOfBands() - 1);

        // Some devices return incorrect value for max frequency of last band.
        // E.g. ZTE Axon 7 returns "1".
        int lastBandMin = equalizer.getBandFreqRange(lastBand)[0];
        int lastBandMid = equalizer.getCenterFreq(lastBand);
        int lastBandMaxCalculated = ((lastBandMid - lastBandMin) * 2) + lastBandMin;
        int maxFreq = Math.max(equalizer.getBandFreqRange(lastBand)[1], lastBandMaxCalculated);
        maxFreq = round(maxFreq / 1000, 10);
        tv.setText(context.getString(R.string.frequency, maxFreq));
    }

    /**
     * Adds the seek bars.
     */
    private void initializeVerticalSeekBars() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.llSeekBars);
        ll.removeAllViews();
        for (int i = 0; i < seekBars.length; i++) {
            final short pos = (short) i;
            final VerticalSeekBar vsb = new VerticalSeekBar(context);
            seekBars[pos] = vsb;
            vsb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));

            // Remove the line drawable for the VerticalSeekBar so we only see the graph.
            vsb.setProgressDrawable(new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)));
            ll.addView(vsb);

            // Set the initial position of the VerticalSeekBar.
            setBandLevelByPercentage(pos, values[i], true, false);

            vsb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setBandLevelByPercentage(pos, progress, true, false);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            // Save the current value.
            vsb.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        SharedPreferences.Editor editor = sharedPrefValues.edit();
                        editor.putInt(String.valueOf(pos), vsb.getProgress());
                        editor.apply();
                        // Save the name as "" for a custom settings
                        SharedPreferences.Editor editorName = sharedPrefGeneral.edit();
                        editorName.putString("CurrentSettings", "");
                        editorName.apply();
                        // Fire the settings changed event
                        if (mOnSettingsChangedListener != null)
                            mOnSettingsChangedListener.onSettingsChangedListener(values, "", true);
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Register a callback to be invoked when the equalizer settings
     * are changed.
     *
     * @param listener The callback that will run
     */
    @SuppressWarnings("unused")
    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        mOnSettingsChangedListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when
     * the equalizer settings are changed.
     */
    public interface OnSettingsChangedListener {
        /**
         * Callback method to be invoked when the equalizer settings are changed.
         * Note that this is not invoked during touch down and touch move events, only
         * on touch up or when {@code setBandLevelByPercentage } is called.
         *
         * @param newSettings  The equalizer gain settings as percentages.
         * @param settingsName The current saved settings name.
         * @param fromUser     True if the user made the change via the graph interface.
         */
        void onSettingsChangedListener(int[] newSettings, String settingsName, boolean fromUser);
    }

    /**
     * Sets an equalizer band to the given gain value and sets the seek bar to the correct position.
     * Note that 0 percent = minimum band level and 100 = max band level.
     * (see @{@link Equalizer#getBandLevelRange()} )
     *
     * @param band                   frequency band that will have the new gain. The numbering of the bands starts
     *                               from 0 and ends at (number of bands - 1). See {@link Equalizer#getNumberOfBands()}
     * @param percentage             new gain as stored locally in 0-100 form.
     * @param invalidateView         If true the graph will be re-drawn. Only use false if you are calling
     *                               multiple times. Then invalidate on last call only.
     * @param triggerSettingsChanged If false the OnSettingsChangedListener will not be triggered.
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public void setBandLevelByPercentage(short band, int percentage, boolean invalidateView,
                                         boolean triggerSettingsChanged) {
        // Ensure values are within max allowed
        percentage = checkIntValue(percentage);

        // Set the band level on the equalizer.
        equalizer.setBandLevel(band, percentageToBandLevel(percentage));

        // Update the value in the array list. Values are stored as 0 to 100.
        values[band] = percentage;

        // Set position of seek bar
        seekBars[band].setProgress(percentage);
        if (invalidateView) invalidate();

        // Fire the settings changed event
        if (triggerSettingsChanged && mOnSettingsChangedListener != null)
            mOnSettingsChangedListener.onSettingsChangedListener(values, getCurrentSettingsName(), false);
    }


    /**
     * Converts a band level to a integer between 0 and 100
     *
     * @param bandLevel new gain in millibels that will be set to the given band.
     *                  {@link Equalizer#getBandLevelRange()} returns the maximum and minimum values.
     * @return the level as an integer between 0 and 100. Note that 0 percent = minimum band level
     * and 100 = max band level. (see @{@link Equalizer#getBandLevelRange()} )
     */
    @SuppressWarnings("unused")
    public int bandLevelToPercentage(short bandLevel) {
        if (bandLevel < bandLevelRangeMin) bandLevel = bandLevelRangeMin;
        if (bandLevel > bandLevelRangeMax) bandLevel = bandLevelRangeMax;
        short fullRange = (short) (bandLevelRangeMax - bandLevelRangeMin);
        int value = (((bandLevel - bandLevelRangeMin) / (fullRange / 100)));

        // Ensure values are within max allowed
        value = checkIntValue(value);

        return value;
    }

    /**
     * Convert a integer value between 0 and 100 to a frequency band value.
     *
     * @param percentage the integer value as stored in values array list. Between 0 and 100.
     *                   Note that 0 percent = minimum band level and 100 = max band level.
     *                   (see @{@link Equalizer#getBandLevelRange()} )
     * @return Gain in millibels.
     */
    public short percentageToBandLevel(int percentage) {
        // Ensure values are within max allowed
        percentage = checkIntValue(percentage);

        // Return the band level
        short fullRange = (short) (bandLevelRangeMax - bandLevelRangeMin);
        return (short) (((percentage / 100f) * fullRange) + bandLevelRangeMin);
    }

    /**
     * Ensures a integer value is between 0 and 100.
     *
     * @param value The value to check and return.
     * @return Integer value between 0 and 100.
     */
    private int checkIntValue(int value) {
        if (value > 100) value = 100;
        if (value < 0) value = 0;
        return value;
    }

    /**
     * Draw the graph line and dots.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        path.reset();

        if (isInEditMode()) {
            // Display example for development purposes only
            LinearLayout ll = (LinearLayout) findViewById(R.id.llSeekBars);
            int height = (int) (ll.getHeight() * 0.9);
            int width = (int) (ll.getWidth() * 0.9);
            int paddingW = (int) (ll.getWidth() * 0.05);
            int paddingH = (int) (ll.getHeight() * 0.05);
            for (int i = 0; i < 5; i++) {
                float x = (width * i / 4) + paddingW + ll.getX() + getPaddingLeft();
                float y = (float) (Math.random() * height) + paddingH + getPaddingTop();
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
                canvas.drawCircle(x, y, GRAPH_DOT_SIZE, paint);
            }
        } else {
            // Draw line and dots
            int[] startPos = getSeekBarProgressPosition(0);
            path.reset();
            path.moveTo(startPos[0], startPos[1]);
            path.addCircle(startPos[0], startPos[1], GRAPH_DOT_SIZE, Path.Direction.CW);
            path.moveTo(startPos[0], startPos[1]);
            for (int i = 1; i < seekBars.length; i++) {
                int[] pos = getSeekBarProgressPosition(i);
                path.lineTo(pos[0], pos[1]);
                canvas.drawCircle(pos[0], pos[1], GRAPH_DOT_SIZE, paint);
            }
        }
        canvas.drawPath(path, paint);
    }


    /**
     * Gets the x and y coordinates for a seekbar within the seekBars array.
     *
     * @param seekBarPosition The position of the seek bar within the seekBars array
     * @return int array. Position 0 = x coordinates, 1 = y coordinates
     */
    private int[] getSeekBarProgressPosition(int seekBarPosition) {
        if (sessionId == 0) return new int[]{0, 0};
        VerticalSeekBar vsb = seekBars[seekBarPosition];
        int height = vsb.getMeasuredHeight() - vsb.getPaddingBottom() - vsb.getPaddingTop() - 50;
        int x = vsb.getLeft() + vsb.getPaddingLeft() + (vsb.getWidth() / 2);
        int y = vsb.getPaddingTop() + TOP_BOTTOM_GRAPH_PADDING + ((height * (100 - vsb.getProgress())) / 100);
        return new int[]{x, y};
    }

    /**
     * Gets the number of frequency bands supported by the Equalizer engine.
     *
     * @return the number of bands
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    @SuppressWarnings("unused")
    public int getNumberOfBands() {
        return equalizer.getNumberOfBands();
    }

    /**
     * Gets the current selected settings name. If the current settings have been changed by the
     * user then an empty string will be returned.
     *
     * @return Current selected settings name as a string.
     */
    public String getCurrentSettingsName() {
        return sharedPrefGeneral.getString("CurrentSettings", "");
    }

    /**
     * Gets a list of saved setting names.
     *
     * @return Saved setting names in a string array.
     */
    @SuppressWarnings("unused")
    public String[] getSavedSettingsNames() {
        Object[] objects = sharedPrefNames.getAll().values().toArray();
        return Arrays.copyOf(objects, objects.length, String[].class);
    }

    /**
     * Saves the current settings. The settings can then be re-loaded using loadSettings
     *
     * @param name The name for the settings.
     * @return True if success. False on error.
     */
    @SuppressWarnings("unused")
    public boolean saveSettings(String name) {
        if (seekBars == null || seekBars.length == 0) return false;
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < seekBars.length; i++) {
            editor.putInt(String.valueOf(i + 1), seekBars[i].getProgress());
        }
        editor.apply();
        SharedPreferences.Editor editorNames = sharedPrefNames.edit();
        editorNames.putString(name, name);
        editorNames.apply();
        SharedPreferences.Editor editorGeneral = sharedPrefGeneral.edit();
        editorGeneral.putString("CurrentSettings", name);
        editorGeneral.apply();
        return true;
    }

    /**
     * Delete saved settings record
     *
     * @param name Name of the saved setting.
     * @return True if successful. False if name not found.
     */
    @SuppressWarnings("unused")
    public boolean deleteSettings(String name) {
        if (!sharedPrefNames.contains(name)) return false;
        SharedPreferences.Editor editorNames = sharedPrefNames.edit();
        editorNames.remove(name);
        editorNames.apply();
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        return true;
    }

    /**
     * Set the equalizer to saved settings. See {@code saveSettings}
     *
     * @param name Name of the saved settings.
     * @return True if successful.
     */
    @SuppressWarnings("unused")
    public boolean loadSettings(String name) {
        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        short[] array = new short[sp.getAll().size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = (short) sp.getInt(String.valueOf(i+1), 0);
        }

        String origName = sharedPrefGeneral.getString("CurrentSettings", "");
        SharedPreferences.Editor editor = sharedPrefGeneral.edit();
        editor.putString("CurrentSettings", name);
        editor.apply();
        boolean loaded = loadValues(array, true);
        if (loaded) {
            // Save values to shared preferences memory so next time the EqualizerGraph is initialized
            // it will load these settings again.
            SharedPreferences.Editor editorValues = sharedPrefValues.edit();
            for (int i = 0; i < array.length; i++) {
                editorValues.putInt(String.valueOf(i), array[i]);
            }
            editorValues.apply();
        } else {
            editor.putString("CurrentSettings", origName);
            editor.commit();
        }
        return loaded;
    }

    /**
     * Updates the equalizer to the supplied values.
     *
     * @param percentages    new gain as stored locally in 0-100 form.
     * @param invalidateView If true the graph will be re-drawn. Only use false if you are calling
     *                       multiple times. Then invalidate on last call only.
     * @return True if successful.
     */
    public boolean loadValues(short[] percentages, boolean invalidateView) {
        if (percentages.length != seekBars.length) return false;
        for (short i = 0; i < percentages.length; i++) {
            if (i == percentages.length - 1) {

                // Only refresh the fiew on the last value.
                setBandLevelByPercentage(i, percentages[i], invalidateView, true);

            } else {
                setBandLevelByPercentage(i, percentages[i], false, false);
            }

        }
        return true;
    }

    /**
     * Rounds a value to a specific incrument. E.g. round(114, 10) will return 110.
     *
     * @param valueToRound The value to be rounded
     * @param incrument    The number to round to.
     * @return integer value rounded.
     */
    private int round(double valueToRound, int incrument) {
        return (int) Math.round(valueToRound / incrument) * incrument;
    }

}
