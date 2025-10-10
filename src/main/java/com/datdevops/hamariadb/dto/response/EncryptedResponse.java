package com.datdevops.hamariadb.dto.response;

import lombok.Data;

@Data
public class EncryptedResponse {
    private String data;
    private String keyIdentifier;
    private String iv;
}
