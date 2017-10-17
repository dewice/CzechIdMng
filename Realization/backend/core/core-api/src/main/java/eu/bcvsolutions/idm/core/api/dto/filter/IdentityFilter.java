package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;

/**
 * Filter for identities
 * 
 * @author Radek Tomiška
 *
 */
public class IdentityFilter extends DataFilter implements CorrelationFilter {
	
	/**
	 * Identity by username
	 */
	public static final String PARAMETER_USERNAME = "username";
	/**
	 * Subordinates for given identity
	 */
	public static final String PARAMETER_SUBORDINATES_FOR = "subordinatesFor";
	/**
	 * Subordinates by given tree structure
	 */
	public static final String PARAMETER_SUBORDINATES_BY_TREE_TYPE = "subordinatesByTreeType";
	/**
	 * Managers for given identity
	 */
	public static final String PARAMETER_MANAGERS_FOR = "managersFor";
	/**
	 * Managers by given tree structure
	 */
	public static final String PARAMETER_MANAGERS_BY_TREE_TYPE = "managersByTreeType";
	/**
	 * Returns managers by identity's contract working prosition 
	 */
	public static final String PARAMETER_MANAGERS_BY_CONTRACT = "managersByContract";
	
	/**
	 * roles - OR
	 */
	private List<UUID> roles;	
	/**
	 * Little dynamic search by identity property and value
	 */
	private String property;
	private String value;
	/**
	 * Identities for tree structure (by identity contract)
	 */
	private UUID treeNode;
	/**
	 * Identities for tree structure recursively down
	 */
	private boolean recursively = true;
	/**
	 * Identities for tree structure (by identity contract)
	 */
	private UUID treeType;
	/**
	 * managers with contract guarantees included
	 */
	private boolean includeGuarantees = true;
	/**
	 * Enabled, disable or empty filter for disabled identities
	 */
	private Boolean disabled;
	/**
	 * Identity first name - exact match
	 */
	private String firstName;
	/**
	 * Identity last name - exact match
	 */
	private String lastName;
	
	public IdentityFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public IdentityFilter(MultiValueMap<String, Object> data) {
		super(IdmIdentityDto.class, data);
	}
	
	public String getUsername() {
		return (String) data.getFirst(PARAMETER_USERNAME);
	}

	public void setUsername(String username) {
		data.set(PARAMETER_USERNAME, username);
	}

	public UUID getSubordinatesFor() {
		return (UUID) data.getFirst(PARAMETER_SUBORDINATES_FOR);
	}

	public void setSubordinatesFor(UUID subordinatesFor) {
		data.set(PARAMETER_SUBORDINATES_FOR, subordinatesFor);
	}

	public UUID getSubordinatesByTreeType() {
		return (UUID) data.getFirst(PARAMETER_SUBORDINATES_BY_TREE_TYPE);
	}

	public void setSubordinatesByTreeType(UUID subordinatesByTreeType) {
		data.set(PARAMETER_SUBORDINATES_BY_TREE_TYPE, subordinatesByTreeType);
	}
	
	public void setManagersFor(UUID managersFor) {
		data.set(PARAMETER_MANAGERS_FOR, managersFor);
	}
	
	public UUID getManagersFor() {
		return (UUID) data.getFirst(PARAMETER_MANAGERS_FOR);
	}
	
	public void setManagersByTreeType(UUID managersByTreeType) {
		data.set(PARAMETER_MANAGERS_BY_TREE_TYPE, managersByTreeType);
	}
	
	public UUID getManagersByTreeType() {
		return (UUID) data.getFirst(PARAMETER_MANAGERS_BY_TREE_TYPE);
	}
	
	public UUID getManagersByContract() {
		return (UUID) data.getFirst(PARAMETER_MANAGERS_BY_CONTRACT);
	}
	
	public void setManagersByContract(UUID managersByContract) {
		data.set(PARAMETER_MANAGERS_BY_CONTRACT, managersByContract);
	}
	
	public void setRoles(List<UUID> roles) {
		this.roles = roles;
	}
	
	public List<UUID> getRoles() {
		if (roles == null) {
			roles = new ArrayList<>();
		}
		return roles;
	}

	@Override
	public String getProperty() {
		return property;
	}

	@Override
	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	public UUID getTreeNode() {
		return treeNode;
	}
	
	public void setTreeNode(UUID treeNode) {
		this.treeNode = treeNode;
	}
	
	public UUID getTreeType() {
		return treeType;
	}
	
	public void setTreeType(UUID treeType) {
		this.treeType = treeType;
	}
	
	public boolean isRecursively() {
		return recursively;
	}
	
	public void setRecursively(boolean recursively) {
		this.recursively = recursively;
	}
	
	public boolean isIncludeGuarantees() {
		return includeGuarantees;
	}
	
	public void setIncludeGuarantees(boolean includeGuarantees) {
		this.includeGuarantees = includeGuarantees;
	}

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}