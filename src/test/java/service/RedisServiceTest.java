package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.UserDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import redis.clients.jedis.Jedis;

public class RedisServiceTest {

    private Jedis jedis;
    private JedisHelper jedisHelper;
    private RedisService redisService;

    public @Rule ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp(){
        this.jedis = new Jedis("localHost");
        this.jedis.flushDB();
        this.redisService = new RedisService(jedis, new ObjectMapper());
        this.jedisHelper = new JedisHelper();
        this.jedisHelper.boostrap(this.jedis);


    }

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
}
