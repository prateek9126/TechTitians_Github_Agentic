package com.mediscan.mediscan.controller;

import com.mediscan.mediscan.model.Doctor;
import com.mediscan.mediscan.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    // Search by City, State (optional) & Specialization
    @GetMapping("/search")
    public List<Doctor> searchDoctors(
            @RequestParam String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, defaultValue = "") String specialization) {

        return doctorService.searchDoctors(city, state, specialization);
    }

    // Search by Current Location (browser geolocation)
    @GetMapping("/nearby")
    public List<Doctor> nearbyDoctors(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "") String specialization) {

        return doctorService.findNearbyDoctors(lat, lon, specialization);
    }

    // Detect City and State from browser lat/lon coordinates
    @GetMapping("/detect-location")
    public java.util.Map<String, String> detectLocation(
            @RequestParam double lat,
            @RequestParam double lon) {

        return doctorService.detectLocation(lat, lon);
    }
}
