package eu.bcvsolutions.idm.core.api.service;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;

/**
 * Export manager
 * 
 * @author Vít Švanda
 *
 */
public interface ExportManager {

	String EXTENSION_JSON = "json";
	String EXTENSION_ZIP = "zip";
	String APPLICATION_ZIP = "application/json";
	String EXPORT_BATCH_FILE_NAME = "export-batch.json";
	/**
	 * Workaround - I need to use BLANK UUID (UUID no exists in DB), because I have
	 * to ensure add all DTO types (in full deep) in correct order (even when no
	 * child entity exists (no schema, no sync ...)).
	 */
	UUID BLANK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	/**
	 * Exports given DTO to the JSON file to a temp.
	 * 
	 * @param dto
	 * @param batch
	 * @return
	 */
	IdmExportImportDto exportDto(BaseDto dto, IdmExportImportDto batch);

	/**
	 * Sets authoritative mode for given DTO class. It means data (this DTO class)
	 * for same super parent object will be deleted from target IdM if will missed
	 * in the batch.
	 * 
	 * @param parentField               - Defines name of field in this DTO class
	 *                                  pointing to a parent.
	 * @param superParentFilterProperty - Defines filter field (setter in filter
	 *                                  object), which will be used for search all
	 *                                  children on target IdM.
	 * @param dtoClass
	 * @param batch
	 */
	void setAuthoritativeMode(String parentField, String superParentFilterProperty, Class<? extends BaseDto> dtoClass,
			IdmExportImportDto batch);

	/**
	 * Sets authoritative mode for given DTO class. It means data (this DTO class)
	 * for same super parent object will be deleted from target IdM if will missed
	 * in the batch.
	 * 
	 * @param parentFields              - Defines name of fields in this DTO class
	 *                                  pointing to a parent. DTO will be show in
	 *                                  the log tree under parent defines by first
	 *                                  value!
	 * @param superParentFilterProperty - Defines filter field (setter in filter
	 *                                  object), which will be used for search all
	 *                                  children on target IdM.
	 * @param dtoClass
	 * @param batch
	 */
	void setAuthoritativeMode(Set<String> parentFields, String superParentFilterProperty,
			Class<? extends BaseDto> dtoClass, IdmExportImportDto batch);

	/**
	 * Create empty directory for given DTO type in temp.
	 * 
	 * @param dtoClass
	 * @param batch
	 * @return
	 */
	Path createDtoDirectory(Class<? extends BaseDto> dtoClass, IdmExportImportDto batch);

	/**
	 * Get export descriptor form given DTO type from the batch.
	 * 
	 * @param batch
	 * @param dtoClass
	 * @return
	 */
	ExportDescriptorDto getDescriptor(IdmExportImportDto batch, Class<? extends BaseDto> dtoClass);

}