package com.riverwatch.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarangayRepository extends JpaRepository<Barangay, Long> {
    List<Barangay> findAllByOrderByCityMunicipalityAscNameAsc();
}
