package com.spring.popj;

/**
 * @author pengshilin
 * @date 2023/2/25 21:41
 */
public class User {
    private String username;
    private String password;
    private String age;
    private String email;

    public User() {
    }

    public User(String username, String password, String age, String email) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "popj{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", age='" + age + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
