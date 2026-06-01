package com.wexinc.transaction.exception;

public class CurrencyConversionException extends RuntimeException {

    public CurrencyConversionException(String country, String currency) {
        super(String.format(
                "Purchase cannot be converted to the target currency. " +
                "No exchange rate available within 6 months of the purchase date for country='%s', currency='%s'.",
                country, currency));
    }
}
