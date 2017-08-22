package service;

import dao.UserDao;
import redis.clients.jedis.Jedis;

public class JedisHelper {

    public void boostrap(Jedis jedis) {
    }

    public UserDao getUserZaffa() {
        return new UserDao("zaffa", "zaffaHashedPassword","jz@gmail.com");
    }
}
