package eu.bcvsolutions.idm.core.api.config.domain;

import java.util.ArrayList;
import java.util.List;

import eu.bcvsolutions.idm.core.api.service.Configurable;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;

/**
 * Configuration for identity (private - sec)
 * 
 * @author Radek Tomiška
 *
 */
public interface PrivateIdentityConfiguration extends Configurable {

	/**
	 * Supports authorization policies for extended form definitions and their values
	 * @deprecated @since 10.2.0 secured attributes will be supported only
	 */
	String PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED = 
			ConfigurationService.IDM_PRIVATE_PROPERTY_PREFIX + "core.identity.formAttributes.secured";
	boolean DEFAULT_IDENTITY_FORM_ATTRIBUTES_SECURED = false;
	
	@Override
	default String getConfigurableType() {
		return "identity";
	}
	
	@Override
	default boolean isDisableable() {
		return false;
	}
	
	@Override
	default public boolean isSecured() {
		return true;
	}
	
	@Override
	default List<String> getPropertyNames() {
		List<String> properties = new ArrayList<>(); // we are not using superclass properties - enable and order does not make a sense here
		properties.add(getPropertyName(PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED));
		return properties;
	}
	
	/**
	 * Returns public configuration
	 * 
	 * @return
	 */
	IdentityConfiguration getPublicConfiguration();
	
	/**
	 * Returns true, when supports authorization policies for extended form definitions and their values
	 * 
	 * @return
	 * @deprecated @since 10.2.0 secured attributes will be supported only, this configuration will be removed
	 */
	boolean isFormAttributesSecured();
	
}
