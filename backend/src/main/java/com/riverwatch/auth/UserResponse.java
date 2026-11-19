package com.riverwatch.auth;

import com.riverwatch.user.Barangay;
import com.riverwatch.user.UserAccount;

public record UserResponse(
        Long userId, String fullName, String email, String phoneNumber,
        String role, String status, BarangayInfo barangay) {

    public record BarangayInfo(Long barangayId, String name, String cityMunicipality) {
        public static BarangayInfo of(Barangay b) {
            return b == null ? null : new BarangayInfo(b.getBarangayId(), b.getName(), b.getCityMunicipality());
        }
    }

    public static UserResponse of(UserAccount u) {
        return new UserResponse(u.getUserId(), u.getFullName(), u.getEmail(), u.getPhoneNumber(),
                u.getRole().name(), u.getAccountStatus().name(), BarangayInfo.of(u.getBarangay()));
    }
}
