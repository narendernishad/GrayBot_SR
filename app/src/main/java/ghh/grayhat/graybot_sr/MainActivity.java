package ghh.grayhat.graybot_sr;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    TextView txtv;
    PyObject pafy;
    private AsyncHttpClient client;
    ArrayList arrayList;
    public static List<Song> songList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchView = (SearchView) findViewById(R.id.search);
        listView = (ListView) findViewById(R.id.list);
        txtv=(TextView)findViewById(R.id.voiceS);
        client = new AsyncHttpClient();
        arrayList = new ArrayList();
        songList = new ArrayList<>();
        searchView.setSubmitButtonEnabled(true);
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getBaseContext()));
            }
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            pafy = Python.getInstance().getModule("pafy");
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(intent); // Handle text being sent
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(MainActivity.this, "Searching", Toast.LENGTH_SHORT).show();
                String url = "";
                try {
                    url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8");
                } catch (Exception e) {
                    url = "https://www.youtube.com/results?search_query=" + query.replace(' ', '+');
                }
                System.out.println("URL : "+url);
                client.get(url, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        String code=responseString;
                        //System.out.println("CODE : \n"+code);
                        List<String> links=new ArrayList<>();
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
                        updateList(links);
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
        PyObject dat=pafy.callAttr("new",sharedText);
        Song song=new Song(dat,sharedText);
        songList.add(song);
        playSong(0);
    }

    private void updateList(List<String> links) {
        ArrayAdapter adapter=new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,arrayList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSong(position);
            }
        });
        for (String link : links)
        {
            if(link.startsWith("https:"))
            {
                ;
            }
            else
            {
                continue;
            }
            try {
                PyObject dat=pafy.callAttr("new",link);
                Song song=new Song(dat,link);
                songList.add(song);
                arrayList.add(""+song.getTitle()+"\n"+song.getChannel());
                adapter.notifyDataSetChanged();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
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
                    txtv.setText(""+result.get(0));
                    searchView.setQuery(""+result.get(0),false);
                    searchView.setQuery(""+result.get(0),true);
                }
                break;
            }
        }
    }
}