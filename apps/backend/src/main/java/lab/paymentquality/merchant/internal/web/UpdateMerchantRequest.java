package lab.paymentquality.merchant.internal.web;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * PATCH body: omitted fields are unchanged; JSON null clears contact fields.
 */
public final class UpdateMerchantRequest {

    private String displayName;
    private boolean displayNameSpecified;
    private String contactPhone;
    private boolean contactPhoneSpecified;
    private String contactAddress;
    private boolean contactAddressSpecified;

    @JsonSetter("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameSpecified = true;
    }

    @JsonSetter("contactPhone")
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
        this.contactPhoneSpecified = true;
    }

    @JsonSetter("contactAddress")
    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
        this.contactAddressSpecified = true;
    }

    public String displayName() {
        return displayName;
    }

    public boolean displayNameSpecified() {
        return displayNameSpecified;
    }

    public String contactPhone() {
        return contactPhone;
    }

    public boolean contactPhoneSpecified() {
        return contactPhoneSpecified;
    }

    public String contactAddress() {
        return contactAddress;
    }

    public boolean contactAddressSpecified() {
        return contactAddressSpecified;
    }
}
