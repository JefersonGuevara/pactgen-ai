// Portado de piggymetrics (6bb2cf9) account-service/src/main/java/com/piggymetrics/account/domain/Item.java. Adaptaciones: ver NOTICE.
package com.piggymetrics.account.domain;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class Item {

	@NotNull
	@Length(min = 1, max = 20)
	private String title;

	@NotNull
	private BigDecimal amount;

	@NotNull
	private Currency currency;

	@NotNull
	private TimePeriod period;

	@NotNull
	private String icon;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public TimePeriod getPeriod() {
		return period;
	}

	public void setPeriod(TimePeriod period) {
		this.period = period;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}
