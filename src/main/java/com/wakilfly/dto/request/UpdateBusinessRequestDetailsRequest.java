package com.wakilfly.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusinessRequestDetailsRequest {

    private String nidaNumber;
    private String tinNumber;
    private String companyName;
    private String idDocumentUrl;
    private String idBackDocumentUrl;
}
