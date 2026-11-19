package com.riverwatch.user;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "barangay")
public class Barangay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barangay_id")
    private Long barangayId;

    @Column(nullable = false)
    private String name;

    @Column(name = "city_municipality", nullable = false)
    private String cityMunicipality;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    public Long getBarangayId() { return barangayId; }
    public String getName() { return name; }
    public String getCityMunicipality() { return cityMunicipality; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
}
