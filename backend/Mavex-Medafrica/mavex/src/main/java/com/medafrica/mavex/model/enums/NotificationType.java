package com.medafrica.mavex.model.enums;
/**
 * PAYMENT_INVOICE_NO_AMOUNT  = Version 1 (email sans montant - mail standard Med Africa)
 * PAYMENT_INVOICE_WITH_AMOUNT = Version 2 (email avec montant USD xxx)
 */
public enum NotificationType {
    PAYMENT_INVOICE_NO_AMOUNT,
    PAYMENT_INVOICE_WITH_AMOUNT,
    PAYMENT_CONFIRMED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_REJECTED,
    PASSWORD_RESET,
    OTP_SENT,
    PROFILE_UPDATE_NEEDED,
    DELIVERY_UPDATE
}