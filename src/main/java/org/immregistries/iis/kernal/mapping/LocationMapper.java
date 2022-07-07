package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.Address;
import org.hl7.fhir.r5.model.Location;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationMapper {

	@Autowired


	public static Location fhirLocation(OrgLocation ol) {
		Location location = new Location();
		if (ol != null) {
			location.setId(ol.getOrgFacilityCode());
			location.addIdentifier(MappingHelper.getFhirIdentifier("OrgLocation", ol.getOrgLocationId()));
			location.setName(ol.getOrgFacilityName());

			Address address = location.getAddress();
			address.addLine(ol.getAddressLine1());
			address.addLine(ol.getAddressLine2());
			address.setCity(ol.getAddressCity());
			address.setState(ol.getAddressState());
			address.setPostalCode(ol.getAddressZip());
			address.setCountry(ol.getAddressCountry());
		}
		return  location;
	}

	public static OrgLocation orgLocationFromFhir(Location l) {
		OrgLocation orgLocation = new OrgLocation();
		orgLocation.setOrgLocationId(l.getId());
		orgLocation.setOrgFacilityCode(l.getId());
		orgLocation.setOrgFacilityName(l.getName());
		orgLocation.setLocationType(l.getTypeFirstRep().getText());
		orgLocation.setAddressCity(l.getAddress().getLine().get(0).getValueNotNull());
		if (l.getAddress().getLine().size() > 1) {
			orgLocation.setAddressLine2(l.getAddress().getLine().get(1).getValueNotNull());
		}
		orgLocation.setAddressCity(l.getAddress().getCity());
		orgLocation.setAddressState(l.getAddress().getState());
		orgLocation.setAddressZip(l.getAddress().getPostalCode());
		orgLocation.setAddressCountry(l.getAddress().getCountry());
		return orgLocation;
	}

}