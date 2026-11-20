// Portado de piggymetrics (6bb2cf9) account-service/src/main/java/com/piggymetrics/account/domain/Currency.java. Adaptaciones: ver NOTICE.
package com.piggymetrics.account.domain;

public enum Currency {

	USD, EUR, RUB;

	public static Currency getDefault() {
		return USD;
	}
}
