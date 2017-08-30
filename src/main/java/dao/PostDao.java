package dao;

public class PostDao {
    private String url;
    private String title;
    private String body;

    public PostDao() {}

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostDao postDao = (PostDao) o;

        if (!url.equals(postDao.url)) return false;
        if (!title.equals(postDao.title)) return false;
        return body.equals(postDao.body);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + body.hashCode();
        return result;
    }
}
