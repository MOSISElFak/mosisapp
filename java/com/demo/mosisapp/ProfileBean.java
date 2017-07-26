package com.demo.mosisapp;

public class ProfileBean
{
    //beans
    private String username;
    private String name;
    private String lastName;
    private String phone;

    //ctor
    public ProfileBean() {}

    public ProfileBean(String username, String name, String lastName, String phone) {
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
    }

    //get set
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
