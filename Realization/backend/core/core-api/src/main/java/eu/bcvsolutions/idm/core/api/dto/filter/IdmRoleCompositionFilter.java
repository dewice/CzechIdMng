package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.RequestFilterPredicate;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCompositionDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;


/**
 * Filter for {@link IdmRoleComposition}
 *
 * TODO: common composition filter?
 *
 * @author Radek Tomiška
 * @since 9.0.0
 */
public class IdmRoleCompositionFilter extends DataFilter implements ExternalIdentifiableFilter {
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

	public IdmRoleCompositionFilter() {
		this(new LinkedMultiValueMap<>());
	}

	public IdmRoleCompositionFilter(MultiValueMap<String, Object> data) {
		super(IdmRoleCompositionDto.class, data);
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
