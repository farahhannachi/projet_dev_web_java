package org.example.model;

public class Address {
    private int id;
    private Integer userId;
    private String fullName;
    private String line1;
    private String line2;
    private String city;
    private String region;
    private String postalCode;
    private String country;
    private String phone;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String toSingleLine() {
        String l2 = line2 == null || line2.isBlank() ? "" : (line2 + ", ");
        return (line1 == null ? "" : line1)
                + ", " + l2
                + (city == null ? "" : city)
                + ", "
                + (region == null ? "" : region)
                + " "
                + (postalCode == null ? "" : postalCode)
                + ", "
                + (country == null ? "" : country);
    }

    @Override
    public String toString() {
        String name = fullName == null ? "" : fullName;
        return name + " - " + toSingleLine();
    }
}
