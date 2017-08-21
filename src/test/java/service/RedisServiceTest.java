package service;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

public class RedisServiceTest {

    private Jedis jedis;
    private JedisHelper jedisHelper;

    @Before
    public void setUp(){
        this.jedis = new Jedis("localHost");
        this.jedisHelper = new JedisHelper();
        this.jedisHelper.boostrap(this.jedis);


    }

    @Test
    public void test(){}
}
