package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaObjectClassFilter;
import eu.bcvsolutions.idm.acc.entity.SysSchemaAttribute_;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass;
import eu.bcvsolutions.idm.acc.repository.SysSchemaObjectClassRepository;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Default schema object class service
 * 
 * @author svandav
 *
 */
@Service
public class DefaultSysSchemaObjectClassService extends AbstractReadWriteDtoService<SysSchemaObjectClassDto, SysSchemaObjectClass, SysSchemaObjectClassFilter>
		implements SysSchemaObjectClassService {

	private final SysSchemaObjectClassRepository repository;
	private final SysSchemaAttributeService schemaAttributeService;
	private final SysSystemMappingService systemMappingService;
	@Autowired
	private ExportManager exportManager;
	
	@Autowired
	public DefaultSysSchemaObjectClassService(
			SysSchemaObjectClassRepository repository,
			SysSchemaAttributeService sysSchemaAttributeService,
			SysSystemMappingService systemMappingService) {
		super(repository);
		//
		Assert.notNull(sysSchemaAttributeService, "Schema attribute service is required!");
		Assert.notNull(systemMappingService, "Service is required.");
		//
		this.repository = repository;
		this.schemaAttributeService = sysSchemaAttributeService;
		this.systemMappingService = systemMappingService;
	}
	
	@Override
	protected Page<SysSchemaObjectClass> findEntities(SysSchemaObjectClassFilter filter, Pageable pageable, BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}
	
	@Override
	@Transactional
	public void delete(SysSchemaObjectClassDto schemaObjectClass, BasePermission... permission) {
		Assert.notNull(schemaObjectClass, "Schema object class is required.");
		//
		// remove all schema attributes for 
		SysSchemaAttributeFilter filter = new SysSchemaAttributeFilter();
		filter.setObjectClassId(schemaObjectClass.getId());
		schemaAttributeService.find(filter, null).forEach(schemaAttribute -> {
			schemaAttributeService.delete(schemaAttribute);
		});	
		// delete all mappings
		systemMappingService.findByObjectClass(schemaObjectClass, null, null).forEach(systemMapping -> {
			systemMappingService.delete(systemMapping);
		});
		//
		super.delete(schemaObjectClass, permission);
	}

	@Override
	public SysSchemaObjectClassDto clone(UUID id) {
		SysSchemaObjectClassDto original = this.get(id);
		Assert.notNull(original, "Schema must be found!");
		
		original.setId(null);
		EntityUtils.clearAuditFields(original);
		return original;
	}
	
	@Override
	public void export(UUID id, IdmExportImportDto batch) {
		super.export(id, batch);
		// Export schema attributes
		SysSchemaAttributeFilter filter = new SysSchemaAttributeFilter();
		filter.setObjectClassId(id);
		List<SysSchemaAttributeDto> schemaAttributes = schemaAttributeService.find(filter, null).getContent();
		if (schemaAttributes.isEmpty()) {
			schemaAttributeService.export(ExportManager.BLANK_UUID, batch);
		}
		schemaAttributes.forEach(schemaAttribute -> {
			schemaAttributeService.export(schemaAttribute.getId(), batch);
		});
		// Set parent field -> set authoritative mode.
		exportManager.setAuthoritativeMode(SysSchemaAttribute_.objectClass.getName(), "systemId",
				SysSchemaAttributeDto.class, batch);
	}
}
