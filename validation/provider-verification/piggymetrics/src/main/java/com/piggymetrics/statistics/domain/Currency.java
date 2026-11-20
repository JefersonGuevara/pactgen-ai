// Portado de piggymetrics (6bb2cf9) statistics-service/src/main/java/com/piggymetrics/statistics/domain/Currency.java. Adaptaciones: ver NOTICE.
package com.piggymetrics.statistics.domain;

public enum Currency {

	USD, EUR, RUB;

	public static Currency getBase() {
		return USD;
	}
}
