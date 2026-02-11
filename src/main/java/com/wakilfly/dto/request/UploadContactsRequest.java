package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request to upload contact list for "People You May Know".
 * Phone numbers and emails are stored hashed and matched against existing users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadContactsRequest {

    @Builder.Default
    @NotNull(message = "Phones list required (can be empty)")
    @Size(max = 5000, message = "At most 5000 phone numbers")
    private List<String> phones = new ArrayList<>();

    @Builder.Default
    @NotNull(message = "Emails list required (can be empty)")
    @Size(max = 5000, message = "At most 5000 emails")
    private List<String> emails = new ArrayList<>();
}
