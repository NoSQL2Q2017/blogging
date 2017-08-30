package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.PostDao;
import dao.PublishedPostDao;
import dao.UserDao;
import dao.UserProfile;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RedisService {
    private static final int TEN_MINUTES_TTL = 600;
    private MapperService mapperService;
    private Jedis jedis;

    public RedisService(Jedis jedis, MapperService mapperService) {
        this.jedis = jedis;
        this.mapperService = mapperService;
    }

    //Users
    public void addUser(UserDao userDao) {
        jedis.watch("user:username:" + userDao.getUsername(), "consistency:email:" + userDao.getEmail());
        if (!this.validateUserConsistency(userDao)) {
            jedis.unwatch();
            throw new RuntimeException("username or email is already used");
        }

        Transaction transaction = jedis.multi();
        transaction.set("user:username:" + userDao.getUsername(), mapperService.getJsonString(userDao));
        transaction.set("consistency:email:" + userDao.getEmail(), "");
        transaction.exec();
    }

    public Optional<UserDao> getUser(String username) {
        return mapperService.getDao(jedis.get("user:username:" + username), UserDao.class);
    }

    public void logUser(String username, String hashedPassword, String ip) {
        String jsonUser = jedis.get("user:username:" + username);
        Optional<UserDao> userDao = mapperService.getDao(jsonUser, UserDao.class);
        if (!userDao.isPresent() || !userDao.get().getHashedPassword().equals(hashedPassword)) {
            throw new RuntimeException("User or password does not exist");
        }
        if (jedis.sismember("log_in:username:" + username, ip)) {
            jedis.setex("log_in:cookie:" + username.hashCode(), TEN_MINUTES_TTL, username);
        } else {
            //send email with confirmation
            throw new RuntimeException("Send email");
            //think a way to test
        }
    }

    private boolean validateUserConsistency(UserDao userDao) {
        return !jedis.exists("consistency:email:" + userDao.getEmail())
                && !jedis.exists("user:username:" + userDao.getUsername());
    }

    //Post
    public void createPost(UserDao owner, PostDao postDao) {
        jedis.lpush("post:username:" + owner.getUsername(), mapperService.getJsonString(postDao));
    }

    public void deletePost(UserDao owner, PostDao postDao) {
        jedis.lrem("post:username:" + owner.getUsername(), 1, mapperService.getJsonString(postDao));
    }

    public List<PostDao> getUserPosts(UserDao owner) {
        return mapperService.getDaoList(jedis.lrange("post:username:" + owner.getUsername(), 0, -1), PostDao.class);
    }

    public void publishPost(UserDao owner, PostDao postDao) {
        PublishedPostDao publishedPostDao = new PublishedPostDao(postDao);
        jedis.set("post:published:date:username:"+ owner.getUsername(), publishedPostDao.getPublishedDate());
        jedis.lpush("post:published:username:" + owner.getUsername(), mapperService.getJsonString(publishedPostDao));
    }

    public void deletePublishedPost(UserDao owner, PublishedPostDao publishedPostDao){
        jedis.lrem("post:published:username:" + owner.getUsername(), 1, mapperService.getJsonString(publishedPostDao));
        this.deletePost(owner, publishedPostDao.getPost());
    }

    public List<PublishedPostDao> getUserpublishedPosts(UserDao owner) {
        return this.getUserpublishedPostsByRange(owner, 0, -1);
    }

    public List<PublishedPostDao> getUserpublishedPostsByRange(UserDao owner, int lowerBound, int upperBound) {
        return mapperService.getDaoList(jedis.lrange("post:published:username:" + owner.getUsername(), lowerBound, upperBound), PublishedPostDao.class);
    }

    //User profile
    //Se podría haber usado scan, pero había que ver que onda con el cursor que guardabas
    public UserProfile getUserNewUserProfile(UserDao owner, BigDecimal postPerPage) {
        Long totalPublishedPost = jedis.llen("post:published:username:" + owner.getUsername());
        int pagesSize = new BigDecimal(totalPublishedPost).divide(postPerPage, BigDecimal.ROUND_UP).intValue();
        List<PublishedPostDao> publishedPost =  getUserpublishedPostsByRange(owner, 0, postPerPage.intValue() -1);
        String lastPublishedPostDate = jedis.get("post:published:date:username:" + owner.getUsername());
        return new UserProfile(owner, publishedPost, lastPublishedPostDate, postPerPage.intValue(), pagesSize);
    }

    public UserProfile getUserUserProfileInCertainPage(UserProfile userProfile, int pageNumber) {
        if(userProfile.getPagesSize() < pageNumber) throw new RuntimeException("pagina inexistente");
        List<PublishedPostDao> publishedPost =getUserpublishedPostsByRange(userProfile.getOwner(), userProfile.getLowerBoundForPage(pageNumber), userProfile.getUpperBoundForPage(pageNumber));
        return new UserProfile(userProfile, publishedPost);
    }

    //User autocomplete
    public List<String> getUsersAutocomplete(String username){
        ScanParams scanParams = new ScanParams();
        List<String> jsonStringKeys = jedis.scan("0", scanParams.match("user:username:*" + username + "*").count(10)).getResult();
        return jsonStringKeys.stream().map(keyString -> keyString.split(":")[2]).collect(Collectors.toList());
    }
}
