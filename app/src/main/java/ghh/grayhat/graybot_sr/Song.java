package ghh.grayhat.graybot_sr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.chaquo.python.PyObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class Song {
    PyObject song;
    String source;
    public Song(PyObject data,String url)
    {
        song=data;
        source=url;
    }
    public String getTitle()
    {
        return ""+song.get("title");
    }
    public  String getChannel()
    {
        return ""+song.get("author");
    }
    public Bitmap getImage()
    {
        return getBitmapFromURL(""+song.get("thumb"));
    }
    public Bitmap getBitmapFromURL(String src) {
        try {
            /*AsyncHttpClient client=new AsyncHttpClient();
            client.get(src, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });*/
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
    public String getMrl()
    {
        return ""+song.callAttr("getbestaudio").get("url");
    }
    public String getThumb()
    {
        return ""+song.get("thumb");
    }
    public String getSource()
    {
        return source;
    }
}
