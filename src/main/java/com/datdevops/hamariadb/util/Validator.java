package com.datdevops.hamariadb.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class Validator {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{10,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\\s\\./0-9]*$");
    private static final Pattern BANK_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}$");

    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public static boolean isValidBankCode(String bankCode) {
        return bankCode != null && BANK_CODE_PATTERN.matcher(bankCode).matches();
    }

    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isWithinTransferLimit(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.valueOf(Constants.MAX_SINGLE_TRANSFER)) <= 0;
    }

    public static boolean isValidFileType(String fileName, String[] allowedTypes) {
        if (fileName == null) return false;

        String fileExtension = getFileExtension(fileName).toLowerCase();
        for (String type : allowedTypes) {
            if (type.equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    private static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return lastIndex == -1 ? "" : fileName.substring(lastIndex + 1);
    }
}
