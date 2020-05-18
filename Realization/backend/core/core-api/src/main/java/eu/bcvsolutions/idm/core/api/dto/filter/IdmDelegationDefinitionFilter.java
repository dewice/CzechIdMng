package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;

/**
 * Filter for a definition of delegation.
 *
 * @author Vít Švanda
 *
 */
public class IdmDelegationDefinitionFilter extends DataFilter {

	public static final String PARAMETER_SOURCE_IDENTITY_ID = "sourceIdentityId";
	public static final String PARAMETER_TARGET_IDENTITY_ID = "targetIdentityId";
	public static final String PARAMETER_TYPE = "type";

	public IdmDelegationDefinitionFilter() {
		this(new LinkedMultiValueMap<>());
	}

	public IdmDelegationDefinitionFilter(MultiValueMap<String, Object> data) {
		super(IdmExportImportDto.class, data);
	}

	public UUID getSourceIdentity() {
		return getParameterConverter().toUuid(getData(), PARAMETER_SOURCE_IDENTITY_ID);
	}

	public void setSourceIdentity(UUID sourceIdentity) {
		set(PARAMETER_SOURCE_IDENTITY_ID, sourceIdentity);
	}

	public UUID getTargetIdentity() {
		return getParameterConverter().toUuid(getData(), PARAMETER_TARGET_IDENTITY_ID);
	}

	public void setTargetIdentity(UUID targetIdentity) {
		set(PARAMETER_TARGET_IDENTITY_ID, targetIdentity);
	}

	public String getType() {
		return getParameterConverter().toString(data, PARAMETER_TYPE);
	}

	public void setType(String type) {
		set(PARAMETER_TYPE, type);
	}

}
