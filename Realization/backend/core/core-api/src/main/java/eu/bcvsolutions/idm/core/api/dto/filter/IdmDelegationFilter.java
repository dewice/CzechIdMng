package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;

/**
 * Filter for a delegation.
 *
 * @author Vít Švanda
 *
 */
public class IdmDelegationFilter extends DataFilter {

	public static final String PARAMETER_SOURCE_IDENTITY_ID = "sourceIdentityId";
	public static final String PARAMETER_TARGET_IDENTITY_ID = "targetIdentityId";
	public static final String PARAMETER_OWNER_TYPE = "ownerType";
	public static final String PARAMETER_OWNER_ID = "ownerId";

	public IdmDelegationFilter() {
		this(new LinkedMultiValueMap<>());
	}

	public IdmDelegationFilter(MultiValueMap<String, Object> data) {
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

	public UUID getOwnerId() {
		return getParameterConverter().toUuid(getData(), PARAMETER_OWNER_ID);
	}

	public void setOwnerId(UUID ownerId) {
		set(PARAMETER_OWNER_ID, ownerId);
	}

	public String getOwnerType() {
		return getParameterConverter().toString(getData(), PARAMETER_OWNER_TYPE);
	}

	public void setOwnerType(String ownerType) {
		set(PARAMETER_OWNER_TYPE, ownerType);
	}

}
