package eu.bcvsolutions.idm.core.api.service;

import java.io.InputStream;

import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmExportImportFilter;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * CRUD for export and import data
 * 
 * @author Vít Švanda
 *
 */
public interface IdmExportImportService extends
	ReadWriteDtoService<IdmExportImportDto, IdmExportImportFilter>,
	AuthorizableService<IdmExportImportDto> {

	/**
	 * Download data (zip) from the batch.
	 * 
	 * @param batch
	 * @return
	 */
	InputStream download(IdmExportImportDto batch);

}
