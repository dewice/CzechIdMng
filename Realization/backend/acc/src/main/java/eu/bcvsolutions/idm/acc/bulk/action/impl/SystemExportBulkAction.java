package eu.bcvsolutions.idm.acc.bulk.action.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.dto.AbstractSysSyncConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBreakConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningBreakConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaObjectClassFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem_;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass_;
import eu.bcvsolutions.idm.acc.entity.SysSyncConfig_;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping_;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBreakConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractExportBulkAction;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Bulk operation to export the system
 * 
 * @author Vít Švanda
 *
 */
@Enabled(AccModuleDescriptor.MODULE_ID)
@Component("systemExportBulkAction")
@Description("Bulk operation to export the system.")
public class SystemExportBulkAction extends AbstractExportBulkAction<SysSystemDto, SysSystemFilter> {

	public static final String NAME = "system-export-bulk-action";

	@Autowired
	private SysSystemService systemService;
	@Autowired
	private SysProvisioningBreakConfigService provisioningBreakService;
	@Autowired
	private IdmFormDefinitionService formDefinitionService;
	@Autowired
	private SysRoleSystemService roleSystemService;
	@Autowired
	private SysSchemaObjectClassService objectClassService;
	@Autowired
	private SysSyncConfigService synchronizationConfigService;
	@Autowired
	private SysSystemMappingService systemMappingService;
	@Autowired
	private SysSchemaObjectClassService schemaObjectClassService;

	@Override
	protected void exportDto(SysSystemDto dto) {
		SysSystemDto systemDto = systemService.get(dto.getId(), IdmBasePermission.READ);
		UUID systemId = systemDto.getId();
		// Create new getBatch() (if doesn't exist)
		initBatch("Export of systems");

		// Export system
		systemService.export(systemDto.getId(), getBatch());
		// Export break
		exportBreakConfig(systemId);
		// Export schema
		exportSchema(systemId);
		// Export mapped attributes
		exportMappedAttributes(systemId);
		// Export synchronizations
		exportSyncConfigs(systemId);
		// Export connector configuration (EAV)
		SysSystemDto system = exportConnectorConfig(systemId);
		// Export pooling configuration (EAV)
		exportPoolingConfig(system);
		// Export role systems
		exportRoleSystems(systemId);
	}

	/**
	 * Export roles-systems
	 * 
	 * @param systemId
	 */
	private void exportRoleSystems(UUID systemId) {
		SysRoleSystemFilter roleSystemFilter = new SysRoleSystemFilter();
		roleSystemFilter.setSystemId(systemId);
		List<SysRoleSystemDto> roleSystems = roleSystemService.find(roleSystemFilter, null).getContent();
		if (roleSystems.isEmpty()) {
			roleSystemService.export(ExportManager.BLANK_UUID, getBatch());
		}
		roleSystems.forEach(roleSystem -> {
			roleSystemService.export(roleSystem.getId(), getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(SysRoleSystem_.system.getName(), "systemId",
				SysRoleSystemDto.class, getBatch());
	}

	/**
	 * Export pooling configuration
	 * 
	 * @param system
	 */
	private void exportPoolingConfig(SysSystemDto system) {
		if (system.getConnectorInstance() != null && system.getConnectorKey() != null) {
			IdmFormDefinitionDto poolingDefinition = systemService
					.getPoolingConnectorFormDefinition(system.getConnectorInstance());
			if (poolingDefinition != null) {
				// Export EAV definition for pooling
				formDefinitionService.export(poolingDefinition.getId(), getBatch());

				IdmFormInstanceDto formInstance = this.getFormService().getFormInstance(system, poolingDefinition);
				if (formInstance != null) {
					formInstance.setId(formInstance.getFormDefinition().getId());
					this.getFormService().export(formInstance, getBatch());
				}
			}
		}
	}

	/**
	 * Export connector configuration
	 * 
	 * @param systemId
	 * @return
	 */
	private SysSystemDto exportConnectorConfig(UUID systemId) {
		SysSystemDto system = systemService.get(systemId);
		if (system.getConnectorInstance() != null && system.getConnectorKey() != null) {
			IdmFormDefinitionDto definition = systemService.getConnectorFormDefinition(system.getConnectorInstance());
			if (definition != null) {
				// Export EAV definition for connector
				formDefinitionService.export(definition.getId(), getBatch());

				IdmFormInstanceDto connectorFormInstance = this.getFormService().getFormInstance(system, definition);
				if (connectorFormInstance != null) {
					connectorFormInstance.setId(connectorFormInstance.getFormDefinition().getId());
					this.getFormService().export(connectorFormInstance, getBatch());
				}
			}
		}
		return system;
	}

	/**
	 * Export sync configurations
	 * 
	 * @param systemId
	 */
	private void exportSyncConfigs(UUID systemId) {
		SysSyncConfigFilter syncConfigFilter = new SysSyncConfigFilter();
		syncConfigFilter.setSystemId(systemId);
		List<AbstractSysSyncConfigDto> syncs = synchronizationConfigService.find(syncConfigFilter, null).getContent();
		if (syncs.isEmpty()) {
			synchronizationConfigService.export(ExportManager.BLANK_UUID, getBatch());
			// Set parent field -> set authoritative mode. If none sync exists, then default
			// type will be used.
			this.getExportManager().setAuthoritativeMode(SysSyncConfig_.systemMapping.getName(),
					SysSyncConfigFilter.PARAMETER_SYSTEM_ID, synchronizationConfigService.getDtoClass(), getBatch());
		}
		syncs.forEach(sync -> {
			synchronizationConfigService.export(sync.getId(), getBatch());
			// Set parent field -> set authoritative mode. For sync could be more then one
			// DTO types.
			this.getExportManager().setAuthoritativeMode(SysSyncConfig_.systemMapping.getName(),
					SysSyncConfigFilter.PARAMETER_SYSTEM_ID, sync.getClass(), getBatch());

		});
	}

	/**
	 * Export mapped attributes
	 * 
	 * @param systemId
	 */
	private void exportMappedAttributes(UUID systemId) {
		SysSystemMappingFilter systemMappingFilter = new SysSystemMappingFilter();
		systemMappingFilter.setSystemId(systemId);
		List<SysSystemMappingDto> systemMappings = systemMappingService.find(systemMappingFilter, null).getContent();
		if (systemMappings.isEmpty()) {
			systemMappingService.export(ExportManager.BLANK_UUID, getBatch());
		}
		systemMappings.forEach(mapping -> {
			systemMappingService.export(mapping.getId(), getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(SysSystemMapping_.objectClass.getName(), "systemId",
				SysSystemMappingDto.class, getBatch());
	}

	/**
	 * Export system schemas
	 * 
	 * @param systemId
	 */
	private void exportSchema(UUID systemId) {
		SysSchemaObjectClassFilter objectClassFilter = new SysSchemaObjectClassFilter();
		objectClassFilter.setSystemId(systemId);
		List<SysSchemaObjectClassDto> objectClasses = objectClassService.find(objectClassFilter, null).getContent();
		if (objectClasses.isEmpty()) {
			schemaObjectClassService.export(ExportManager.BLANK_UUID, getBatch());
		}
		objectClasses.forEach(schema -> {
			schemaObjectClassService.export(schema.getId(), getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(SysSchemaObjectClass_.system.getName(), "systemId",
				SysSchemaObjectClassDto.class, getBatch());
	}

	/**
	 * Export break configurations
	 * 
	 * @param systemId
	 */
	private void exportBreakConfig(UUID systemId) {
		SysProvisioningBreakConfigFilter provisioningBreakConfigFilter = new SysProvisioningBreakConfigFilter();
		provisioningBreakConfigFilter.setSystemId(systemId);
		List<SysProvisioningBreakConfigDto> breakConfigs = provisioningBreakService
				.find(provisioningBreakConfigFilter, null).getContent();
		if (breakConfigs.isEmpty()) {
			provisioningBreakService.export(ExportManager.BLANK_UUID, getBatch());
		}
		breakConfigs.forEach(breakConfig -> {
			provisioningBreakService.export(breakConfig.getId(), getBatch());
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(SysSchemaObjectClass_.system.getName(), "systemId",
				SysProvisioningBreakConfigDto.class, getBatch());
	}

	@Override
	public List<String> getAuthorities() {
		List<String> authorities = super.getAuthorities();
		authorities.add(AccGroupPermission.SYSTEM_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_CREATE);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_UPDATE);
		return authorities;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(AccGroupPermission.SYSTEM_READ);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 1000;
	}

	@Override
	public ReadWriteDtoService<SysSystemDto, SysSystemFilter> getService() {
		return systemService;
	}

}
