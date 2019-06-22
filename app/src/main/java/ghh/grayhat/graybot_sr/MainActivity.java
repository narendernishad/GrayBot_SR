package ghh.grayhat.graybot_sr;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    private AsyncHttpClient client;
    ArrayList arrayList;
    public static List<Song> songList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchView = (SearchView) findViewById(R.id.search);
        listView = (ListView) findViewById(R.id.list);
        client = new AsyncHttpClient();
        client.setTimeout(30*1000);
        client.setConnectTimeout(30*1000);
        client.setResponseTimeout(30*1000);
        arrayList = new ArrayList();
        songList = new ArrayList<>();
        searchView.setSubmitButtonEnabled(true);
        try {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(intent); // Handle text being sent
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        searchView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                record(v);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query.startsWith("https://"))
                {
                    Toast.makeText(MainActivity.this,"Cannot search a url",Toast.LENGTH_SHORT).show();
                    return false;
                }
                Toast.makeText(MainActivity.this, "Searching : "+query, Toast.LENGTH_SHORT).show();
                String url = "https://gray-application.herokuapp.com/songs/list/"+query.replaceAll("\\s","+");
                System.out.println("URL : "+url);
                client.get(url, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String data=new String(responseBody);
                        Log.d("recievedData",data);
                        try {
                            JSONArray array=new JSONArray(data);
                            for(int i=0;i<array.length();i++)
                            {
                                JSONObject jsnobj = array.getJSONObject(i);
                                String link=jsnobj.getString("url");
                                String title=jsnobj.getString("title");
                                String author=jsnobj.getString("author");
                                String mrl=jsnobj.getString("mrl");
                                Song song=new Song(link,title,author,mrl);
                                songList.add(song);
                                arrayList.add(title+"\n"+author);
                            }
                            ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,arrayList);
                            listView.setAdapter(adapter);
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    playSong(position);
                                }
                            });
                        }
                        catch (Exception ex)
                        {
                            Log.d("fail","client error");
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if(sharedText==null)
            return;
        client=new AsyncHttpClient();
        client.setTimeout(30*1000);
        songList=new ArrayList<>();
        System.out.println(sharedText);
        String base="https://www.youtube.com/watch?v=";
        String base2="https://youtu.be/";
        String url="https://gray-application.herokuapp.com/songs/data/";
        /*if(sharedText.startsWith(base))
        {
            url+=sharedText.substring(base.length());
        }
        else if (sharedText.startsWith(base2))
        {
            url+=sharedText.substring(base2.length());
        }
        else
        {
            Toast.makeText(this,"Invalid url",Toast.LENGTH_SHORT).show();
            return;
        }*/
        url+=sharedText.substring(sharedText.lastIndexOf('/')+1);
        System.out.println("url : "+url);
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String data=new String(responseBody);
                Log.d("Detdata",data);
                try {
                    JSONObject jsonObject=new JSONObject(data);
                    String lnk=jsonObject.getString("url");
                    String tit=jsonObject.getString("title");
                    String aut=jsonObject.getString("author");
                    String mrl=jsonObject.getString("mrl");
                    Song song=new Song(lnk,tit,aut,mrl);
                    songList.add(song);
                    playSong(0);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void playSong(int position) {
        Intent player=new Intent(this,Player.class);
        player.putExtra("position",position);
        startActivity(player);
    }

    public void record(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
        try {
            startActivityForResult(intent, 3000);
        } catch (ActivityNotFoundException a) {
            a.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 3000: {
                if(resultCode == RESULT_OK && null!=data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    System.out.println(""+result.get(0));
                    searchView.setQueryHint(result.get(0)+"");
                    searchView.setQuery(""+result.get(0),true);
                }
                break;
            }
        }
    }
}