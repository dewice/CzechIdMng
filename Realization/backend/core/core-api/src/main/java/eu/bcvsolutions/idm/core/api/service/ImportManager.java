package eu.bcvsolutions.idm.core.api.service;

import java.io.File;
import java.io.InputStream;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.ImportContext;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Import manager
 * 
 * @author Vít Švanda
 *
 */
public interface ImportManager {
	
	/**
	 * Upload import file
	 * 
	 * @param name
	 * @param fileName
	 * @param inputStream
	 * @param permission
	 * @return
	 */
	IdmExportImportDto uploadImport(String name, String fileName, InputStream inputStream, BasePermission... permission);

	/**
	 * Execute import batch (running import LRT)
	 * 
	 * @param idmExportImportDto
	 * @param dryRun
	 * @return
	 */
	IdmExportImportDto executeImport(IdmExportImportDto idmExportImportDto, boolean dryRun);

	/**
	 * Internal executing of the batch
	 * 
	 * @param batch
	 * @param dryRun
	 * @param importTaskExecutor
	 * @return
	 */
	ImportContext internalExecuteImport(IdmExportImportDto batch, boolean dryRun, AbstractLongRunningTaskExecutor<OperationResult> importTaskExecutor);

	
	/**
	 * Converts given file to the DTO.
	 * 
	 * @param file
	 * @param dtoClass
	 * @param context
	 * @return
	 */
	BaseDto convertFileToDto(File file, Class<? extends BaseDto> dtoClass, ImportContext context);


}