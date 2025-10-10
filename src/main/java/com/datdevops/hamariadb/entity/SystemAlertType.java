package com.datdevops.hamariadb.entity;

public enum SystemAlertType {
    SYSTEM_OVERLOAD,    // Hệ thống quá tải
    HIGH_LATENCY,       // Độ trễ cao
    ERROR_RATE,         // Tỷ lệ lỗi cao
    SECURITY_BREACH     // Vi phạm bảo mật
}
