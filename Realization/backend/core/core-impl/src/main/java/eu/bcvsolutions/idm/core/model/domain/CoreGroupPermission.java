package eu.bcvsolutions.idm.core.model.domain;

import java.util.Arrays;
import java.util.List;

import eu.bcvsolutions.idm.core.CoreModuleDescriptor;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.ContractBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.GroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdentityBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.RoleBasePermission;

/**
 * Aggregate base permission. Name can't contain character '_' - its used for joining to authority name.
 * 
 * @author Radek Tomiška
 *
 */
public enum CoreGroupPermission implements GroupPermission {
	
	AUTHORIZATIONPOLICY(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	IDENTITY(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE,
			IdentityBasePermission.PASSWORDCHANGE,
			IdentityBasePermission.CHANGEPERMISSION,
			IdentityBasePermission.MANUALLYDISABLE,
			IdentityBasePermission.MANUALLYENABLE,
			IdentityBasePermission.CHANGEPROJECTION,
			IdentityBasePermission.CHANGEUSERNAME,
			IdentityBasePermission.CHANGENAME,
			IdentityBasePermission.CHANGEPHONE,
			IdentityBasePermission.CHANGEEMAIL,
			IdentityBasePermission.CHANGEEXTERNALCODE,
			IdentityBasePermission.CHANGEDESCRIPTION),
	PROFILE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE),
	IDENTITYCONTRACT(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ,
			IdmBasePermission.CREATE,
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE,
			ContractBasePermission.CHANGEPERMISSION),
	CONTRACTSLICE(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ,
			IdmBasePermission.CREATE,
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE),
	IDENTITYROLE(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE,
			RoleBasePermission.CANBEREQUESTED),
	ROLE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE,
			RoleBasePermission.CANBEREQUESTED),
	ROLECATALOGUE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	ROLECATALOGUEROLE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	ROLECOMPOSITION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	INCOMPATIBLEROLE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	ROLETREENODE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE,
			IdmBasePermission.AUTOCOMPLETE),
	ROLEGUARANTEE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	ROLEGUARANTEEROLE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	TREENODE(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	TREETYPE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ,
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CONFIGURATION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ,
			IdmBasePermission.CREATE,
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	PASSWORDPOLICY(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	PASSWORD(
			IdmBasePermission.READ, 
			IdmBasePermission.UPDATE),
	SCRIPT(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.DELETE),
	AUDIT(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ),
	MODULE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE),
	SCHEDULER( // ~ LONGRUNNINGTASK
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.EXECUTE, 
			IdmBasePermission.READ,
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE),
	ROLEREQUEST(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE, 
			IdmBasePermission.EXECUTE),
	AUTOMATICROLEREQUEST(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE, 
			IdmBasePermission.EXECUTE),
	FORMDEFINITION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	FORMPROJECTION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	FORMATTRIBUTE(
			IdmBasePermission.ADMIN,
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	FORMVALUE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CODELIST(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CODELISTITEM(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CONTRACTGUARANTEE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CONTRACTPOSITION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	CONTRACTSLICEGUARANTEE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	WORKFLOWDEFINITION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE,
			IdmBasePermission.DELETE),
	CONFIDENTIALSTORAGEVALUE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ),
	AUTOMATICROLEATTRIBUTE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE,
			IdmBasePermission.AUTOCOMPLETE),
	AUTOMATICROLEATTRIBUTERULE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	AUTOMATICROLEATTRIBUTERULEREQUEST(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	WORKFLOWTASK(
			IdmBasePermission.ADMIN,
			IdmBasePermission.READ,
			IdmBasePermission.EXECUTE),
	TOKEN(
			IdmBasePermission.ADMIN,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	REQUEST(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE, 
			IdmBasePermission.EXECUTE),
	REQUESTITEM(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE, 
			IdmBasePermission.EXECUTE),
	GENERATEVALUE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT,
			IdmBasePermission.AUTOCOMPLETE, 
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	ROLEFORMATTRIBUTE(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	EXPORTIMPORT(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE),
	DELEGATIONDEFINITION(
			IdmBasePermission.ADMIN, 
			IdmBasePermission.COUNT, 
			IdmBasePermission.AUTOCOMPLETE,
			IdmBasePermission.READ, 
			IdmBasePermission.CREATE, 
			IdmBasePermission.UPDATE, 
			IdmBasePermission.DELETE);
	
	// String constants could be used in pre / post authorize SpEl expressions
	
	public static final String AUTHORIZATIONPOLICY_COUNT = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "COUNT";
	public static final String AUTHORIZATIONPOLICY_AUTOCOMPLETE = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String AUTHORIZATIONPOLICY_READ = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "READ";
	public static final String AUTHORIZATIONPOLICY_CREATE = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "CREATE";
	public static final String AUTHORIZATIONPOLICY_UPDATE = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "UPDATE";
	public static final String AUTHORIZATIONPOLICY_DELETE = "AUTHORIZATIONPOLICY" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String IDENTITY_ADMIN = "IDENTITY" + BasePermission.SEPARATOR + "ADMIN";
	public static final String IDENTITY_COUNT = "IDENTITY" + BasePermission.SEPARATOR + "COUNT";
	public static final String IDENTITY_AUTOCOMPLETE = "IDENTITY" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String IDENTITY_READ = "IDENTITY" + BasePermission.SEPARATOR + "READ";
	public static final String IDENTITY_CREATE = "IDENTITY" + BasePermission.SEPARATOR + "CREATE";
	public static final String IDENTITY_UPDATE = "IDENTITY" + BasePermission.SEPARATOR + "UPDATE";
	public static final String IDENTITY_DELETE = "IDENTITY" + BasePermission.SEPARATOR + "DELETE";
	public static final String IDENTITY_PASSWORDCHANGE = "IDENTITY" + BasePermission.SEPARATOR + "PASSWORDCHANGE";
	public static final String IDENTITY_CHANGEPERMISSION = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEPERMISSION";
	public static final String IDENTITY_MANUALLYDISABLE = "IDENTITY" + BasePermission.SEPARATOR + "MANUALLYDISABLE";
	public static final String IDENTITY_MANUALLYENABLE = "IDENTITY" + BasePermission.SEPARATOR + "MANUALLYENABLE";
	public static final String IDENTITY_CHANGEPROJECTION = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEPROJECTION";
	public static final String IDENTITY_CHANGEUSERNAME = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEUSERNAME";
	public static final String IDENTITY_CHANGENAME = "IDENTITY" + BasePermission.SEPARATOR + "CHANGENAME";
	public static final String IDENTITY_CHANGEPHONE = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEPHONE";
	public static final String IDENTITY_CHANGEEMAIL = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEEMAIL";
	public static final String IDENTITY_CHANGEEXTERNALCODE = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEEXTERNALCODE";
	public static final String IDENTITY_CHANGEDESCRIPTION = "IDENTITY" + BasePermission.SEPARATOR + "CHANGEDESCRIPTION";
	//
	public static final String PROFILE_ADMIN = "PROFILE" + BasePermission.SEPARATOR + "ADMIN";
	public static final String PROFILE_COUNT = "PROFILE" + BasePermission.SEPARATOR + "COUNT";
	public static final String PROFILE_AUTOCOMPLETE = "PROFILE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String PROFILE_READ = "PROFILE" + BasePermission.SEPARATOR + "READ";
	public static final String PROFILE_CREATE = "PROFILE" + BasePermission.SEPARATOR + "CREATE";
	public static final String PROFILE_UPDATE = "PROFILE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String PROFILE_DELETE = "PROFILE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String IDENTITYROLE_ADMIN = "IDENTITYROLE" + BasePermission.SEPARATOR + "ADMIN";
	public static final String IDENTITYROLE_COUNT = "IDENTITYROLE" + BasePermission.SEPARATOR + "COUNT";
	public static final String IDENTITYROLE_AUTOCOMPLETE = "IDENTITYROLE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String IDENTITYROLE_READ = "IDENTITYROLE" + BasePermission.SEPARATOR + "READ";
	public static final String IDENTITYROLE_CREATE = "IDENTITYROLE" + BasePermission.SEPARATOR + "CREATE";
	public static final String IDENTITYROLE_UPDATE = "IDENTITYROLE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String IDENTITYROLE_DELETE = "IDENTITYROLE" + BasePermission.SEPARATOR + "DELETE";
	public static final String IDENTITYROLE_CANBEREQUESTED = "IDENTITYROLE" + BasePermission.SEPARATOR + "CANBEREQUESTED";
	//
	public static final String IDENTITYCONTRACT_COUNT = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "COUNT";
	public static final String IDENTITYCONTRACT_AUTOCOMPLETE = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String IDENTITYCONTRACT_READ = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "READ";
	public static final String IDENTITYCONTRACT_CREATE = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "CREATE";
	public static final String IDENTITYCONTRACT_UPDATE = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "UPDATE";
	public static final String IDENTITYCONTRACT_DELETE = "IDENTITYCONTRACT" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONTRACTSLICE_COUNT = "CONTRACTSLICE" + BasePermission.SEPARATOR + "COUNT";
	public static final String CONTRACTSLICE_AUTOCOMPLETE = "CONTRACTSLICE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CONTRACTSLICE_READ = "CONTRACTSLICE" + BasePermission.SEPARATOR + "READ";
	public static final String CONTRACTSLICE_CREATE = "CONTRACTSLICE" + BasePermission.SEPARATOR + "CREATE";
	public static final String CONTRACTSLICE_UPDATE = "CONTRACTSLICE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CONTRACTSLICE_DELETE = "CONTRACTSLICE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONTRACTGUARANTEE_COUNT = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "COUNT";
	public static final String CONTRACTGUARANTEE_AUTOCOMPLETE = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CONTRACTGUARANTEE_READ = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "READ";
	public static final String CONTRACTGUARANTEE_CREATE = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "CREATE";
	public static final String CONTRACTGUARANTEE_UPDATE = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CONTRACTGUARANTEE_DELETE = "CONTRACTGUARANTEE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONTRACTPOSITION_COUNT = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "COUNT";
	public static final String CONTRACTPOSITION_AUTOCOMPLETE = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CONTRACTPOSITION_READ = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "READ";
	public static final String CONTRACTPOSITION_CREATE = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "CREATE";
	public static final String CONTRACTPOSITION_UPDATE = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CONTRACTPOSITION_DELETE = "CONTRACTPOSITION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONTRACTSLICEGUARANTEE_AUTOCOMPLETE = "CONTRACTSLICEGUARANTEE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CONTRACTSLICEGUARANTEE_READ = "CONTRACTSLICEGUARANTEE" + BasePermission.SEPARATOR + "READ";
	public static final String CONTRACTSLICEGUARANTEE_CREATE = "CONTRACTSLICEGUARANTEE" + BasePermission.SEPARATOR + "CREATE";
	public static final String CONTRACTSLICEGUARANTEE_UPDATE = "CONTRACTSLICEGUARANTEE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CONTRACTSLICEGUARANTEE_DELETE = "CONTRACTSLICEGUARANTEE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONFIGURATION_ADMIN = "CONFIGURATION" + BasePermission.SEPARATOR + "ADMIN";
	public static final String CONFIGURATION_COUNT = "CONFIGURATION" + BasePermission.SEPARATOR + "COUNT";
	public static final String CONFIGURATION_AUTOCOMPLETE = "CONFIGURATION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CONFIGURATION_READ = "CONFIGURATION" + BasePermission.SEPARATOR + "READ";
	public static final String CONFIGURATION_CREATE = "CONFIGURATION" + BasePermission.SEPARATOR + "CREATE";
	public static final String CONFIGURATION_UPDATE = "CONFIGURATION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CONFIGURATION_DELETE = "CONFIGURATION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String TREENODE_COUNT = "TREENODE" + BasePermission.SEPARATOR + "COUNT";
	public static final String TREENODE_AUTOCOMPLETE = "TREENODE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String TREENODE_READ = "TREENODE" + BasePermission.SEPARATOR + "READ";
	public static final String TREENODE_CREATE = "TREENODE" + BasePermission.SEPARATOR + "CREATE";
	public static final String TREENODE_UPDATE = "TREENODE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String TREENODE_DELETE = "TREENODE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String TREETYPE_COUNT = "TREETYPE" + BasePermission.SEPARATOR + "COUNT";
	public static final String TREETYPE_AUTOCOMPLETE = "TREETYPE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String TREETYPE_READ = "TREETYPE" + BasePermission.SEPARATOR + "READ";
	public static final String TREETYPE_CREATE = "TREETYPE" + BasePermission.SEPARATOR + "CREATE";
	public static final String TREETYPE_UPDATE = "TREETYPE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String TREETYPE_DELETE = "TREETYPE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String ROLE_COUNT = "ROLE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLE_AUTOCOMPLETE = "ROLE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLE_READ = "ROLE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLE_CREATE = "ROLE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLE_UPDATE = "ROLE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLE_DELETE = "ROLE" + BasePermission.SEPARATOR + "DELETE";
	public static final String ROLE_CANBEREQUESTED = "ROLE" + BasePermission.SEPARATOR + "CANBEREQUESTED";
	//
	public static final String ROLECATALOGUE_COUNT = "ROLECATALOGUE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLECATALOGUE_AUTOCOMPLETE = "ROLECATALOGUE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLECATALOGUE_READ = "ROLECATALOGUE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLECATALOGUE_CREATE = "ROLECATALOGUE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLECATALOGUE_UPDATE = "ROLECATALOGUE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLECATALOGUE_DELETE = "ROLECATALOGUE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String ROLECATALOGUEROLE_COUNT = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLECATALOGUEROLE_AUTOCOMPLETE = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLECATALOGUEROLE_READ = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLECATALOGUEROLE_CREATE = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLECATALOGUEROLE_UPDATE = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLECATALOGUEROLE_DELETE = "ROLECATALOGUEROLE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String ROLECOMPOSITION_COUNT = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLECOMPOSITION_AUTOCOMPLETE = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLECOMPOSITION_READ = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "READ";
	public static final String ROLECOMPOSITION_CREATE = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLECOMPOSITION_UPDATE = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLECOMPOSITION_DELETE = "ROLECOMPOSITION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String INCOMPATIBLEROLE_COUNT = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "COUNT";
	public static final String INCOMPATIBLEROLE_AUTOCOMPLETE = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String INCOMPATIBLEROLE_READ = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "READ";
	public static final String INCOMPATIBLEROLE_CREATE = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "CREATE";
	public static final String INCOMPATIBLEROLE_UPDATE = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String INCOMPATIBLEROLE_DELETE = "INCOMPATIBLEROLE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String ROLETREENODE_READ = "ROLETREENODE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLETREENODE_CREATE = "ROLETREENODE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLETREENODE_UPDATE = "ROLETREENODE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLETREENODE_DELETE = "ROLETREENODE" + BasePermission.SEPARATOR + "DELETE";
	public static final String ROLETREENODE_AUTOCOMPLETE = "ROLETREENODE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String ROLEGUARANTEE_COUNT = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLEGUARANTEE_AUTOCOMPLETE = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLEGUARANTEE_READ = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLEGUARANTEE_CREATE = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLEGUARANTEE_UPDATE = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLEGUARANTEE_DELETE = "ROLEGUARANTEE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String ROLEGUARANTEEROLE_COUNT = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLEGUARANTEEROLE_AUTOCOMPLETE = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLEGUARANTEEROLE_READ = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLEGUARANTEEROLE_CREATE = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLEGUARANTEEROLE_UPDATE = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLEGUARANTEEROLE_DELETE = "ROLEGUARANTEEROLE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String PASSWORDPOLICY_COUNT = "PASSWORDPOLICY" + BasePermission.SEPARATOR + "COUNT";
	public static final String PASSWORDPOLICY_CREATE = "PASSWORDPOLICY" + BasePermission.SEPARATOR + "CREATE";
	public static final String PASSWORDPOLICY_UPDATE = "PASSWORDPOLICY" + BasePermission.SEPARATOR + "UPDATE";
	public static final String PASSWORDPOLICY_DELETE = "PASSWORDPOLICY" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String PASSWORD_READ = "PASSWORD" + BasePermission.SEPARATOR + "READ";
	public static final String PASSWORD_UPDATE = "PASSWORD" + BasePermission.SEPARATOR + "UPDATE";
	//
	public static final String SCRIPT_AUTOCOMPLETE = "SCRIPT" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String SCRIPT_READ = "SCRIPT" + BasePermission.SEPARATOR + "READ";
	public static final String SCRIPT_CREATE = "SCRIPT" + BasePermission.SEPARATOR + "CREATE";
	public static final String SCRIPT_UPDATE = "SCRIPT" + BasePermission.SEPARATOR + "UPDATE";
	public static final String SCRIPT_DELETE = "SCRIPT" + BasePermission.SEPARATOR + "DELETE";
	public static final String SCRIPT_COUNT = "SCRIPT" + BasePermission.SEPARATOR + "COUNT";
	//
	public static final String AUDIT_READ = "AUDIT" + BasePermission.SEPARATOR + "READ";
	//
	public static final String MODULE_READ = "MODULE" + BasePermission.SEPARATOR + "READ";
	public static final String MODULE_CREATE = "MODULE" + BasePermission.SEPARATOR + "CREATE";
	public static final String MODULE_UPDATE = "MODULE" + BasePermission.SEPARATOR + "UPDATE";
	//
	public static final String SCHEDULER_COUNT = "SCHEDULER" + BasePermission.SEPARATOR + "COUNT";
	public static final String SCHEDULER_AUTOCOMPLETE = "SCHEDULER" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String SCHEDULER_READ = "SCHEDULER" + BasePermission.SEPARATOR + "READ";
	public static final String SCHEDULER_CREATE = "SCHEDULER" + BasePermission.SEPARATOR + "CREATE";
	public static final String SCHEDULER_UPDATE = "SCHEDULER" + BasePermission.SEPARATOR + "UPDATE";
	public static final String SCHEDULER_DELETE = "SCHEDULER" + BasePermission.SEPARATOR + "DELETE";
	public static final String SCHEDULER_EXECUTE = "SCHEDULER" + BasePermission.SEPARATOR + "EXECUTE";
	//
	public static final String ROLE_REQUEST_ADMIN = "ROLEREQUEST" + BasePermission.SEPARATOR + "ADMIN";
	public static final String ROLE_REQUEST_COUNT = "ROLEREQUEST" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLE_REQUEST_READ = "ROLEREQUEST" + BasePermission.SEPARATOR + "READ";
	public static final String ROLE_REQUEST_CREATE = "ROLEREQUEST" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLE_REQUEST_UPDATE = "ROLEREQUEST" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLE_REQUEST_DELETE = "ROLEREQUEST" + BasePermission.SEPARATOR + "DELETE";
	public static final String ROLE_REQUEST_EXECUTE = "ROLEREQUEST" + BasePermission.SEPARATOR + "EXECUTE";
	//
	public static final String AUTOMATIC_ROLE_REQUEST_ADMIN = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "ADMIN";
	public static final String AUTOMATIC_ROLE_REQUEST_READ = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "READ";
	public static final String AUTOMATIC_ROLE_REQUEST_CREATE = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "CREATE";
	public static final String AUTOMATIC_ROLE_REQUEST_UPDATE = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "UPDATE";
	public static final String AUTOMATIC_ROLE_REQUEST_DELETE = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "DELETE";
	public static final String AUTOMATIC_ROLE_REQUEST_AUTOCOMPLETE = "AUTOMATICROLEREQUEST" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String FORM_DEFINITION_COUNT = "FORMDEFINITION" + BasePermission.SEPARATOR + "COUNT";
	public static final String FORM_DEFINITION_AUTOCOMPLETE = "FORMDEFINITION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String FORM_DEFINITION_READ = "FORMDEFINITION" + BasePermission.SEPARATOR + "READ";
	public static final String FORM_DEFINITION_CREATE = "FORMDEFINITION" + BasePermission.SEPARATOR + "CREATE";
	public static final String FORM_DEFINITION_UPDATE = "FORMDEFINITION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String FORM_DEFINITION_DELETE = "FORMDEFINITION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String FORM_PROJECTION_COUNT = "FORMPROJECTION" + BasePermission.SEPARATOR + "COUNT";
	public static final String FORM_PROJECTION_AUTOCOMPLETE = "FORMPROJECTION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String FORM_PROJECTION_READ = "FORMPROJECTION" + BasePermission.SEPARATOR + "READ";
	public static final String FORM_PROJECTION_CREATE = "FORMPROJECTION" + BasePermission.SEPARATOR + "CREATE";
	public static final String FORM_PROJECTION_UPDATE = "FORMPROJECTION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String FORM_PROJECTION_DELETE = "FORMPROJECTION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String FORM_ATTRIBUTE_COUNT = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "COUNT";
	public static final String FORM_ATTRIBUTE_AUTOCOMPLETE = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String FORM_ATTRIBUTE_READ = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "READ";
	public static final String FORM_ATTRIBUTE_CREATE = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "CREATE";
	public static final String FORM_ATTRIBUTE_UPDATE = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String FORM_ATTRIBUTE_DELETE = "FORMATTRIBUTE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String FORM_VALUE_READ = "FORMVALUE" + BasePermission.SEPARATOR + "READ";
	public static final String FORM_VALUE_UPDATE = "FORMVALUE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String FORM_VALUE_DELETE = "FORMVALUE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CODE_LIST_ITEM_COUNT = "CODELISTITEM" + BasePermission.SEPARATOR + "COUNT";
	public static final String CODE_LIST_ITEM_AUTOCOMPLETE = "CODELISTITEM" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CODE_LIST_ITEM_READ = "CODELISTITEM" + BasePermission.SEPARATOR + "READ";
	public static final String CODE_LIST_ITEM_CREATE = "CODELISTITEM" + BasePermission.SEPARATOR + "CREATE";
	public static final String CODE_LIST_ITEM_UPDATE = "CODELISTITEM" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CODE_LIST_ITEM_DELETE = "CODELISTITEM" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CODE_LIST_COUNT = "CODELIST" + BasePermission.SEPARATOR + "COUNT";
	public static final String CODE_LIST_AUTOCOMPLETE = "CODELIST" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String CODE_LIST_READ = "CODELIST" + BasePermission.SEPARATOR + "READ";
	public static final String CODE_LIST_CREATE = "CODELIST" + BasePermission.SEPARATOR + "CREATE";
	public static final String CODE_LIST_UPDATE = "CODELIST" + BasePermission.SEPARATOR + "UPDATE";
	public static final String CODE_LIST_DELETE = "CODELIST" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String WORKFLOW_DEFINITION_READ = "WORKFLOWDEFINITION" + BasePermission.SEPARATOR + "READ";
	public static final String WORKFLOW_DEFINITION_CREATE = "WORKFLOWDEFINITION" + BasePermission.SEPARATOR + "CREATE";
	public static final String WORKFLOW_DEFINITION_UPDATE = "WORKFLOWDEFINITION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String WORKFLOW_DEFINITION_DELETE = "WORKFLOWDEFINITION" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String CONFIDENTIAL_STORAGE_VALUE_READ = "CONFIDENTIALSTORAGEVALUE" + BasePermission.SEPARATOR + "READ";
	//
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_READ = "AUTOMATICROLEATTRIBUTE" + BasePermission.SEPARATOR + "READ";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_CREATE = "AUTOMATICROLEATTRIBUTE" + BasePermission.SEPARATOR + "CREATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_UPDATE = "AUTOMATICROLEATTRIBUTE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_DELETE = "AUTOMATICROLEATTRIBUTE" + BasePermission.SEPARATOR + "DELETE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_AUTOCOMPLETE = "AUTOMATICROLEATTRIBUTE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_READ = "AUTOMATICROLEATTRIBUTERULE" + BasePermission.SEPARATOR + "READ";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_CREATE = "AUTOMATICROLEATTRIBUTERULE" + BasePermission.SEPARATOR + "CREATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_UPDATE = "AUTOMATICROLEATTRIBUTERULE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_DELETE = "AUTOMATICROLEATTRIBUTERULE" + BasePermission.SEPARATOR + "DELETE";
	// 
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_REQUEST_READ = "AUTOMATICROLEATTRIBUTERULEREQUEST" + BasePermission.SEPARATOR + "READ";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_REQUEST_CREATE = "AUTOMATICROLEATTRIBUTERULEREQUEST" + BasePermission.SEPARATOR + "CREATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_REQUEST_UPDATE = "AUTOMATICROLEATTRIBUTERULEREQUEST" + BasePermission.SEPARATOR + "UPDATE";
	public static final String AUTOMATIC_ROLE_ATTRIBUTE_RULE_REQUEST_DELETE = "AUTOMATICROLEATTRIBUTERULEREQUEST" + BasePermission.SEPARATOR + "DELETE";
	// 
	public static final String WORKFLOW_TASK_ADMIN = "WORKFLOWTASK" + BasePermission.SEPARATOR + "ADMIN";
	public static final String WORKFLOW_TASK_READ = "WORKFLOWTASK" + BasePermission.SEPARATOR + "READ";
	public static final String WORKFLOW_TASK_EXECUTE = "WORKFLOWTASK" + BasePermission.SEPARATOR + "EXECUTE";
	//
	public static final String REQUEST_ADMIN = "REQUEST" + BasePermission.SEPARATOR + "ADMIN";
	public static final String REQUEST_READ = "REQUEST" + BasePermission.SEPARATOR + "READ";
	public static final String REQUEST_CREATE = "REQUEST" + BasePermission.SEPARATOR + "CREATE";
	public static final String REQUEST_UPDATE = "REQUEST" + BasePermission.SEPARATOR + "UPDATE";
	public static final String REQUEST_DELETE = "REQUEST" + BasePermission.SEPARATOR + "DELETE";
	public static final String REQUEST_AUTOCOMPLETE = "REQUEST" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String REQUEST_ITEM_ADMIN = "REQUESTITEM" + BasePermission.SEPARATOR + "ADMIN";
	public static final String REQUEST_ITEM_READ = "REQUESTITEM" + BasePermission.SEPARATOR + "READ";
	public static final String REQUEST_ITEM_CREATE = "REQUESTITEM" + BasePermission.SEPARATOR + "CREATE";
	public static final String REQUEST_ITEM_UPDATE = "REQUESTITEM" + BasePermission.SEPARATOR + "UPDATE";
	public static final String REQUEST_ITEM_DELETE = "REQUESTITEM" + BasePermission.SEPARATOR + "DELETE";
	public static final String REQUEST_ITEM_AUTOCOMPLETE = "REQUESTITEM" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String GENERATE_VALUE_ADMIN = "GENERATEVALUE" + BasePermission.SEPARATOR + "ADMIN";
	public static final String GENERATE_VALUE_READ = "GENERATEVALUE" + BasePermission.SEPARATOR + "READ";
	public static final String GENERATE_VALUE_COUNT = "GENERATEVALUE" + BasePermission.SEPARATOR + "COUNT";
	public static final String GENERATE_VALUE_CREATE = "GENERATEVALUE" + BasePermission.SEPARATOR + "CREATE";
	public static final String GENERATE_VALUE_UPDATE = "GENERATEVALUE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String GENERATE_VALUE_DELETE = "GENERATEVALUE" + BasePermission.SEPARATOR + "DELETE";
	public static final String GENERATE_VALUE_AUTOCOMPLETE = "GENERATEVALUE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	//
	public static final String ROLEFORMATTRIBUTE_COUNT = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "COUNT";
	public static final String ROLEFORMATTRIBUTE_AUTOCOMPLETE = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String ROLEFORMATTRIBUTE_READ = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "READ";
	public static final String ROLEFORMATTRIBUTE_CREATE = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "CREATE";
	public static final String ROLEFORMATTRIBUTE_UPDATE = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "UPDATE";
	public static final String ROLEFORMATTRIBUTE_DELETE = "ROLEFORMATTRIBUTE" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String EXPORTIMPORT_ADMIN = "EXPORTIMPORT" + BasePermission.SEPARATOR + "ADMIN";
	public static final String EXPORTIMPORT_COUNT = "EXPORTIMPORT" + BasePermission.SEPARATOR + "COUNT";
	public static final String EXPORTIMPORT_AUTOCOMPLETE = "EXPORTIMPORT" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String EXPORTIMPORT_READ = "EXPORTIMPORT" + BasePermission.SEPARATOR + "READ";
	public static final String EXPORTIMPORT_CREATE = "EXPORTIMPORT" + BasePermission.SEPARATOR + "CREATE";
	public static final String EXPORTIMPORT_UPDATE = "EXPORTIMPORT" + BasePermission.SEPARATOR + "UPDATE";
	public static final String EXPORTIMPORT_DELETE = "EXPORTIMPORT" + BasePermission.SEPARATOR + "DELETE";
	//
	public static final String DELEGATIONDEFINITION_ADMIN = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "ADMIN";
	public static final String DELEGATIONDEFINITION_COUNT = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "COUNT";
	public static final String DELEGATIONDEFINITION_AUTOCOMPLETE = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "AUTOCOMPLETE";
	public static final String DELEGATIONDEFINITION_READ = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "READ";
	public static final String DELEGATIONDEFINITION_CREATE = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "CREATE";
	public static final String DELEGATIONDEFINITION_UPDATE = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "UPDATE";
	public static final String DELEGATIONDEFINITION_DELETE = "DELEGATIONDEFINITION" + BasePermission.SEPARATOR + "DELETE";
	
	private final List<BasePermission> permissions;

	private CoreGroupPermission(BasePermission... permissions) {
		this.permissions = Arrays.asList(permissions);
	}
	
	@Override
	public List<BasePermission> getPermissions() {
		return permissions;
	}
	
	@Override
	public String getName() {
		return name();
	}	
	
	@Override
	public String getModule() {
		return CoreModuleDescriptor.MODULE_ID;
	}
}
