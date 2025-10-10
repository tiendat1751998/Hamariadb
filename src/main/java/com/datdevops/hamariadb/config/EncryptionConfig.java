package com.datdevops.hamariadb.config;



import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "app.security.encryption")
public class EncryptionConfig {
    private Integer aesKeySize;
    private Integer rsaKeySize;
    private Integer keyExpirationHours;

    // Getters and Setters
    public Integer getAesKeySize() {
        return aesKeySize;
    }

    public void setAesKeySize(Integer aesKeySize) {
        this.aesKeySize = aesKeySize;
    }

    public Integer getRsaKeySize() {
        return rsaKeySize;
    }

    public void setRsaKeySize(Integer rsaKeySize) {
        this.rsaKeySize = rsaKeySize;
    }

    public Integer getKeyExpirationHours() {
        return keyExpirationHours;
    }

    public void setKeyExpirationHours(Integer keyExpirationHours) {
        this.keyExpirationHours = keyExpirationHours;
    }

    @Bean
    public com.datdevops.hamariadb.security.AESUtil aesUtil() {
        return new com.datdevops.hamariadb.security.AESUtil();
    }

    @Bean
    public com.datdevops.hamariadb.security.RSAUtil rsaUtil() {
        return new com.datdevops.hamariadb.security.RSAUtil();
    }
}
