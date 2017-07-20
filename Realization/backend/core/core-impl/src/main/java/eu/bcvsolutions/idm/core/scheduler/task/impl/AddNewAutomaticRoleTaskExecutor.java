package eu.bcvsolutions.idm.core.scheduler.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleTreeNodeDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleTreeNodeService;

/**
 * Long running task for add newly added automatic role to users.
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 *
 */
@Service
@Description("Add new automatic role from IdmRoleTreeNode.")
public class AddNewAutomaticRoleTaskExecutor extends AbstractAutomaticRoleTaskExecutor {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AddNewAutomaticRoleTaskExecutor.class);
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdmRoleTreeNodeService roleTreeNodeService;
	@Autowired private IdmRoleService roleService;
	@Autowired private IdmRoleRequestService roleRequestService;
	@Autowired private IdmIdentityRoleService identityRoleService;

	@Override
	public void init(Map<String, Object> properties) {
		this.setRoleTreeNodeId(getParameterConverter().toUuid(properties, PARAMETER_ROLE_TREE_NODE));
		super.init(properties);
	}

	@Override
	public Boolean process() {
		IdmRoleTreeNodeDto roleTreeNode = roleTreeNodeService.get(getRoleTreeNodeId());
		if (roleTreeNode == null) {
			throw new ResultCodeException(CoreResultCode.AUTOMATIC_ROLE_TASK_EMPTY);
		}
		IdmRole role = roleService.get(roleTreeNode.getRole());
		//
		// TODO: pageable?
		List<IdmIdentityContractDto> contracts = identityContractService.findAllByWorkPosition(
				roleTreeNode.getTreeNode(), roleTreeNode.getRecursionType());
		//
		counter = 0L;
		count = Long.valueOf(contracts.size());
		//
		LOG.debug("Start: Add new role [{}] by automatic role [{}]. Count: [{}]", role.getCode(), roleTreeNode.getId(), count);
		//
		List<String> failedIdentities = new ArrayList<>();
		boolean canContinue = true;
		for (IdmIdentityContractDto identityContract : contracts) {
			List<IdmIdentityRoleDto> allByContract = identityRoleService.findAllByContract(identityContract.getId());
			// skip already assigned automatic roles
			boolean alreadyAssigned = false;
			for (IdmIdentityRoleDto roleByContract : allByContract) {
				if (ObjectUtils.equals(roleByContract.getRoleTreeNode(), roleTreeNode.getId())) {
					alreadyAssigned = true;
					break;
				}
			}
			if (alreadyAssigned) {
				counter++;
				if (!updateState()) {
					break;
				}
				continue;
			}

			IdmRoleRequestDto roleRequest = roleTreeNodeService.prepareAssignAutomaticRoles(identityContract, Sets.newHashSet(roleTreeNode));
			roleRequest = roleRequestService.startRequest(roleRequest.getId(), false);
			if (roleRequest.getState() != RoleRequestState.EXCEPTION) {
				counter++;
			} else {
				IdmIdentityDto identity = DtoUtils.getEmbedded(identityContract, IdmIdentityContract_.identity, IdmIdentityDto.class);
				failedIdentities.add(identity.getUsername());
			}
			canContinue = updateState();
			if (!canContinue) {
				break;
			}
		}
		LOG.debug("End: Add new role [{}] by automatic role [{}]. Count: [{}/{}]", role.getCode(), roleTreeNode.getId(), counter, count);
		if (!failedIdentities.isEmpty()) {
			throw new ResultCodeException(CoreResultCode.AUTOMATIC_ROLE_ASSIGN_TASK_NOT_COMPLETE,
					ImmutableMap.of(
							"role", role.getCode(),
							"roleTreeNode", roleTreeNode.getId(),
							"identities", StringUtils.join(failedIdentities, ",")));
		}
		return Boolean.TRUE;
	}
}
