package ghh.grayhat.graybot_sr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class Player extends AppCompatActivity {

    private static final boolean AUTO_HIDE = true;
    FrameLayout frame;
    String title;
    private boolean isPlaying=false;
    String author;
    String mrl;
    ImageButton b,p,n;
    SeekBar seekBar;
    TextView details;
    private double startTime = 0;
    private double finalTime = 0;
    private Handler myHandler = new Handler();
    Song song;
    int percentage;
    MediaPlayer mediaPlayer;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            details.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        b=(ImageButton)findViewById(R.id.back);
        p=(ImageButton)findViewById(R.id.play);
        n=(ImageButton)findViewById(R.id.next);
        seekBar=(SeekBar)findViewById(R.id.seekbar);
        seekBar.setClickable(false);
        b.setEnabled(false);
        n.setEnabled(false);
        p.setEnabled(false);
        frame=(FrameLayout)findViewById(R.id.frame);
        details=(TextView)findViewById(R.id.detail);
        int position=Integer.parseInt(""+getIntent().getExtras().get("position"));
        song=MainActivity.songList.get(position);
        //image=song.getImage();
        title=song.getTitle();
        author=song.getChannel();
        mrl=song.getMrl();
        //frame.setBackground(new BitmapDrawable(getResources(),image));
        details.setText(title+"\n"+author);
        if(MediaPlayerData.player==null)
            mediaPlayer=new MediaPlayer();
        else {
            mediaPlayer = MediaPlayerData.player;
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }
        try {
            mediaPlayer=new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(mrl);
            mediaPlayer.prepare();
            finalTime=mediaPlayer.getDuration();
            seekBar.setProgress((int)startTime);
            MediaPlayerData.player=mediaPlayer;
            mediaPlayer.start();
            finalTime=mediaPlayer.getDuration();
            myHandler.postDelayed(UpdateSongTime,100);
            isPlaying=true;
            setButtons();
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    percentage=percent;
                }
            });
            System.out.println("PREPARED");
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying=false;
                    setButtons();
                    mediaPlayer.release();
                    mediaPlayer=null;
                    MediaPlayerData.player=null;
                }
            });
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // Set up the user interaction to manually show or hide the system UI.
        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    private void setButtons() {
        if(isPlaying)
        {
            p.setEnabled(true);
            p.setImageResource(R.drawable.ic_pause);
            b.setEnabled(true);
            n.setEnabled(true);
        }
        else
        {
            p.setEnabled(false);
            p.setImageResource(R.drawable.ic_play);
            b.setEnabled(false);
            n.setEnabled(false);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        details.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    public void runb(View view) {
        if(isPlaying&&mediaPlayer.getCurrentPosition()-5000>=0)
        {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()-5000);
        }
    }

    public void play(View view) {
        if(isPlaying)
        {
            mediaPlayer.pause();
            p.setImageResource(R.drawable.ic_play);
            isPlaying=false;
        }
        else
        {
            mediaPlayer.start();
            isPlaying=true;
            p.setImageResource(R.drawable.ic_pause);
        }
    }

    public void runn(View view) {
        if(isPlaying&&mediaPlayer.getCurrentPosition()+5000<mediaPlayer.getDuration())
        {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+5000);
        }
    }
    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            seekBar.setMax((int)finalTime);
            seekBar.setProgress((int)startTime);
            if(mediaPlayer==null)
                return;
            if(!mediaPlayer.isPlaying())
                return;
            myHandler.postDelayed(this, 100);
        }
    };

    public void share(View view) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "SHARED FROM GrayBot\nLink : "+song.getSource();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Song Shared from GrayBot");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share To"));
    }
}
