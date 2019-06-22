package ghh.grayhat.graybot_sr;

public class Song {

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getMrl() {
        return mrl;
    }

    private String url;
    private String title;
    private String author;
    private String mrl;

    public Song(String source,String head,String channel,String media)
    {
        url=source;
        title=head;
        author=channel;
        mrl=media;
    }

}
