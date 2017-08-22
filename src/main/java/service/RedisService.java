package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.UserDao;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Optional;

public class RedisService {
    private static final int TEN_MINUTES_TTL = 600;
    private ObjectMapper objectMapper;
    private Jedis jedis;

    public RedisService(Jedis jedis, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    public void addUser(UserDao userDao) {
        String jsonUser = getJsonString(userDao);
        //problemas transaccionales :(
        if (!this.validateUserConsistency(userDao)) throw new RuntimeException("username or email is already used");
        jedis.set("user:username:" + userDao.getUsername(), this.getJsonString(userDao));
        jedis.set("consistency:email:" + userDao.getEmail(), "");
    }

    public Optional<UserDao> getUser(String username) {
        return this.getDao(jedis.get("user:username:" + username), UserDao.class);
    }

    public void logUser(String username, String hashedPassword, String ip) {
        String jsonUser = jedis.get("user:username:" + username);
        Optional<UserDao> userDao = this.getDao(jsonUser, UserDao.class);
        if(!userDao.isPresent() || !userDao.get().getHashedPassword().equals(hashedPassword)) {
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

    private <T> Optional<T> getDao(String jsonUser, Class<T> clazz) {
        if(jsonUser == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(jsonUser, clazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean validateUserConsistency(UserDao userDao) {
        return !jedis.exists("consistency:email:" + userDao.getEmail())
                && !jedis.exists("user:username:" + userDao.getUsername());
    }

    private String getJsonString(Object objectDao) {
        try {
            return objectMapper.writeValueAsString(objectDao);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
