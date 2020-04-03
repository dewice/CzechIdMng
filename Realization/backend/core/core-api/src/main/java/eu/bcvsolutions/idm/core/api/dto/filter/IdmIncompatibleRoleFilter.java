package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.RequestFilterPredicate;
import eu.bcvsolutions.idm.core.api.dto.IdmIncompatibleRoleDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;

/**
 * Segregation of Duties
 *
 * @author Radek Tomiška
 * @since 9.4.0
 */
public class IdmIncompatibleRoleFilter extends DataFilter implements ExternalIdentifiableFilter {
	/**
	 * Superior role
	 */
	public static final String PARAMETER_SUPERIOR_ID = "superiorId";
	/**
	 * Sub role
	 */
	public static final String PARAMETER_SUB_ID = "subId";
	/**
	 * All for role
	 */
	public static final String PARAMETER_ROLE_ID = "roleId";

	public IdmIncompatibleRoleFilter() {
		this(new LinkedMultiValueMap<>());
	}

	public IdmIncompatibleRoleFilter(MultiValueMap<String, Object> data) {
		super(IdmIncompatibleRoleDto.class, data);
	}

	@RequestFilterPredicate(field = "superior")
	public UUID getSuperiorId() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_SUPERIOR_ID));
	}

	public void setSuperiorId(UUID superiorId) {
		data.set(PARAMETER_SUPERIOR_ID, superiorId);
	}

	@RequestFilterPredicate(field = "sub")
	public UUID getSubId() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_SUB_ID));
	}

	public void setSubId(UUID subId) {
		data.set(PARAMETER_SUB_ID, subId);
	}

	public UUID getRoleId() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_ROLE_ID));
	}

	public void setRoleId(UUID roleId) {
		data.set(PARAMETER_ROLE_ID, roleId);
	}
}
