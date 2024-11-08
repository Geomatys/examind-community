package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Billing {
    @JsonProperty("currency")
    private String currency = "EUR";

    @JsonProperty("default_plan")
    private String defaultPlan;

    @JsonProperty("plans")
    @Valid
    private List<BillingPlan> plans = null;

    public Billing(String currency, String defaultPlan, List<BillingPlan> plans) {
        this.currency = currency;
        this.defaultPlan = defaultPlan;
        this.plans = plans;
    }

    public Billing currency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Billing defaultPlan(String defaultPlan) {
        this.defaultPlan = defaultPlan;
        return this;
    }

    public String getDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(String defaultPlan) {
        this.defaultPlan = defaultPlan;
    }

    public Billing plans(List<BillingPlan> plans) {
        this.plans = plans;
        return this;
    }

    public Billing addPlansItem(BillingPlan plansItem) {
        if (this.plans == null) {
            this.plans = new ArrayList<>();
        }
        this.plans.add(plansItem);
        return this;
    }

    public List<BillingPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<BillingPlan> plans) {
        this.plans = plans;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Billing billing = (Billing) o;
        return Objects.equals(this.currency, billing.currency) &&
                Objects.equals(this.defaultPlan, billing.defaultPlan) &&
                Objects.equals(this.plans, billing.plans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, defaultPlan, plans);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Billing {\n");

        sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
        sb.append("    defaultPlan: ").append(toIndentedString(defaultPlan)).append("\n");
        sb.append("    plans: ").append(toIndentedString(plans)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
