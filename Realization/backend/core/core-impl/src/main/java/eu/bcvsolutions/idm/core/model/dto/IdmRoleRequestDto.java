package eu.bcvsolutions.idm.core.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdentityDto;
import eu.bcvsolutions.idm.core.model.domain.RoleRequestState;

/**
 * Dto for role request
 * 
 * @author svandav
 *
 */
public class IdmRoleRequestDto extends AbstractDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = IdentityDto.class)
	private UUID identity;
	private RoleRequestState state;
	private String wfProcessId;
	private String originalRequest;
	private List<IdmConceptRoleRequestDto> conceptRoles;
	private boolean executeImmediately = false;
	@Embedded(dtoClass = IdmRoleRequestDto.class)
	private UUID duplicatedToRequest;
	private String log;

	public RoleRequestState getState() {
		return state;
	}

	public void setState(RoleRequestState state) {
		this.state = state;
	}

	public String getWfProcessId() {
		return wfProcessId;
	}

	public void setWfProcessId(String wfProcessId) {
		this.wfProcessId = wfProcessId;
	}

	public String getOriginalRequest() {
		return originalRequest;
	}

	public void setOriginalRequest(String originalRequest) {
		this.originalRequest = originalRequest;
	}

	public UUID getIdentity() {
		return identity;
	}

	public void setIdentity(UUID identity) {
		this.identity = identity;
	}

	public List<IdmConceptRoleRequestDto> getConceptRoles() {
		if (conceptRoles == null) {
			conceptRoles = new ArrayList<>();
		}
		return conceptRoles;
	}

	public void setConceptRoles(List<IdmConceptRoleRequestDto> conceptRoles) {
		this.conceptRoles = conceptRoles;
	}

	public UUID getDuplicatedToRequest() {
		return duplicatedToRequest;
	}

	public void setDuplicatedToRequest(UUID duplicatedToRequest) {
		this.duplicatedToRequest = duplicatedToRequest;
	}

	public boolean isExecuteImmediately() {
		return executeImmediately;
	}

	public void setExecuteImmediately(boolean executeImmediately) {
		this.executeImmediately = executeImmediately;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

}