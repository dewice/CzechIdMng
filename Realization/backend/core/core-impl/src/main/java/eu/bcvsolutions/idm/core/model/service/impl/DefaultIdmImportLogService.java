package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.dto.IdmImportLogDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmImportLogFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult_;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmImportLogService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmExportImport_;
import eu.bcvsolutions.idm.core.model.entity.IdmImportLog;
import eu.bcvsolutions.idm.core.model.entity.IdmImportLog_;
import eu.bcvsolutions.idm.core.model.repository.IdmImportLogRepository;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * CRUD for import logs
 * 
 * @author Vít Švanda
 *
 */
@Service("importLogService")
public class DefaultIdmImportLogService extends
		AbstractEventableDtoService<IdmImportLogDto, IdmImportLog, IdmImportLogFilter> implements IdmImportLogService {

	@Autowired
	public DefaultIdmImportLogService(IdmImportLogRepository repository, EntityEventManager entityEventManager) {
		super(repository, entityEventManager);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.EXPORTIMPORT, getEntityClass());
	}

	@Override
	protected IdmImportLogDto toDto(IdmImportLog entity, IdmImportLogDto dto, IdmImportLogFilter filter) {
		dto = super.toDto(entity, dto, filter);
		if (dto != null && dto.getDtoId() != null) {
			// Set count of children
			IdmImportLogFilter childrenFilter = new IdmImportLogFilter();
			childrenFilter.setParent(dto.getDtoId());
			childrenFilter.setBatchId(dto.getBatch());
			
			dto.setChildrenCount(this.count(childrenFilter));
		}
		return dto;
	}

	@Override
	public boolean supportsToDtoWithFilter() {
		return true;
	}
	
	@Override
	@Transactional
	public IdmImportLogDto saveDistinct(IdmImportLogDto dto, BasePermission... permission) {
		if (this.isNew(dto)) {
			Assert.notNull(dto.getBatch(), "Batch ID must be filled for distinct save!");
			Assert.notNull(dto.getDtoId(), "DTO ID must be filled for distinct save!");
			
			IdmImportLogFilter filter = new IdmImportLogFilter();
			filter.setBatchId(dto.getBatch());
			filter.setDtoId(dto.getDtoId());
			
			// If exists import-log for same batch and DTO ID, then is used his ID (prevent creation of duplicated logs).
			if (this.count(filter) > 0) {
				IdmImportLogDto originalLog = this.find(filter, null).getContent().get(0);
				dto.setId(originalLog.getId());
			}
		}
		
		return super.save(dto, permission);
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmImportLog> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			IdmImportLogFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		
		if (StringUtils.isNotEmpty(filter.getText())) {
			predicates.add(builder.like(builder.lower(root.get(IdmImportLog_.type)), "%" + filter.getText().toLowerCase() + "%"));
		}

		UUID batchId = filter.getBatchId();
		if (batchId != null) {
			predicates.add(builder.equal(root.get(IdmImportLog_.batch).get(IdmExportImport_.id), batchId));
		}
		
		UUID parentId = filter.getParent();
		if (parentId != null) {
			predicates.add(builder.equal(root.get(IdmImportLog_.parentId), parentId));
		}

		UUID dtoId = filter.getDtoId();
		if (dtoId != null) {
			predicates.add(builder.equal(root.get(IdmImportLog_.dtoId), dtoId));
		}
		
		Boolean roots = filter.getRoots();
		if (roots != null && roots) {
			if (filter.getBatchId() == null) {
				predicates.add(builder.equal(root.get(IdmImportLog_.batch).get(IdmExportImport_.id), ExportManager.BLANK_UUID));
			}
			predicates.add(builder.isNull(root.get(IdmImportLog_.parentId)));
		}

		RequestOperationType operation = filter.getOperation();
		if (operation != null) {
			predicates.add(builder.equal(root.get(IdmImportLog_.operation), operation));
		}
		
		OperationState operationState = filter.getOperationState();
		if (operationState != null) {
			predicates.add(builder.equal(root.get(IdmImportLog_.result).get(OperationResult_.state), operationState));
		}
		

		return predicates;
	}

}
