package dao;

public class UserDao {
    private String username;
    private String hashedPassword;
    private String email;

    public UserDao() {}

    public UserDao(String username, String hashedPassword, String email) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDao userDao = (UserDao) o;

        if (!username.equals(userDao.username)) return false;
        if (!hashedPassword.equals(userDao.hashedPassword)) return false;
        return email.equals(userDao.email);
    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + hashedPassword.hashCode();
        result = 31 * result + email.hashCode();
        return result;
    }
}
