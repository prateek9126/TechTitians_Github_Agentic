package com.mediscan.mediscan.model;

public class Doctor {

    private String name;
    private String specialization;
    private String hospital;
    private String address;
    private String phone;
    private String experience;
    private String rating;
    private String map;

    // New Fields
    private String website;
    private String email;
    private String openingHours;
    private String distance;
    private String qualification;
    private String department;
    private String consultationFee;
    private String availableDays;

    public Doctor() {

    }

    public Doctor(
            String name,
            String specialization,
            String hospital,
            String address,
            String phone,
            String experience,
            String rating,
            String map,
            String website,
            String email,
            String openingHours,
            String distance
    ) {

        this.name = name;
        this.specialization = specialization;
        this.hospital = hospital;
        this.address = address;
        this.phone = phone;
        this.experience = experience;
        this.rating = rating;
        this.map = map;
        this.website = website;
        this.email = email;
        this.openingHours = openingHours;
        this.distance = distance;

    }

    // =============================
    // Name
    // =============================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // =============================
    // Specialization
    // =============================

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    // =============================
    // Hospital
    // =============================

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    // =============================
    // Address
    // =============================

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // =============================
    // Phone
    // =============================

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // =============================
    // Experience
    // =============================

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    // =============================
    // Rating
    // =============================

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    // =============================
    // Map
    // =============================

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    // =============================
    // Website
    // =============================

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    // =============================
    // Email
    // =============================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // =============================
    // Opening Hours
    // =============================

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    // =============================
    // Distance
    // =============================

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    // =============================
    // Qualification
    // =============================

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    // =============================
    // Department
    // =============================

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // =============================
    // Consultation Fee
    // =============================

    public String getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(String consultationFee) {
        this.consultationFee = consultationFee;
    }

    // =============================
    // Available Days
    // =============================

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

}