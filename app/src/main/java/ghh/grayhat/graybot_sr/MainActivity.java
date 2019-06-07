package ghh.grayhat.graybot_sr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

    Button ffb,back,play,next,fff;
    EditText queryS;
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
    ProgressBar pg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            isSongStarted=false;
            currentSong=0;
            thumb=(ImageView)findViewById(R.id.thumbnail);
            title=(TextView)findViewById(R.id.title);
            channel=(TextView)findViewById(R.id.channel);
            ffb=(Button)findViewById(R.id.ffb);
            ffb.setText("<<<");
            queryS=(EditText)findViewById(R.id.query);
            back=(Button)findViewById(R.id.back);
            back.setText("<<");
            play=(Button)findViewById(R.id.play);
            play.setText("[>");
            next=(Button)findViewById(R.id.next);
            next.setText(">>");
            fff=(Button)findViewById(R.id.fff);
            fff.setText(">>>");
            pg=(ProgressBar)findViewById(R.id.progress);
            try
            {
                if (! Python.isStarted()) {
                    Python.start(new AndroidPlatform(getBaseContext()));
                }
                pafy = Python.getInstance().getModule("pafy");
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            seek=(SeekBar)findViewById(R.id.seek);
            pg.setVisibility(pg.INVISIBLE);
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
    }

    private void seekAudio(int progress, boolean fromUser) {
        if(mediaPlayer != null && fromUser){
            mediaPlayer.seekTo(progress);
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
            pg.setVisibility(pg.VISIBLE);
            URL url = new URL("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8"));
            HttpsURLConnection urlConnection=(HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            String code = toString(urlConnection.getInputStream());
            pg.setVisibility(pg.INVISIBLE);
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
        String query=""+queryS.getText();
        Toast.makeText(this,"SEARCHING...",Toast.LENGTH_SHORT).show();
        songs = getLinks(query);
        isSongStarted=false;
        currentSong=0;
        setView();
        Toast.makeText(this,"SEARCHING DONE",Toast.LENGTH_SHORT).show();
    }

    private void setView() {
        pg.setVisibility(pg.VISIBLE);
        ndat=pafy.callAttr("new",""+songs.get(currentSong));
        PyObject titl=ndat.get("title");
        PyObject img=ndat.get("thumb");
        pg.setVisibility(pg.INVISIBLE);
        try {
            thumb.setImageBitmap(getBitmapFromURL(""+img));
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
        Toast.makeText(this,"PLAYING : "+ndat.get("title"),Toast.LENGTH_LONG).show();
        if(currentSong>=songs.size())
        {
            return;
        }
        mediaPlayer=new MediaPlayer();
        isSongStarted=true;
        setView();
        try {
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getUrl());
            pg.setVisibility(pg.VISIBLE);
            mediaPlayer.prepare();
            pg.setVisibility(pg.INVISIBLE);
            mediaPlayer.start();
            seek.setMax(getDuration()*1000);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    play.setText("|>");
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    isSongStarted=false;
                    mediaPlayer=null;
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
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()-5000);
    }

    public void runBack(View view) {
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
        if(!isSongStarted)
        {
            Toast.makeText(this,"Loading",Toast.LENGTH_LONG).show();
            play.setText("O");
            playAudio();
            play.setText("||");
            return;
        }
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            play.setText("|>");
        }
        else
        {
            mediaPlayer.start();
            play.setText("||");
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
        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+5000);
    }

}