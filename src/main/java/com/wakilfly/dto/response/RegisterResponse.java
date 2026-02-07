package com.wakilfly.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after registration.
 * otpForDev is set only when app.dev-mode=true (development) so UI can show OTP without SMS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {

    private UserResponse user;
    /** Only present when app.dev-mode=true; do not use in production. */
    private String otpForDev;
}
