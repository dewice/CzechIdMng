package eu.bcvsolutions.idm.core.bulk.action.impl.role;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.bulk.action.AbstractExportBulkAction;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIncompatibleRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCompositionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleFormAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAuthorizationPolicyFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIncompatibleRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleCatalogueRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleCompositionFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFormAttributeFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeRoleFilter;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmIncompatibleRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCompositionService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleFormAttributeService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmAuthorizationPolicy_;
import eu.bcvsolutions.idm.core.model.entity.IdmIncompatibleRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleCatalogueRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleComposition_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleFormAttribute_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleGuaranteeRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleGuarantee_;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Bulk operation to export the role
 * 
 * @author Vít Švanda
 *
 */
@Component("roleExportBulkAction")
@Description("Bulk operation to export the role.")
public class RoleExportBulkAction extends AbstractExportBulkAction<IdmRoleDto, IdmRoleFilter> {

	public static final String NAME = "role-export-bulk-action";

	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private IdmRoleCompositionService roleCompositionService;
	@Autowired
	private IdmIncompatibleRoleService incompatibleRoleService;
	@Autowired
	private IdmRoleGuaranteeService roleGuaranteeService;
	@Autowired
	private IdmRoleGuaranteeRoleService roleGuaranteeRoleService;
	@Autowired
	private IdmAuthorizationPolicyService authorizationPolicyService;
	@Autowired
	private IdmRoleCatalogueRoleService roleCatalogueRoleService;
	@Autowired
	private IdmRoleFormAttributeService roleFormAttributeService;

	@Override
	protected void exportDto(IdmRoleDto dto) {
		IdmRoleDto role = roleService.get(dto.getId(), IdmBasePermission.READ);
		// Create new batch (if doesn't exist)
		initBatch("Export of roles");

		// Export role
		roleService.export(role.getId(), this.getBatch());
		// Export EAVs
		this.exportEavs(role);
		// Export business-roles
		this.exportBusinessRoles(role);
		// Export incompatible-roles
		this.exportIncompatibleRoles(role);
		// Export role guarantess
		this.exportRoleGuarantees(role);
		// Export identity guarantees
		this.exportIdentityGuarantees(role);
		// Export permissions
		this.exportPermissions(role);
		// Export role catalogs
		this.exportRoleCatalogs(role);
		// Export role attributes
		this.exportRoleAttributes(role);
	}

	/**
	 * Export EAVs for given role.
	 * 
	 * @param role
	 */
	private void exportEavs(IdmRoleDto role) {
		this.getFormService()//
				.getDefinitions(IdmRoleDto.class)//
				.forEach(definition -> {
					// Export EAV definition
					this.getFormDefinitionService().export(definition.getId(), this.getBatch());
					// Export EAV form instance
					IdmFormInstanceDto formInstance = this.getFormService().getFormInstance(role, definition);
					if (formInstance != null) {
						formInstance.setId(formInstance.getFormDefinition().getId());
						this.getFormService().export(formInstance, this.getBatch());
					}
				});
	}

	/**
	 * Export incompatible roles for given role.
	 * 
	 * @param role
	 */
	private void exportIncompatibleRoles(IdmRoleDto role) {
		IdmIncompatibleRoleFilter incompatibleFilter = new IdmIncompatibleRoleFilter();
		incompatibleFilter.setRoleId(role.getId());
		List<IdmIncompatibleRoleDto> incompatibles = incompatibleRoleService.find(incompatibleFilter, null)
				.getContent();
		if (incompatibles.isEmpty()) {
			incompatibleRoleService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		incompatibles.forEach(incompatible -> {
			incompatibleRoleService.export(incompatible.getId(), this.getBatch());
		});

		// Set parent fields -> set authoritative mode. Here are two parent fields!
		Set<String> parents = new LinkedHashSet<String>();
		parents.add(IdmIncompatibleRole_.superior.getName());
		parents.add(IdmIncompatibleRole_.sub.getName());

		this.getExportManager().setAuthoritativeMode(parents, IdmIncompatibleRoleFilter.PARAMETER_ROLE_ID,
				IdmIncompatibleRoleDto.class, this.getBatch());
	}

	/**
	 * Export business roles for given role.
	 * 
	 * @param role
	 */
	private void exportBusinessRoles(IdmRoleDto role) {
		IdmRoleCompositionFilter compositionFilter = new IdmRoleCompositionFilter();
		compositionFilter.setRoleId(role.getId());
		List<IdmRoleCompositionDto> compositions = roleCompositionService.find(compositionFilter, null).getContent();
		if (compositions.isEmpty()) {
			roleCompositionService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		compositions.forEach(composition -> {
			roleCompositionService.export(composition.getId(), this.getBatch());
		});
		// Set parent fields -> set authoritative mode. Here are two parent fields!
		Set<String> parents = new LinkedHashSet<String>();
		parents.add(IdmRoleComposition_.superior.getName());
		parents.add(IdmRoleComposition_.sub.getName());

		this.getExportManager().setAuthoritativeMode(parents, IdmRoleCompositionFilter.PARAMETER_ROLE_ID,
				IdmRoleCompositionDto.class, this.getBatch());
	}

	/**
	 * Export identity gurantees for given role.
	 * 
	 * @param role
	 */
	private void exportIdentityGuarantees(IdmRoleDto role) {
		IdmRoleGuaranteeFilter filter = new IdmRoleGuaranteeFilter();
		filter.setRole(role.getId());
		List<IdmRoleGuaranteeDto> dtos = roleGuaranteeService.find(filter, null).getContent();
		if (dtos.isEmpty()) {
			roleGuaranteeService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		dtos.forEach(dto -> {
			roleGuaranteeService.export(dto.getId(), this.getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(IdmRoleGuarantee_.role.getName(),
				IdmRoleGuaranteeFilter.PARAMETER_ROLE, IdmRoleGuaranteeDto.class, this.getBatch());
	}

	/**
	 * Export role gurantees for given role.
	 * 
	 * @param role
	 */
	private void exportRoleGuarantees(IdmRoleDto role) {
		IdmRoleGuaranteeRoleFilter filter = new IdmRoleGuaranteeRoleFilter();
		filter.setRole(role.getId());
		List<IdmRoleGuaranteeRoleDto> dtos = roleGuaranteeRoleService.find(filter, null).getContent();
		if (dtos.isEmpty()) {
			roleGuaranteeRoleService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		dtos.forEach(dto -> {
			roleGuaranteeRoleService.export(dto.getId(), this.getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(IdmRoleGuaranteeRole_.role.getName(),
				IdmRoleGuaranteeFilter.PARAMETER_ROLE, IdmRoleGuaranteeRoleDto.class, this.getBatch());
	}

	/**
	 * Export permissions for given role.
	 * 
	 * @param role
	 */
	private void exportPermissions(IdmRoleDto role) {
		IdmAuthorizationPolicyFilter filter = new IdmAuthorizationPolicyFilter();
		filter.setRoleId(role.getId());

		List<IdmAuthorizationPolicyDto> dtos = authorizationPolicyService.find(filter, null).getContent();
		if (dtos.isEmpty()) {
			authorizationPolicyService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		dtos.forEach(dto -> {
			authorizationPolicyService.export(dto.getId(), this.getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(IdmAuthorizationPolicy_.role.getName(), "roleId",
				IdmAuthorizationPolicyDto.class, this.getBatch());
	}

	/**
	 * Export role-catalogs for given role.
	 * 
	 * @param role
	 */
	private void exportRoleCatalogs(IdmRoleDto role) {
		IdmRoleCatalogueRoleFilter filter = new IdmRoleCatalogueRoleFilter();
		filter.setRoleId(role.getId());

		List<IdmRoleCatalogueRoleDto> dtos = roleCatalogueRoleService.find(filter, null).getContent();
		if (dtos.isEmpty()) {
			roleCatalogueRoleService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		dtos.forEach(dto -> {
			roleCatalogueRoleService.export(dto.getId(), this.getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(IdmRoleCatalogueRole_.role.getName(), "roleId",
				IdmRoleCatalogueRoleDto.class, this.getBatch());
		List<ExportDescriptorDto> exportOrder = getBatch().getExportOrder();
		
		// Order can be wrong now! Catalog descriptor must be first (then role-catalogue-role)!
		ExportDescriptorDto roleCatalogDescriptor = this.getExportManager().getDescriptor(getBatch(), IdmRoleCatalogueRoleDto.class);
		ExportDescriptorDto catalogDescriptor = this.getExportManager().getDescriptor(getBatch(), IdmRoleCatalogueDto.class);

		if (roleCatalogDescriptor != null && catalogDescriptor != null) {
			int roleCatalogueIndex = exportOrder.indexOf(roleCatalogDescriptor);
			int catalogueIndex = exportOrder.indexOf(catalogDescriptor);

			if (roleCatalogueIndex < catalogueIndex) {
				exportOrder.set(catalogueIndex, roleCatalogDescriptor);
				exportOrder.set(roleCatalogueIndex, catalogDescriptor);
			}
		}
	}

	/**
	 * Export role-attributes for given role.
	 * 
	 * @param role
	 */
	private void exportRoleAttributes(IdmRoleDto role) {
		IdmRoleFormAttributeFilter filter = new IdmRoleFormAttributeFilter();
		filter.setRole(role.getId());

		List<IdmRoleFormAttributeDto> dtos = roleFormAttributeService.find(filter, null).getContent();
		if (dtos.isEmpty()) {
			roleFormAttributeService.export(ExportManager.BLANK_UUID, this.getBatch());
		}
		dtos.forEach(dto -> {
			roleFormAttributeService.export(dto.getId(), this.getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(IdmRoleFormAttribute_.role.getName(), "role",
				IdmRoleFormAttributeDto.class, this.getBatch());
	}

	@Override
	public List<String> getAuthorities() {
		List<String> authorities = super.getAuthorities();
		authorities.add(CoreGroupPermission.ROLE_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_CREATE);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_UPDATE);
		return authorities;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.ROLE_READ);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 80;
	}

	@Override
	public ReadWriteDtoService<IdmRoleDto, IdmRoleFilter> getService() {
		return roleService;
	}

}
