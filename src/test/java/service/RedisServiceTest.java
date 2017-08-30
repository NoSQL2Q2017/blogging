package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.PostDao;
import dao.PublishedPostDao;
import dao.UserDao;
import dao.UserProfile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;

public class RedisServiceTest {

    private Jedis jedis;
    private JedisHelper jedisHelper;
    private RedisService redisService;

    public @Rule ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp(){
        this.jedis = new Jedis("localHost");
        this.jedis.flushDB();
        MapperService mapperService = new MapperService(new ObjectMapper());
        this.redisService = new RedisService(jedis, mapperService);
        this.jedisHelper = new JedisHelper();
        this.jedisHelper.boostrap(this.jedis);


    }

    //User
    @Test
    public void addValidUser(){
        UserDao zaffa = this.jedisHelper.getUserZaffa();
        redisService.addUser(zaffa);
        Assert.assertEquals(zaffa, redisService.getUser(zaffa.getUsername()).get());
    }

    @Test
    public void failInAddingUserWithUsernameAlreadyTaken(){
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("username or email is already used");

        UserDao zaffa = this.jedisHelper.getUserZaffa();
        redisService.addUser(zaffa);
        zaffa.setEmail("differentEmail");
        redisService.addUser(zaffa);
    }


    @Test
    public void failInAddingUserEmailAlreadyTaken(){
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("username or email is already used");

        UserDao zaffa = this.jedisHelper.getUserZaffa();
        redisService.addUser(zaffa);
        zaffa.setUsername("differentUsername");
        redisService.addUser(zaffa);
    }

    @Test
    public void userLogInWithIpAlreadyUsedGenerateCookieWith10MinutesTTL(){
        UserDao zaffa = this.jedisHelper.getUserZaffa();
        String ip = "192.168.1.23";
        redisService.addUser(zaffa);
        this.jedis.sadd("log_in:username:" + zaffa.getUsername(), ip);
        redisService.logUser(zaffa.getUsername(), zaffa.getHashedPassword(), ip);

        Assert.assertEquals(zaffa.getUsername(), this.jedis.get("log_in:cookie:" + zaffa.getUsername().hashCode()));
        Assert.assertEquals(600, this.jedis.ttl("log_in:cookie:" + zaffa.getUsername().hashCode()),10);
    }

    @Test
    public void userLogInWithNewIpThrowsException(){
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Send email");


        UserDao zaffa = this.jedisHelper.getUserZaffa();
        String ip = "192.168.1.23";
        redisService.addUser(zaffa);
        redisService.logUser(zaffa.getUsername(), zaffa.getHashedPassword(), ip);
    }


    @Test
    public void userLogInWithWrongPasswordThrowsException(){
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("User or password does not exist");


        UserDao zaffa = this.jedisHelper.getUserZaffa();
        String ip = "192.168.1.23";
        redisService.addUser(zaffa);
        redisService.logUser(zaffa.getUsername(), "wrongPassword", ip);
    }


    @Test
    public void userLogInWithNonExistentUsernameThrowsException(){
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("User or password does not exist");


        UserDao zaffa = this.jedisHelper.getUserZaffa();
        String ip = "192.168.1.23";
        redisService.logUser(zaffa.getUsername(), zaffa.getHashedPassword(), ip);
    }

    //Post
    @Test
    public void createPost(){
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        List<PostDao> posts = redisService.getUserPosts(this.jedisHelper.getUserZaffa());

        Assert.assertEquals(1, posts.size());
        Assert.assertEquals(this.jedisHelper.getPaper(), posts.get(0));
    }

    @Test
    public void deletePost(){
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.deletePost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        List<PostDao> posts = redisService.getUserPosts(this.jedisHelper.getUserZaffa());

        Assert.assertTrue(posts.isEmpty());

    }

    @Test
    public void publishedPost(){
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        List<PostDao> posts = redisService.getUserPosts(this.jedisHelper.getUserZaffa());
        List<PublishedPostDao> publishedPosts = redisService.getUserpublishedPosts(this.jedisHelper.getUserZaffa());


        Assert.assertEquals(posts.get(0), publishedPosts.get(0).getPost());
    }

    @Test
    public void getPublishedPost(){
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());

        PublishedPostDao publishedPost = redisService.getPublishedPost(this.jedisHelper.getPaper().getUrl());


        Assert.assertEquals(this.jedisHelper.getPaper(), publishedPost.getPost());
    }

    @Test
    public void deletePublishedPost(){
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        List<PublishedPostDao> auxPublishedPosts = redisService.getUserpublishedPosts(this.jedisHelper.getUserZaffa());
        redisService.deletePublishedPost(this.jedisHelper.getUserZaffa(), auxPublishedPosts.get(0));

        List<PostDao> posts = redisService.getUserPosts(this.jedisHelper.getUserZaffa());
        List<PublishedPostDao> publishedPosts = redisService.getUserpublishedPosts(this.jedisHelper.getUserZaffa());


        Assert.assertTrue(posts.isEmpty());
        Assert.assertTrue(publishedPosts.isEmpty());
    }

    //User profile
    @Test
    public void userProfileInPageOneWithOnePostPerPage() {
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getOtherPaper());
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getChebotkoPaper());

        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getOtherPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getChebotkoPaper());

        UserProfile zaffaProfile = redisService.getUserNewUserProfile(this.jedisHelper.getUserZaffa(), new BigDecimal(1));
        Assert.assertEquals(3, zaffaProfile.getPagesSize());
        Assert.assertEquals(1, zaffaProfile.getPublishedPost().size());
        Assert.assertEquals(this.jedisHelper.getChebotkoPaper(), zaffaProfile.getPublishedPost().get(0).getPost());//LastPublishedPaper

    }

    //User profile
    @Test
    public void userProfileInPageTwoWithTwoPostPerPage() {
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getOtherPaper());
        redisService.createPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getChebotkoPaper());

        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getOtherPaper());
        redisService.publishPost(this.jedisHelper.getUserZaffa(), this.jedisHelper.getChebotkoPaper());

        UserProfile zaffaProfilePageOne = redisService.getUserNewUserProfile(this.jedisHelper.getUserZaffa(), new BigDecimal(2));
        UserProfile zaffaProfile = redisService.getUserUserProfileInCertainPage(zaffaProfilePageOne, 2);

        Assert.assertEquals(2, zaffaProfile.getPagesSize());
        Assert.assertEquals(1, zaffaProfile.getPublishedPost().size());
        Assert.assertEquals(this.jedisHelper.getPaper(), zaffaProfile.getPublishedPost().get(0).getPost());//FirstPublishedPaper

    }


    //User autocomplete
    @Test
    public void userAutocompleteWithTwoUsers(){
        this.redisService.addUser(this.jedisHelper.getUserZaffa());
        this.redisService.addUser(this.jedisHelper.getUserMaxi());

        List<String> autocompletedUsernames = this.redisService.getUsersAutocomplete("a");

        Assert.assertEquals(2, autocompletedUsernames.size());
        Assert.assertTrue(autocompletedUsernames.contains(this.jedisHelper.getUserZaffa().getUsername()));
        Assert.assertTrue(autocompletedUsernames.contains(this.jedisHelper.getUserMaxi().getUsername()));
    }

}
