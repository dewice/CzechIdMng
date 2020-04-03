package eu.bcvsolutions.idm.core.api.dto;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;

/**
 * Context for import
 * 
 * @author Vít Švanda
 *
 */
public class ImportContext {

	private Map<UUID, UUID> replacedIDs = Maps.newHashMap();
	private Path tempDirectory;
	private IdmExportImportDto manifest;
	private List<ExportDescriptorDto> exportDescriptors;
	private boolean dryRun;
	private IdmExportImportDto batch;
	private AbstractLongRunningTaskExecutor<OperationResult> importTaskExecutor;

	public Map<UUID, UUID> getReplacedIDs() {
		return replacedIDs;
	}

	public Path getTempDirectory() {
		return tempDirectory;
	}

	public ImportContext setTempDirectory(Path tempDirectory) {
		this.tempDirectory = tempDirectory;
		return this;
	}

	public IdmExportImportDto getManifest() {
		return manifest;
	}

	public ImportContext setManifest(IdmExportImportDto batch) {
		this.manifest = batch;
		return this;
	}

	public IdmExportImportDto getBatch() {
		return batch;
	}

	public ImportContext setBatch(IdmExportImportDto batch) {
		this.batch = batch;
		return this;
	}

	public List<ExportDescriptorDto> getExportDescriptors() {
		return exportDescriptors;
	}

	public ImportContext setExportDescriptors(List<ExportDescriptorDto> exportDescriptors) {
		this.exportDescriptors = exportDescriptors;
		return this;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public ImportContext setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	public ImportContext setImportTaskExecutor(AbstractLongRunningTaskExecutor<OperationResult> importTaskExecutor) {
		this.importTaskExecutor = importTaskExecutor;
		
		return this;
	}

	public AbstractLongRunningTaskExecutor<OperationResult> getImportTaskExecutor() {
		return this.importTaskExecutor;
	}

}