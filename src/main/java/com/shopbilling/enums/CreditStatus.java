package com.shopbilling.enums;

public enum CreditStatus {
    PENDING,    // nothing paid yet
    PARTIAL,    // some paid, balance remaining
    CLEARED     // fully paid
}