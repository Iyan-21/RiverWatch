package com.riverwatch.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/barangays")
public class BarangayController {
    public record BarangayDto(Long barangayId, String name, String cityMunicipality) {}

    private final BarangayRepository repo;

    public BarangayController(BarangayRepository repo) { this.repo = repo; }

    /** Public list used by the registration forms. */
    @GetMapping
    public List<BarangayDto> list() {
        return repo.findAllByOrderByCityMunicipalityAscNameAsc().stream()
                .map(b -> new BarangayDto(b.getBarangayId(), b.getName(), b.getCityMunicipality()))
                .toList();
    }
}
