package eu.bcvsolutions.idm.core.api.bulk.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.ExportImportType;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmExportImportService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.api.utils.ZipUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Abstract export bulk operation
 * 
 * @author Vít Švanda
 *
 */
public abstract class AbstractExportBulkAction<DTO extends AbstractDto, F extends BaseFilter>
		extends AbstractBulkAction<DTO, F> {

	@Autowired
	private ExportManager exportManager;
	@Autowired
	private IdmExportImportService exportImportService;
	@Autowired
	private AttachmentManager attachmentManager;
	@Autowired
	@Lazy
	private FormService formService;
	@Autowired
	@Lazy
	private IdmFormDefinitionService formDefinitionService;

	private IdmExportImportDto batch = null;
	private OperationResult itemException;
	// Name of export batch
	public static final String PROPERTY_NAME = "name";
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractExportBulkAction.class);

	@Override
	protected OperationResult processDto(DTO dto) {
		try {
			Assert.notNull(dto, "Role is required!");
			Assert.notNull(dto.getId(), "Id of system is required!");
			// Export DTO
			this.exportDto(dto);

			// Create directory for all exported classes. Some can be empty, its OK, we need
			// empty folder for authoritative mode (for delete others items on target IdM).
			batch.getExportOrder().forEach(descriptor -> {
				exportManager.createDtoDirectory(descriptor.getDtoClass(), batch);
			});

			batch.getExportedDtos().forEach(extportDto -> {
				exportManager.exportDto(extportDto, batch);
			});

			// Clear cache for next item
			batch.getExportedDtos().clear();
		} catch (Exception ex) {
			itemException = new OperationResult//
					.Builder(OperationState.EXCEPTION)//
							.setCause(ex)//
							.build();//
			return itemException;
		}

		return new OperationResult(OperationState.EXECUTED);
	}

	protected abstract void exportDto(DTO dto);

	@Override
	protected OperationResult end(OperationResult result, Exception ex) {
		Assert.notNull(batch, "Batch must exists!");
		Path tempDirectory = batch.getTempDirectory();
		if (tempDirectory != null) {
			Path zipPath = null;
			try {
				// Export batch as "manifest" contains basic batch information.
				exportManifest(tempDirectory);

				File tempDirectoryFile = tempDirectory.toFile();
				zipPath = Paths
						.get(MessageFormat.format("{0}.{1}", tempDirectory.toString(), ExportManager.EXTENSION_ZIP));

				// Zip all results and save as attachment
				ZipUtils.compress(tempDirectoryFile, zipPath.toString());
				InputStream inputStream = Files.newInputStream(zipPath);

				IdmAttachmentDto attachment = new IdmAttachmentDto();
				attachment.setName(zipPath.toFile().getName());
				attachment.setMimetype(ExportManager.APPLICATION_ZIP);
				attachment.setInputData(inputStream);
				attachment.setOwnerType(getLookupService().getOwnerType(IdmExportImportDto.class));

				attachment = attachmentManager.saveAttachment(batch, attachment);
				batch.setData(attachment.getId());
				batch = exportImportService.save(batch);

			} catch (IOException e) {
				result = new OperationResult.Builder(OperationState.EXCEPTION)
						.setException(new ResultCodeException(CoreResultCode.EXPORT_ZIP_FAILED, e)).build();
				return super.end(result, null);
			} finally {
				// Delete temp files.
				try {
					Files.walk(tempDirectory)//
							.sorted(Comparator.reverseOrder())//
							.map(Path::toFile)//
							.forEach(File::delete);

					if (zipPath != null) {
						zipPath.toFile().delete();
					}
				} catch (IOException e) {
					// Only log a error.
					LOG.error(ex.getLocalizedMessage(), ex);
				}
			}
		}

		// If some item failed, then whole bulk action will be marked as failed.
		if (itemException != null) {
			result = itemException;
		}
		result = super.end(result, ex);
		// Adds attachment metadata to the operation result (for download attachment
		// directly from bulk action modal dialog).
		addAttachmentMetadata(result);
		return result;
	}

	/**
	 * Adds attachment metadata to the operation result (for download attachment
	 * directly from bulk action modal dialog).
	 * 
	 * @param result
	 */
	private void addAttachmentMetadata(OperationResult result) {

		IdmLongRunningTaskDto task = getLongRunningTaskService().get(getLongRunningTaskId());
		OperationResult taskResult = task.getResult();

		if (OperationState.EXECUTED == taskResult.getState()) {
			ResultModel model = new DefaultResultModel(CoreResultCode.LONG_RUNNING_TASK_PARTITIAL_DOWNLOAD,
					ImmutableMap.of(//
							AttachableEntity.PARAMETER_DOWNLOAD_URL,
							MessageFormat.format("export-imports/{0}/download", batch.getId()),
							AttachableEntity.PARAMETER_OWNER_ID, batch.getId(), //
							AttachableEntity.PARAMETER_OWNER_TYPE, batch.getClass().getName()//
					));//

			taskResult.setModel(model);
			getLongRunningTaskService().save(task);
		}
	}

	/**
	 * Export batch as "manifest" contains basic batch information (name, creator
	 * ...)
	 * 
	 * @param tempDirectory
	 * @throws IOException
	 */
	private void exportManifest(Path tempDirectory) throws IOException {
		// Create copy of batch
		IdmExportImportDto batchToExport = new IdmExportImportDto(batch.getId());
		batchToExport.setName(batch.getName());
		batchToExport.setType(batch.getType());
		batchToExport.setResult(batch.getResult());
		batchToExport.setExecutorName(batch.getExecutorName());
		batchToExport.getExportOrder().addAll(batch.getExportOrder());
		EntityUtils.copyAuditFields(batch, batchToExport);

		exportManager.exportDto(batchToExport, batch);
		Path source = Paths.get(tempDirectory.toString(), IdmExportImportDto.class.getSimpleName(),
				MessageFormat.format("{0}.{1}", batch.getId().toString(), ExportManager.EXTENSION_JSON));
		Path target = Paths.get(tempDirectory.toString(), ExportManager.EXPORT_BATCH_FILE_NAME);
		// Move main export batch (create "Manifest")
		Files.move(source, target);
	}

	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		List<IdmFormAttributeDto> formAttributes = super.getFormAttributes();
		formAttributes.add(new IdmFormAttributeDto(PROPERTY_NAME, "Name", PersistentType.SHORTTEXT));

		return formAttributes;
	}

	protected ExportManager getExportManager() {
		return exportManager;
	}

	protected IdmExportImportService getExportImportSerivce() {
		return exportImportService;
	}

	protected AttachmentManager getAttachmentManager() {
		return attachmentManager;
	}

	protected IdmExportImportDto getBatch() {
		return batch;
	}

	protected FormService getFormService() {
		return formService;
	}

	protected IdmFormDefinitionService getFormDefinitionService() {
		return formDefinitionService;
	}

	protected void initBatch(String name) {
		if (batch == null) {
			String nameFromUser = (String) getProperties().get(PROPERTY_NAME);
			if (Strings.isNotEmpty(nameFromUser)) {
				name = nameFromUser;
			}

			batch = new IdmExportImportDto();
			batch.setName(name);
			batch.setType(ExportImportType.EXPORT);
			batch.setExecutorName(this.getName());
			batch.setLongRunningTask(getLongRunningTaskId());

			batch = exportImportService.save(batch, IdmBasePermission.CREATE);

		}
	}

}
