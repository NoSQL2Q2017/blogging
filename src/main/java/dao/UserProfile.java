package dao;

import java.util.List;

public class UserProfile {

    private UserDao owner;
    private List<PublishedPostDao> publishedPost;
    private String lastPublishedPostDate;
    private int postPerPage;
    private int pagesSize;

    public UserProfile(){}

    public UserProfile(UserProfile userProfile, List<PublishedPostDao> publishedPost) {
        this.owner = userProfile.getOwner();
        this.lastPublishedPostDate = userProfile.getLastPublishedPostDate();
        this.postPerPage = userProfile.getPostPerPage();
        this.pagesSize = userProfile.getPagesSize();
        this.publishedPost = publishedPost;
    }

    public UserProfile(UserDao owner, List<PublishedPostDao> publishedPost, String lastPublishedPostDate, int postPerPage, int pagesSize) {
        this.owner = owner;
        this.publishedPost = publishedPost;
        this.lastPublishedPostDate = lastPublishedPostDate;
        this.postPerPage = postPerPage;
        this.pagesSize = pagesSize;
    }

    public int getLowerBoundForPage(int pageNumber) {
        return pageNumber * this.postPerPage - postPerPage;
    }

    public int getUpperBoundForPage(int pageNumber) {
        return pageNumber * this.postPerPage - 1;
    }

    public List<PublishedPostDao> getPublishedPost() {
        return publishedPost;
    }

    public void setPublishedPost(List<PublishedPostDao> publishedPost) {
        this.publishedPost = publishedPost;
    }

    public String getLastPublishedPostDate() {
        return lastPublishedPostDate;
    }

    public void setLastPublishedPostDate(String lastPublishedPostDate) {
        this.lastPublishedPostDate = lastPublishedPostDate;
    }

    public int getPostPerPage() {
        return postPerPage;
    }

    public void setPostPerPage(int postPerPage) {
        this.postPerPage = postPerPage;
    }

    public int getPagesSize() {
        return pagesSize;
    }

    public void setPagesSize(int pagesSize) {
        this.pagesSize = pagesSize;
    }

    public UserDao getOwner() {
        return owner;
    }

    public void setOwner(UserDao owner) {
        this.owner = owner;
    }
}
