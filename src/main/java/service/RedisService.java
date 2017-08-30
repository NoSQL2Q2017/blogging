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
    private ObjectMapper objectMapper;
    private Jedis jedis;

    public RedisService(Jedis jedis, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    //Users
    public void addUser(UserDao userDao) {
        jedis.watch("user:username:" + userDao.getUsername(), "consistency:email:" + userDao.getEmail());
        if (!this.validateUserConsistency(userDao)) {
            jedis.unwatch();
            throw new RuntimeException("username or email is already used");
        }

        Transaction transaction = jedis.multi();
        transaction.set("user:username:" + userDao.getUsername(), this.getJsonString(userDao));
        transaction.set("consistency:email:" + userDao.getEmail(), "");
        transaction.exec();
    }

    public Optional<UserDao> getUser(String username) {
        return this.getDao(jedis.get("user:username:" + username), UserDao.class);
    }

    public void logUser(String username, String hashedPassword, String ip) {
        String jsonUser = jedis.get("user:username:" + username);
        Optional<UserDao> userDao = this.getDao(jsonUser, UserDao.class);
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
        jedis.lpush("post:username:" + owner.getUsername(), this.getJsonString(postDao));
    }

    public void deletePost(UserDao owner, PostDao postDao) {
        jedis.lrem("post:username:" + owner.getUsername(), 1, this.getJsonString(postDao));
    }

    public List<PostDao> getUserPosts(UserDao owner) {
        return this.getDaoList(jedis.lrange("post:username:" + owner.getUsername(), 0, -1), PostDao.class);
    }

    public void publishPost(UserDao owner, PostDao postDao) {
        PublishedPostDao publishedPostDao = new PublishedPostDao(postDao);
        jedis.set("post:published:date:username:"+ owner.getUsername(), publishedPostDao.getPublishedDate());
        jedis.lpush("post:published:username:" + owner.getUsername(), this.getJsonString(publishedPostDao));
    }

    public void deletePublishedPost(UserDao owner, PublishedPostDao publishedPostDao){
        jedis.lrem("post:published:username:" + owner.getUsername(), 1, this.getJsonString(publishedPostDao));
        this.deletePost(owner, publishedPostDao.getPost());
    }

    public List<PublishedPostDao> getUserpublishedPosts(UserDao owner) {
        return this.getUserpublishedPostsByRange(owner, 0, -1);
    }

    public List<PublishedPostDao> getUserpublishedPostsByRange(UserDao owner, int lowerBound, int upperBound) {
        return this.getDaoList(jedis.lrange("post:published:username:" + owner.getUsername(), lowerBound, upperBound), PublishedPostDao.class);
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

    //User search
    public List<String> getUsersAutocomplete(String username){
        ScanParams scanParams = new ScanParams();
        List<String> jsonStringKeys = jedis.scan("0", scanParams.match("user:username:*" + username + "*").count(10)).getResult();
        return jsonStringKeys.stream().map(keyString -> keyString.split(":")[2]).collect(Collectors.toList());
    }


    //Move to Mapper Service
    private String getJsonString(Object objectDao) {
        try {
            return objectMapper.writeValueAsString(objectDao);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Optional<T> getDao(String jsonUser, Class<T> clazz) {
        if (jsonUser == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(jsonUser, clazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private <T> List<T> getDaoList(List<String> jsonList, Class<T> clazz) {
        return jsonList.stream().map(jsonString -> this.getDao(jsonString, clazz))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
