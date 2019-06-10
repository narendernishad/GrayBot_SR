package ghh.grayhat.graybot_sr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ImageButton back,play,next;
    SearchView searchView;
    MediaPlayer mediaPlayer;
    List<String> songs;
    ImageView thumb;
    TextView title,channel;
    boolean isSongStarted;
    PyObject pafy;
    int currentSong;
    PyObject ndat;
    SeekBar seek;
    private Handler mHandler;
    private Notification mNotification;
    private static final int NOTIF_ID = 1234;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    Bitmap bit;
    private boolean notificationStarted;

    @Override
    protected void onResume() {

        super.onResume();
        if(songs==null)
        {
            return;
        }
        if(songs.size()>0)
        {
            setViewResume();
            if(mediaPlayer!=null) {
                seek.setMax(mediaPlayer.getDuration());
                seek.setProgress(mediaPlayer.getCurrentPosition());
            }
        }
    }

    private void setViewResume() {
        PyObject titl=ndat.get("title");
        PyObject img=ndat.get("thumb");
        try {
            thumb.setImageBitmap(bit);
            title.setText(""+titl);
            PyObject chnl=ndat.get("author");
            channel.setText(""+chnl);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override protected void onStart() {

        super.onStart();
        Toast.makeText(this,"STARTED",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isSongStarted=false;
        currentSong=0;
        searchView=(SearchView)findViewById(R.id.searchbar);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(searchView);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        thumb=(ImageView)findViewById(R.id.thumbnail);
        title=(TextView)findViewById(R.id.title);
        channel=(TextView)findViewById(R.id.channel);
        back=(ImageButton)findViewById(R.id.back);
        play=(ImageButton)findViewById(R.id.play);
        next=(ImageButton)findViewById(R.id.next);
        seek=(SeekBar)findViewById(R.id.seek);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try
            {
                if (! Python.isStarted()) {
                    Python.start(new AndroidPlatform(getBaseContext()));
                }
                pafy = Python.getInstance().getModule("pafy");
                Intent intent = getIntent();
                String action = intent.getAction();
                String type = intent.getType();

                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    if ("text/plain".equals(type)) {
                        handleSendText(intent); // Handle text being sent
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            songs=null;
            if(mediaPlayer!=null)
            {
                if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                }
            }
            mediaPlayer=null;
            songs=new ArrayList<>();
            songs.add(sharedText);
            setView();
            play(play);
        }
    }

    private void seekAudio(int progress, boolean fromUser) {
        if(mediaPlayer != null && fromUser){
            mediaPlayer.pause();
            mediaPlayer.seekTo(progress);
            mediaPlayer.start();
        }
        else if(fromUser)
        {
            Toast.makeText(this,"No Audio Playing",Toast.LENGTH_SHORT).show();
        }
    }

    public List getLinks(String query)
    {
        List<String> links=new ArrayList<>();
        try {
            URL url = new URL("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8"));
            HttpsURLConnection urlConnection=(HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            String code = toString(urlConnection.getInputStream());
            while(code.contains("watch?v="))
            {
                code=code.substring(code.indexOf("watch?v="));
                String tmp=code.substring(0,11+"watch?v=".length());
                System.out.println("https://www.youtube.com/"+tmp);
                if(!links.contains("https://www.youtube.com/"+tmp))
                {
                    links.add("https://www.youtube.com/"+tmp);
                    System.out.println("UNIQUE : https://www.youtube.com/"+tmp);
                }
                code=code.substring(12);
            }
            System.out.println("GOT LINKS : "+links.size());
            return links;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("GOT LINKS ERROR");
            return links;
        }
    }
    private static String toString(InputStream inputStream) throws IOException
    {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8")))
        {
            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null)
            {
                stringBuilder.append(inputLine);
            }

            return stringBuilder.toString();
        }
    }

    public void search(View view) {
        String query=""+searchView.getQuery();
        Toast.makeText(this,"SEARCHING...",Toast.LENGTH_SHORT).show();
        songs = getLinks(query);
        isSongStarted=false;
        currentSong=0;
        setView();
        Toast.makeText(this,"SEARCHING DONE",Toast.LENGTH_SHORT).show();
    }

    private void setView() {
        ndat=pafy.callAttr("new",""+songs.get(currentSong));
        PyObject titl=ndat.get("title");
        PyObject img=ndat.get("thumb");
        try {
            bit=getBitmapFromURL(""+img);
            thumb.setImageBitmap(bit);
            title.setText(""+titl);
            PyObject chnl=ndat.get("author");
            channel.setText(""+chnl);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            Log.e("src",src);
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.e("Bitmap","returned");
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Exception",e.getMessage());
            return null;
        }
    }

    private String getUrl() {
        PyObject url=ndat.callAttr("getbestaudio").get("url");
        return ""+url;
    }

    private int getDuration()
    {
        PyObject lngth=ndat.get("length");
        return Integer.parseInt(""+lngth);
    }

    private void playAudio() {
        System.out.println("SONG NUMBER : "+currentSong);
        if(songs==null)
        {
            return;
        }
        if(currentSong>=songs.size())
        {
            return;
        }
        mediaPlayer=new MediaPlayer();
        isSongStarted=true;
        setView();
        Toast.makeText(this,"PLAYING : "+ndat.get("title"),Toast.LENGTH_LONG).show();
        try {
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();

            if(notificationStarted)
            {
                updateNotification();
            }
            else
            {
                setUpNotification();
            }
            play.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
            seek.setProgress(0);
            seek.setMax(mediaPlayer.getDuration());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    play.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    isSongStarted=false;
                    mediaPlayer=null;
                    runNext(next);
                }
            });
            isSongStarted=true;
            mHandler=new Handler();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mediaPlayer!=null)
                    {
                        int mCurrentPosition=mediaPlayer.getCurrentPosition();
                        seek.setProgress(mCurrentPosition);
                        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                seekAudio(progress,fromUser);
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });
                    }
                    mHandler.postDelayed(this,1);
                }
            });
        }
        catch (Exception er)
        {
            er.printStackTrace();
        }
    }

    public void runffb(View view) {
        if(mediaPlayer==null)
        {
            Toast.makeText(this,"NO CURRENTLY PLAYING SONG",Toast.LENGTH_SHORT).show();
            return;
        }
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()-5000);
    }

    public void runBack(View view) {
        if(mediaPlayer==null)
        {
            Toast.makeText(this,"NO CURRENTLY PLAYING SONG",Toast.LENGTH_SHORT).show();
            return;
        }
        if(currentSong==0)
        {
            mediaPlayer.seekTo(0);
        }
        else
        {
            currentSong-=1;
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer=null;
                playAudio();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void play(View view) {
        if(songs==null)
        {
            Toast.makeText(this,"NO SONG FOUND",Toast.LENGTH_LONG).show();
        }
        if(!isSongStarted)
        {
            Toast.makeText(this,"Loading",Toast.LENGTH_LONG).show();
            playAudio();
            play.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
            return;
        }
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            play.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
        }
        else
        {
            mediaPlayer.start();
            play.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
        }
    }

    public void runNext(View view) {
        currentSong+=1;
        Toast.makeText(this,"Loading",Toast.LENGTH_LONG).show();
        try {
            if(mediaPlayer!=null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            playAudio();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void runfff(View view) {
        if(mediaPlayer==null)
        {
            return;
        }
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+5000);
    }

    private void setUpNotification(){

        notificationStarted=true;

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // we need to build a basic notification first, then update it
        Intent intentNotif = new Intent(this, MainActivity.class);
        intentNotif.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intentNotif, PendingIntent.FLAG_UPDATE_CURRENT);

        // notification's layout
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        // notification's icon
        mRemoteViews.setImageViewResource(R.id.notif_icon, R.mipmap.ic_launcher);
        // notification's title
        mRemoteViews.setTextViewText(R.id.notif_title, ndat.get("title")+"");

        mRemoteViews.setTextViewText(R.id.notif_content,ndat.get("author")+"");

        mBuilder = new NotificationCompat.Builder(this);

        CharSequence ticker = getResources().getString(R.string.ticker_text);
        int apiVersion = Build.VERSION.SDK_INT;

        if (apiVersion < Build.VERSION_CODES.HONEYCOMB) {
            mNotification = new Notification(R.mipmap.ic_launcher, ticker, System.currentTimeMillis());
            mNotification.contentView = mRemoteViews;
            mNotification.contentIntent = pendIntent;

            mNotification.flags |= Notification.FLAG_NO_CLEAR; //Do not clear the notification
            mNotification.defaults |= Notification.DEFAULT_LIGHTS;

            // starting service with notification in foreground mode
            mNotificationManager.notify(NOTIF_ID, mNotification);

        }else if (apiVersion >= Build.VERSION_CODES.HONEYCOMB) {
            mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContent(mRemoteViews)
                    .setTicker(ticker);

            // starting service with notification in foreground mode
            mNotificationManager.notify(NOTIF_ID, mBuilder.build());
        }
    }

    // use this method to update the Notification's UI
    private void updateNotification(){

        int api = Build.VERSION.SDK_INT;
        // update the title
        mRemoteViews.setTextViewText(R.id.notif_title, ndat.get("title")+"");
        mRemoteViews.setTextViewText(R.id.notif_content,ndat.get("author")+"");

        // update the notification
        if (api < Build.VERSION_CODES.HONEYCOMB) {
            mNotificationManager.notify(NOTIF_ID, mNotification);
        }else if (api >= Build.VERSION_CODES.HONEYCOMB) {
            mNotificationManager.notify(NOTIF_ID, mBuilder.build());
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mediaPlayer=null;
        isSongStarted=false;
        mNotificationManager.cancel(NOTIF_ID);
        notificationStarted=false;
        mNotificationManager.cancelAll();
        mNotification=null;
    }

    public void share(View view) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "SHARED FROM GrayBot\nLink : "+songs.get(currentSong);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Song Shared from GrayBot");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share To"));
    }
}