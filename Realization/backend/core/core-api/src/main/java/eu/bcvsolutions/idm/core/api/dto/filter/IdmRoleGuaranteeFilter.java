package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;


/**
 * Filter for {@link IdmRoleGuarantee}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 *
 */
public class IdmRoleGuaranteeFilter extends DataFilter {
	
	/**
	 * Owner role
	 */
	public static final String PARAMETER_ROLE = "role";
	/**
	 * guarantee as identity
	 */
	public static final String PARAMETER_GUARANTEE = "guarantee";
	
	public IdmRoleGuaranteeFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public IdmRoleGuaranteeFilter(MultiValueMap<String, Object> data) {
		super(IdmRoleGuaranteeDto.class, data);
	}
	
	public UUID getRole() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_ROLE));
	}
	
	public void setRole(UUID role) {
		data.set(PARAMETER_ROLE, role);
	}
	
	public UUID getGuarantee() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_GUARANTEE));
	}
	
	public void setGuarantee(UUID guarantee) {
		data.set(PARAMETER_GUARANTEE, guarantee);
	}
}
