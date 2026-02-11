package com.wakilfly.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    // Optional: for "people nearby" / suggested friends (location, age, interests)
    private String currentCity; // Mji e.g. Dar es Salaam
    private String region;      // Mkoa e.g. Dar es Salaam, Mwanza
    private String country;     // Taifa e.g. Tanzania
    private java.time.LocalDate dateOfBirth;
    private String interests;   // Hobbies comma-separated e.g. Music,Sports,Tech

    // Optional: Agent referral code
    private String referralCode;
}
