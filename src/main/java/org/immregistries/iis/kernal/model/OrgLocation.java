package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class OrgLocation extends AbstractMappedObject implements Serializable {
	
  private static final long serialVersionUID = 1L;
  
  private String orgLocationId = "";
  private String orgFacilityCode = "";
  private Tenant tenant = null;
  private String orgFacilityName = "";
  private String locationType = "";
  private String addressLine1 = "";
  private String addressLine2 = "";
  private String addressCity = "";
  private String addressState = "";
  private String addressZip = "";
  private String addressCountry = "";
  private String addressCountyParish = "";
  private String vfcProviderPin = "";

  public String getVfcProviderPin() {
    return vfcProviderPin;
  }

  public void setVfcProviderPin(String vfcProviderPin) {
    this.vfcProviderPin = vfcProviderPin;
  }

  public String getOrgLocationId() {
    return orgLocationId;
  }

  public void setOrgLocationId(String orgLocationId) {
    this.orgLocationId = orgLocationId;
  }

  public String getOrgFacilityCode() {
    return orgFacilityCode;
  }

  public void setOrgFacilityCode(String orgFacilityCode) {
    this.orgFacilityCode = orgFacilityCode;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }

  public String getOrgFacilityName() {
    return orgFacilityName;
  }

  public void setOrgFacilityName(String orgFacilityName) {
    this.orgFacilityName = orgFacilityName;
  }

  public String getLocationType() {
    return locationType;
  }

  public void setLocationType(String locationType) {
    this.locationType = locationType;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public String getAddressState() {
    return addressState;
  }

  public void setAddressState(String addressState) {
    this.addressState = addressState;
  }

  public String getAddressZip() {
    return addressZip;
  }

  public void setAddressZip(String addressZip) {
    this.addressZip = addressZip;
  }

  public String getAddressCountry() {
    return addressCountry;
  }

  public void setAddressCountry(String addressCountry) {
    this.addressCountry = addressCountry;
  }

  public String getAddressCountyParish() {
    return addressCountyParish;
  }

  public void setAddressCountyParish(String addressCountyParish) {
    this.addressCountyParish = addressCountyParish;
  }

  public String getAddressCity() {
    return addressCity;
  }

  public void setAddressCity(String addressCity) {
    this.addressCity = addressCity;
  }
}
