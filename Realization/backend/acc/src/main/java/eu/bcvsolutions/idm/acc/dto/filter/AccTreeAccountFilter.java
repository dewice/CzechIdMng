package eu.bcvsolutions.idm.acc.dto.filter;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;

/**
 * Filter for tree - accounts
 * 
 * @author Svanda
 *
 */
public class AccTreeAccountFilter implements BaseFilter, EntityAccountFilter {

	private UUID accountId;
	private UUID treeNodeId;
	private UUID systemId;
	private UUID roleSystemId;
	private Boolean ownership;

	@Override
	public Boolean isOwnership() {
		return ownership;
	}

	@Override
	public void setOwnership(Boolean ownership) {
		this.ownership = ownership;
	}

	@Override
	public UUID getAccountId() {
		return accountId;
	}

	@Override
	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}
	
	@Override
	public void setSystemId(UUID systemId) {
		this.systemId = systemId;
	}
	
	public UUID getSystemId() {
		return systemId;
	}

	public UUID getRoleSystemId() {
		return roleSystemId;
	}

	public void setRoleSystemId(UUID roleSystemId) {
		this.roleSystemId = roleSystemId;
	}

	public UUID getTreeNodeId() {
		return treeNodeId;
	}

	public void setTreeNodeId(UUID treeNodeId) {
		this.treeNodeId = treeNodeId;
	}

	@Override
	public void setEntityId(UUID entityId) {
		this.treeNodeId = entityId;
	}
	
	
}
