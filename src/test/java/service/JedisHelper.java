package service;

import dao.PostDao;
import dao.UserDao;
import redis.clients.jedis.Jedis;

public class JedisHelper {

    public void boostrap(Jedis jedis) {
    }

    public UserDao getUserZaffa() {
        return new UserDao("zaffa", "zaffaHashedPassword","jz@gmail.com");
    }

    public UserDao getUserMaxi() {
        return new UserDao("maxi", "d3p3g4r","mz@gmail.com");
    }

    public PostDao getPaper() {
        return new PostDao("dblandit.com", "noSql", "saraza");
    }

    public PostDao getOtherPaper() {
        return new PostDao("despegar.com", "Data Lake", "smoke on the water");
    }
}
