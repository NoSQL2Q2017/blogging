package dao;

public class PostDao {
    private String url;
    private String title;
    private String body;
    private String publishedDate;

    public PostDao() {}

    public PostDao(String url, String title, String body, String publishedDate) {
        this.url = url;
        this.title = title;
        this.body = body;
        this.publishedDate = publishedDate;
    }

    public PostDao(String url, String title, String body) {
        this.url = url;
        this.title = title;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }
}
