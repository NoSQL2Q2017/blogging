package dao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PublishedPostDao {
    private PostDao post;
    private String publishedDate;

    public PublishedPostDao() {}
    public PublishedPostDao(PostDao post) {
        this.post = post;
        this.publishedDate = DateTimeFormatter.ISO_DATE.format(LocalDate.now());
    }

    public PostDao getPost() {
        return post;
    }

    public void setPost(PostDao post) {
        this.post = post;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublishedPostDao that = (PublishedPostDao) o;

        if (!post.equals(that.post)) return false;
        return publishedDate.equals(that.publishedDate);
    }

    @Override
    public int hashCode() {
        int result = post.hashCode();
        result = 31 * result + publishedDate.hashCode();
        return result;
    }
}
