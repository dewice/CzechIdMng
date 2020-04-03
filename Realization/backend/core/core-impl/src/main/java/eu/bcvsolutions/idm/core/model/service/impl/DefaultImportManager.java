package eu.bcvsolutions.idm.core.model.service.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.ExportImportType;
import eu.bcvsolutions.idm.core.api.domain.Inheritable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.EmbeddedDto;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.IdmImportLogDto;
import eu.bcvsolutions.idm.core.api.dto.ImportContext;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmImportLogFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmExportImportService;
import eu.bcvsolutions.idm.core.api.service.IdmImportLogService;
import eu.bcvsolutions.idm.core.api.service.ImportManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.api.utils.ZipUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.ecm.api.config.AttachmentConfiguration;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.scheduler.api.dto.LongRunningFutureTask;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.scheduler.task.impl.ImportTaskExecutor;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Import manager
 * 
 * @author Vít Švanda
 *
 */

@Service("importManager")
public class DefaultImportManager implements ImportManager {

	@Autowired
	private IdmExportImportService exportImportService;
	@Autowired
	private IdmImportLogService importLogService;
	@Autowired
	private AttachmentManager attachmentManager;
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private AttachmentConfiguration attachmentConfiguration;
	@Autowired
	private LookupService lookupService;
	@Autowired
	private LongRunningTaskManager longRunningTaskManager;
	@Autowired
	private FormService formService;
	@Autowired
	private SecurityService securityService;
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultImportManager.class);

	@Override
	@Transactional
	public IdmExportImportDto uploadImport(String name, String fileName, InputStream inputStream, BasePermission... permission) {
		Assert.notNull(name, "Name cannot be null!");
		Assert.notNull(fileName, "File name cannot be null!");
		Assert.notNull(inputStream, "Input stream cannot be null!");
		LOG.info("Upload of a import [{}] starts ...", name);

		IdmExportImportDto batch = new IdmExportImportDto();
		batch.setName(name);
		batch.setType(ExportImportType.IMPORT);
		batch = exportImportService.save(batch);

		IdmAttachmentDto attachment = new IdmAttachmentDto();
		attachment.setName(fileName);
		attachment.setMimetype(ExportManager.APPLICATION_ZIP);
		attachment.setInputData(inputStream);
		attachment.setOwnerType(lookupService.getOwnerType(IdmExportImportDto.class));

		attachment = attachmentManager.saveAttachment(batch, attachment);

		Path tempDirectory = null;
		try {
			tempDirectory = extractZip(attachment);

			IdmExportImportDto manifest = validate(tempDirectory);
			batch.setName(manifest.getName());
			batch.setExecutorName(manifest.getExecutorName());
			batch.setData(attachment.getId());
			batch = exportImportService.save(batch, permission);
		} finally {
			// Delete temp files.
			try {
				Files.walk(tempDirectory)//
						.sorted(Comparator.reverseOrder())//
						.map(Path::toFile)//
						.forEach(File::delete);
			} catch (IOException ex) {
				// Only log a error.
				LOG.error(ex.getLocalizedMessage(), ex);
			}
		}

		return batch;
	}

	@Override
	@Transactional
	public IdmExportImportDto executeImport(IdmExportImportDto importBatch, boolean dryRun) {		
		Assert.notNull(importBatch, "Batch cannot be null!");
		Assert.notNull(importBatch.getId(), "Id of batch cannot be null!");
		LOG.info("Import [{}, dry-run: {}] starts ...", importBatch.toString(), dryRun);

		if (!dryRun && !securityService.isAdmin()) {
			// Only super-admin can execute import!
			throw new ResultCodeException(CoreResultCode.IMPORT_CAN_EXECUTE_ONLY_ADMIN);
		}

		OperationResult operationResult = importBatch.getResult();
		if (operationResult != null && OperationState.RUNNING == operationResult.getState()) {
			throw new ResultCodeException(CoreResultCode.IMPORT_IS_ALREADY_RUNNING,
					ImmutableMap.of("batch", importBatch.toString()));
		}

		ImportTaskExecutor lrt = new ImportTaskExecutor(importBatch.getId(), dryRun);
		LongRunningFutureTask<OperationResult> result = longRunningTaskManager.execute(lrt);

		UUID taskId = result.getExecutor().getLongRunningTaskId();
		importBatch.setLongRunningTask(taskId);

		return exportImportService.save(importBatch);
	}

	@Override
	@Transactional
	public ImportContext internalExecuteImport(IdmExportImportDto batch, boolean dryRun,
			AbstractLongRunningTaskExecutor<OperationResult> importTaskExecutor) {
		Assert.notNull(batch, "Batch cannot be null!");
		Assert.notNull(batch.getId(), "Batch ID cannot be null!");
		LOG.info("Internal import [{}, dry-run: {}] starts ...", batch.toString(), dryRun);
		
		// Delete all logs for this batch.
		IdmImportLogFilter logFilter = new IdmImportLogFilter();
		logFilter.setBatchId(batch.getId());
		importLogService.findIds(logFilter, null)//
				.getContent()//
				.forEach(logId -> {
					importLogService.deleteById(logId);
				});

		IdmAttachmentDto attachment = attachmentManager.get(batch.getData());
		Path tempDirectory = null;
		try {
			tempDirectory = extractZip(attachment);
			// Load manifest - batch contains order of import
			IdmExportImportDto manifest = validate(tempDirectory);

			ImportContext context = new ImportContext();
			context.setTempDirectory(tempDirectory)//
					.setManifest(manifest)//
					.setExportDescriptors(manifest.getExportOrder())//
					.setDryRun(dryRun)//
					.setBatch(batch)//
					.setImportTaskExecutor(importTaskExecutor);

			// Set count of all files in the batch (minus manifest)
			long countOfFiles = countOfFiles(tempDirectory);
			context.getImportTaskExecutor().setCounter(0L);
			context.getImportTaskExecutor().setCount(countOfFiles - 1);

			// Import new and update exist DTOs.
			manifest.getExportOrder().forEach(descriptor -> {
				this.executeImportForType(descriptor, context);
			});

			// Delete redundant DTOs.
			Lists.reverse(manifest.getExportOrder()).forEach(descriptor -> {
				this.removeRedundant(descriptor, context);
			});

			return context;
		} finally {
			// Delete temp files.
			try {
				Files.walk(tempDirectory)//
						.sorted(Comparator.reverseOrder())//
						.map(Path::toFile)//
						.forEach(File::delete);
			} catch (IOException ex) {
				// Only log a error.
				LOG.error(ex.getLocalizedMessage(), ex);
			}
			LOG.info("Internal import [{}, dry-run: {}] ended", batch.toString(), dryRun);
		}
	}

	/**
	 * Ensures add new and update existed DTOs by given batch.
	 * 
	 * @param dtoClass
	 * @param context
	 */
	private void executeImportForType(ExportDescriptorDto descriptor, ImportContext context) {

		Class<? extends BaseDto> dtoClass = descriptor.getDtoClass();
		Path dtoTypePath = Paths.get(context.getTempDirectory().toString(), dtoClass.getSimpleName());

		try {
			List<BaseDto> dtos = Files.walk(dtoTypePath)//
					.filter(path -> Files.isRegularFile(path))//
					.map(path -> {
						BaseDto dto = convertFileToDto(path.toFile(), dtoClass, context);
						Assert.notNull(dto, "DTO cannot be null after conversion from the batch!");

						return dto;
					}).collect(Collectors.toList());

			if (dtos.isEmpty()) {
				return;
			}

			// Sorts all DTOs for this type (maybe it is tree).
			dtos = sortsDTOs(dtoClass, dtos);

			dtos.forEach(dto -> {

				// Increase counter and update state of import LRT.
				context.getImportTaskExecutor().increaseCounter();
				context.getImportTaskExecutor().updateState();

				BaseDto parentDto = getParentDtoFromBatch(dto, context);
				if (parentDto == null) {
					parentDto = dto;
				}

				BaseDto originalDto = dto;
				try {
					dto = makeAdvancedPairing(dto, context, dtoClass);
					if (dto == null) {
						// If DTO after advanced pairing is null, then was not found and is optional ->
						// skip.
						IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), originalDto,
								RequestOperationType.ADD, (UUID) parentDto.getId());
						ResultModel resultModel = new DefaultResultModel(CoreResultCode.IMPORT_DTO_SKIPPED,
								ImmutableMap.of("dto", originalDto.toString()));
						dtoLog.setResult(
								new OperationResultDto.Builder(OperationState.CANCELED).setModel(resultModel).build());
						importLogService.saveDistinct(dtoLog);

						return;
					}
				} catch (ResultCodeException ex) {
					if (context.isDryRun() && ex.getError() != null && ex.getError().getError() != null
							&& CoreResultCode.IMPORT_ADVANCED_PARING_FAILED_NOT_FOUND.name()
									.equals(ex.getError().getError().getStatusEnum())) {
						// Not found DTO we will mark as skipped in dry run mode.
						IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), originalDto,
								RequestOperationType.ADD, (UUID) parentDto.getId());
						dtoLog.setResult(
								new OperationResultDto.Builder(OperationState.EXCEPTION).setException(ex).build());
						importLogService.saveDistinct(dtoLog);

						return;
					} else if (ex.getError() != null && ex.getError().getError() != null
							&& CoreResultCode.IMPORT_ADVANCED_PARING_NOT_FOUND_OPTIONAL.name()
									.equals(ex.getError().getError().getStatusEnum())) {
						// Not found DTO, but optional, we will mark as skipped.
						IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), originalDto,
								RequestOperationType.ADD, (UUID) parentDto.getId());
						dtoLog.setResult(
								new OperationResultDto.Builder(OperationState.CANCELED).setException(ex).build());
						importLogService.saveDistinct(dtoLog);

						return;
					}
					throw ex;
				}

				Class<? extends BaseDto> serviceDtoClass = dtoClass;

				if (dto instanceof IdmFormInstanceDto) {
					// Form instance is very special here (doesn't have entity in DB).

					IdmFormInstanceDto formInstance = (IdmFormInstanceDto) dto;
					IdmFormDefinitionDto definition = formInstance.getFormDefinition();
					Assert.notNull(definition, "Definition cannot be null for import!");

					CoreEvent<IdmFormInstanceDto> event = new CoreEvent<IdmFormInstanceDto>(CoreEventType.UPDATE,
							formInstance);

					// Check if owner exist (UPDATE/ADD)
					@SuppressWarnings("unchecked")
					Class<? extends BaseDto> ownerType = (Class<? extends BaseDto>) ((IdmFormInstanceDto) dto)
							.getOwnerType();
					UUID ownerId = UUID.fromString((String) ((IdmFormInstanceDto) dto).getOwnerId());
					BaseDto ownerDto = this.getDtoService(ownerType).get(ownerId);

					IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), dto,
							ownerDto != null ? RequestOperationType.UPDATE : RequestOperationType.ADD, ownerId);
					if (!context.isDryRun()) {
						formService.publish(event);
						dtoLog.setResult(new OperationResultDto(OperationState.EXECUTED));
					} else {
						dtoLog.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
								.setModel(new DefaultResultModel(CoreResultCode.IMPORT_EXECUTED_AS_DRYRUN))//
								.build());
					}
					importLogService.saveDistinct(dtoLog);

					return;
				}

				if (dto.getClass().isAnnotationPresent(Inheritable.class)) {
					serviceDtoClass = dto.getClass().getAnnotation(Inheritable.class).dtoService();
				}

				ReadWriteDtoService<BaseDto, ?> dtoService = getDtoService(serviceDtoClass);
				BaseDto currentDto = dtoService.get(dto.getId());

				if (currentDto != null) {
					// DTO with same ID already exists -> update.
					IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), dto, RequestOperationType.UPDATE,
							(UUID) parentDto.getId());
					if (!context.isDryRun()) {
						dtoService.save(dto);
						dtoLog.setResult(new OperationResultDto(OperationState.EXECUTED));
					} else {
						dtoLog.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
								.setModel(new DefaultResultModel(CoreResultCode.IMPORT_EXECUTED_AS_DRYRUN))//
								.build());
					}
					importLogService.saveDistinct(dtoLog);

					return;
				}
				if (dto instanceof Codeable) {
					// We try to find exists DTO by code.
					currentDto = lookupService.lookupDto(serviceDtoClass, ((Codeable) dto).getCode());
				}

				if (dto instanceof IdmFormDefinitionDto) {
					IdmFormDefinitionDto definition = (IdmFormDefinitionDto) dto;
					// We try to find exists definition by code and type (IdmFormDefinitionDto is
					// not Codeable).
					currentDto = formService.getDefinition(definition.getType(), definition.getCode());
				}
				if (dto instanceof IdmFormAttributeDto) {
					IdmFormAttributeDto attribute = (IdmFormAttributeDto) dto;
					IdmFormDefinitionDto definition = formService.getDefinition(attribute.getFormDefinition());
					if (definition != null) {
						// We try to find exists attribute definition by code and form definition.
						currentDto = formService.getAttribute(definition, attribute.getCode());
					} else {
						currentDto = null;
					}
				}

				if (currentDto != null) {
					// We found current DTO in IdM.
					// Save old and new ID for next DTOs.
					context.getReplacedIDs().put((UUID) dto.getId(), (UUID) currentDto.getId());
					// We have to change the ID in import DTO.
					dto.setId(currentDto.getId());
					// Update current DTO by batch DTO.
					IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), dto, RequestOperationType.UPDATE,
							(UUID) parentDto.getId());
					if (!context.isDryRun()) {
						dtoService.save(dto);
						dtoLog.setResult(new OperationResultDto(OperationState.EXECUTED));
					} else {
						dtoLog.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
								.setModel(new DefaultResultModel(CoreResultCode.IMPORT_EXECUTED_AS_DRYRUN))//
								.build());
					}
					importLogService.saveDistinct(dtoLog);
				} else {
					IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), dto, RequestOperationType.ADD,
							(UUID) parentDto.getId());
					// No current DTO was found -> create.
					if (!context.isDryRun()) {
						dtoService.save(dto);
						dtoLog.setResult(new OperationResultDto(OperationState.EXECUTED));
					} else {
						dtoLog.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
								.setModel(new DefaultResultModel(CoreResultCode.IMPORT_EXECUTED_AS_DRYRUN))//
								.build());
					}
					importLogService.saveDistinct(dtoLog);
				}
			});

		} catch (IOException | IllegalArgumentException e) {
			throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_IO_FAILED, e);
		}
	}

	private List<BaseDto> sortsDTOs(Class<? extends BaseDto> dtoClass, List<BaseDto> dtos) {

		String itselfField = this.containsItselfTypeRelation(dtoClass);
		if (itselfField == null) {
			return dtos;
		}
		// Itself field was found.
		// This DTO class is "tree" and we need to sorting all DTOs first.

		// Find roots (parentId == null)
		Set<BaseDto> roots = dtos.stream()//
				.filter(dto -> {
					UUID id = getFieldUUIDValue(itselfField, dto, dtoClass);
					if (id == null) {
						return true;
					}
					return false;
				}).collect(Collectors.toSet());
		if (roots.isEmpty()) {
			throw new ResultCodeException(CoreResultCode.IMPORT_FAILED_ROOT_NOT_FOUND,
					ImmutableMap.of("type", dtoClass));
		}

		// Sorts all children by parent ID.
		List<BaseDto> sortedDtos = Lists.newArrayList();
		roots.forEach(root -> {
			sortedDtos.add(root);
			this.sortingChildren((UUID) root.getId(), itselfField, dtoClass, dtos, sortedDtos);
		});

		return sortedDtos;
	}

	/**
	 * Sorts recursively all children by parent ID.
	 * 
	 * @param parentId
	 * @param itselfField
	 * @param dtoClass
	 * @param dtos
	 * @param orderedDtos
	 */
	private void sortingChildren(UUID parentId, String itselfField, Class<? extends BaseDto> dtoClass,
			List<BaseDto> dtos, List<BaseDto> orderedDtos) {

		List<BaseDto> chlidren = dtos.stream()//
				.filter(dto -> {
					UUID id = getFieldUUIDValue(itselfField, dto, dtoClass);
					if (parentId.equals(id)) {
						return true;
					}
					return false;
				}).collect(Collectors.toList());
		// Add all children to ordered list
		orderedDtos.addAll(chlidren);
		chlidren.forEach(child -> {
			this.sortingChildren((UUID) child.getId(), itselfField, dtoClass, dtos, orderedDtos);
		});
	}

	/**
	 * Returns UUID value from given field in DTO.
	 * 
	 * @param itselfField
	 * @param dto
	 * @param dtoClass
	 * @return
	 */
	private UUID getFieldUUIDValue(String itselfField, BaseDto dto, Class<? extends BaseDto> dtoClass) {
		try {
			Object value = new PropertyDescriptor(itselfField, dtoClass).getReadMethod().invoke(dto);
			if (value != null && !(value instanceof UUID)) {
				throw new ResultCodeException(CoreResultCode.IMPORT_FIELD_MUST_CONTAINS_UUID,
						ImmutableMap.of("type", value.getClass()));
			}
			return (UUID) value;
		} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
			throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_REFLECTION_FAILED, e);
		}
	}

	/**
	 * Check if given DTO class contains field with same DTO class (is tree). If yes
	 * returns name of this field.
	 * 
	 * @param dtoClass
	 * @return
	 */
	private String containsItselfTypeRelation(Class<? extends BaseDto> dtoClass) {
		Field[] fields = dtoClass.getDeclaredFields();
		for (Field field : fields) {

			Class<? extends AbstractDto> dtoClassField = this.getDtoClassFromField(dtoClass, field.getName());
			if (dtoClass == dtoClassField) {
				return field.getName();
			}
		}
		return null;
	}

	/**
	 * Make advanced paring
	 * 
	 * @param dto
	 * @param context
	 * @param dtoClass
	 * @return
	 */
	private BaseDto makeAdvancedPairing(BaseDto dto, ImportContext context, Class<? extends BaseDto> dtoClass) {
		ExportDescriptorDto descriptor = this.getDescriptor(context, dtoClass);

		for (String advancedParingField : descriptor.getAdvancedParingFields()) {
			try {
				Class<? extends AbstractDto> dtoClassField = this.getDtoClassFromField(dtoClass, advancedParingField);
				if (dtoClassField == null) {
					throw new ResultCodeException(CoreResultCode.IMPORT_FIELD_EMBEDDED_ANNOTATION_MISSING,
							ImmutableMap.of("field", advancedParingField));
				}
				UUID id = this.getFieldUUIDValue(advancedParingField, dto, dtoClass);

				if (id == null) {
					// ID is null -> no relation exists -> continues
					continue;
				}

				// Get service for DTO in field.
				Class<? extends BaseDto> serviceDtoClass = dtoClassField;
				if (dtoClassField.isAnnotationPresent(Inheritable.class)) {
					serviceDtoClass = dto.getClass().getAnnotation(Inheritable.class).dtoService();
				}
				ReadWriteDtoService<BaseDto, ?> dtoServiceForField = getDtoService(serviceDtoClass);

				BaseDto currentFieldDto = dtoServiceForField.get(id);
				if (currentFieldDto != null) {
					// DTO exists -> no change is necessary.
					continue;
				}

				// DTO not found by ID, we will try find it by code.
				if (dto instanceof AbstractDto) {
					Path dtoPath = Paths.get(context.getTempDirectory().toString(), dtoClass.getSimpleName(),
							MessageFormat.format("{0}.{1}", dto.getId().toString(), ExportManager.EXTENSION_JSON));

					EmbeddedDto embeddedDto = (EmbeddedDto) this.convertFileToDto(dtoPath.toFile(), EmbeddedDto.class,
							context);
					JsonNode batchFieldDtoAsString = embeddedDto.getEmbedded().get(advancedParingField);

					if (batchFieldDtoAsString == null) {
						if (descriptor.isOptional()) {
							return null;
						} else {
							Assert.notNull(batchFieldDtoAsString,
									MessageFormat.format(
											"Embedded map must contains DTO for advaced paring field [{0}]",
											advancedParingField));
						}
					}

					BaseDto batchFieldDto = null;
					try {
						batchFieldDto = this.convertStringToDto(batchFieldDtoAsString.toString(), dtoClassField,
								context);
					} catch (IOException e) {
						throw new ResultCodeException(CoreResultCode.IMPORT_CONVERT_TO_DTO_FAILED,
								ImmutableMap.of("file", "Converted from String.", "dto", dtoClass), e);
					}

					if (batchFieldDto instanceof Codeable) {
						String code = ((Codeable) batchFieldDto).getCode();
						if (Strings.isNotEmpty(code)) {
							currentFieldDto = lookupService.lookupDto(serviceDtoClass, code);
							if (currentFieldDto != null) {
								// DTO for given code exists -> replace ID by this new in given DTO.
								new PropertyDescriptor(advancedParingField, dtoClass)//
										.getWriteMethod()//
										.invoke(dto, currentFieldDto.getId());
								// Save old and new ID for next DTOs.
								context.getReplacedIDs().put(id, (UUID) currentFieldDto.getId());
								continue;
							} else {
								// No target DTO was found on target IdM.
								// If is DTO set as optional, we will only skip this DTO.
								if (descriptor.isOptional()) {
									throw new ResultCodeException(
											CoreResultCode.IMPORT_ADVANCED_PARING_NOT_FOUND_OPTIONAL,
											ImmutableMap.of("field", advancedParingField, "dto", dto.toString(),
													"notFoundDto", batchFieldDto.toString(), "code", code));
								}
								throw new ResultCodeException(CoreResultCode.IMPORT_ADVANCED_PARING_FAILED_NOT_FOUND,
										ImmutableMap.of("field", advancedParingField, "dto", dto.toString(),
												"notFoundDto", batchFieldDto.toString(), "code", code));
							}
						}
					}
				}

				// No target DTO was found on target IdM.
				// If is DTO set as optional, we will no throw a exception, but only return null
				// (skip this DTO).
				if (descriptor.isOptional()) {
					return null;
				}

			} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e) {
				throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_REFLECTION_FAILED, e);
			}

		}
		return dto;
	}

	/**
	 * Ensures delete redundant entities in target IdM. Deletes DTOs are within
	 * golden DTO type (super parent = typically system, role, ...).
	 * 
	 * @param descriptor
	 * @param context
	 */
	private void removeRedundant(ExportDescriptorDto descriptor, ImportContext context) {

		Class<? extends BaseDto> dtoClass = descriptor.getDtoClass();

		// Does this DTO support the authoritative mode?
		boolean supportsAuthoritativeMode = descriptor.isSupportsAuthoritativeMode();

		if (!supportsAuthoritativeMode) {
			return;
		}

		String superParentFilterProperty = descriptor.getSuperParentFilterProperty();
		Assert.notNull(superParentFilterProperty, "For authoritative mode must be superParentFilterProperty defined!");

		// Find super parent (gold) DTO (typically it is DTO for system, role ...)
		Class<? extends AbstractDto> superParentDtoClass = getSuperParentDtoClass(descriptor, context);

		Path superParentDtoTypePath = Paths.get(context.getTempDirectory().toString(),
				superParentDtoClass.getSimpleName());
		try {
			// Find all super parent IDs for this DTO type in batch.
			Set<UUID> superParentIdsInBatch = Files.walk(superParentDtoTypePath)//
					.filter(path -> Files.isRegularFile(path))//
					.map(path -> {
						BaseDto dto = convertFileToDto(path.toFile(), superParentDtoClass, context);
						return (UUID) dto.getId();
					}).collect(Collectors.toSet());

			Set<Class<? extends BaseDto>> inheritedClasses = getInheritedClasses(dtoClass, context.getManifest());
			// Find all IDs for all children classes
			Set<Serializable> childrenIdsInBatch = Sets.newHashSet();
			for (Class<? extends BaseDto> inheritedClass : inheritedClasses) {
				// Find all IDs for this DTO type in batch.
				Path dtoTypePath = Paths.get(context.getTempDirectory().toString(), inheritedClass.getSimpleName());
				Set<Serializable> childrenIds = Files.walk(dtoTypePath)//
						.filter(path -> Files.isRegularFile(path))//
						.map(path -> (BaseDto) convertFileToDto(path.toFile(), inheritedClass, context))//
						.map(dto -> {
							// If ID has been replaced, then we need to also replace it.
							if (context.getReplacedIDs().containsKey(dto.getId())) {
								return context.getReplacedIDs().get(dto.getId());
							}
							return dto.getId();
						}).collect(Collectors.toSet());
				childrenIdsInBatch.addAll(childrenIds);
			}

			superParentIdsInBatch.forEach(superParentId -> {
				try {
					Class<? extends BaseDto> serviceDtoClass = dtoClass;
					if (dtoClass.isAnnotationPresent(Inheritable.class)) {
						serviceDtoClass = dtoClass.getAnnotation(Inheritable.class).dtoService();
					}

					ReadWriteDtoService<BaseDto, BaseFilter> dtoService = getDtoService(serviceDtoClass);
					BaseFilter filterBase = (BaseFilter) dtoService.getFilterClass().newInstance();

					// Fill super-parent-property by superParentId (call setter = check if filter is
					// implemented).
					new PropertyDescriptor(superParentFilterProperty, dtoService.getFilterClass()).getWriteMethod()
							.invoke(filterBase, superParentId);

					// Load all IDs in IdM for this parent ID.
					List<UUID> childrenIdsInIdM = (List<UUID>) dtoService.find(filterBase, null)//
							.getContent()//
							.stream()//
							.map(childDto -> ((AbstractDto) childDto).getId())//
							.collect(Collectors.toList());

					// IDs to delete = entities missing in the batch.
					Set<UUID> idsToDelete = childrenIdsInIdM.stream()//
							.filter(idmId -> !childrenIdsInBatch.contains(idmId))//
							.collect(Collectors.toSet());

					idsToDelete.forEach(id -> {
						BaseDto baseDto = dtoService.get(id);
						IdmImportLogDto dtoLog = new IdmImportLogDto(context.getBatch(), baseDto,
								RequestOperationType.REMOVE, superParentId);
						if (!context.isDryRun()) {
							dtoService.delete(baseDto);
							dtoLog.setResult(new OperationResultDto(OperationState.EXECUTED));
						} else {
							dtoLog.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
									.setModel(new DefaultResultModel(CoreResultCode.IMPORT_EXECUTED_AS_DRYRUN))//
									.build());
						}
						importLogService.saveDistinct(dtoLog);
					});
				} catch (ReflectiveOperationException | IllegalArgumentException | IntrospectionException e) {
					throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_REFLECTION_FAILED, e);
				}
			});
		} catch (IOException e) {
			throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_IO_FAILED, e);
		}
	}

	/**
	 * Find all inherited classes for given dtoClass
	 * 
	 * @param dtoClass
	 * @param manifest
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Set<Class<? extends BaseDto>> getInheritedClasses(Class<? extends BaseDto> dtoClass,
			IdmExportImportDto manifest) {

		if (!(dtoClass.isAnnotationPresent(Inheritable.class))) {
			return Sets.newHashSet(dtoClass);
		}

		Class<? extends BaseDto> parentClass = dtoClass.getAnnotation(Inheritable.class).dtoService();

		return manifest.getExportOrder()//
				.stream()//
				.map(descriptor -> {
					return descriptor.getDtoClass();
				}).filter(dto -> {
					if (dto.isAnnotationPresent(Inheritable.class)) {
						if (parentClass == dto.getAnnotation(Inheritable.class).dtoService()) {
							return true;
						}
					}
					return false;
				}).collect(Collectors.toSet());
	}

	/**
	 * Find super parent DTO for given DTO. Searching is only in the batch.
	 * 
	 * @param dto
	 * @param context
	 * @return
	 */
	@SuppressWarnings("unused")
	private BaseDto getSuperParentDtoFromBatch(BaseDto dto, ImportContext context) {

		BaseDto parentDto = dto;
		BaseDto lastParentDto = dto;
		while (true) {
			parentDto = this.getParentDtoFromBatch(parentDto, context);
			if (parentDto == null) {
				return lastParentDto;
			}
			lastParentDto = parentDto;
		}
	}

	/**
	 * Find parent DTO in the batch by parent field in given DTO.
	 * 
	 * @param dto
	 * @param context
	 * @return
	 * @throws IOException
	 */
	private BaseDto getParentDtoFromBatch(BaseDto dto, ImportContext context) {
		ExportDescriptorDto descriptor = this.getDescriptor(context, dto.getClass());
		Assert.notNull(descriptor, "Descriptor cannot be null!");

		Set<String> parentFields = descriptor.getParentFields();
		if (parentFields.isEmpty()) {
			return null;
		}

		for (String parentField : parentFields) {
			try {
				UUID parentId = this.getFieldUUIDValue(parentField, dto, dto.getClass());
				Class<? extends AbstractDto> parentType = this.getDtoClassFromField(dto.getClass(), parentField);
				Assert.notNull(parentType, "Parent type cannot be null!");

				if (parentId == null) {
					continue;
				}

				Path dtoTypePath = Paths.get(context.getTempDirectory().toString(), parentType.getSimpleName());
				BaseDto parentDto = Files.walk(dtoTypePath)//
						.filter(path -> Files.isRegularFile(path))//
						.map(path -> (BaseDto) convertFileToDto(path.toFile(), parentType, context))//
						.filter(d -> parentId.equals(d.getId()))//
						.findFirst()//
						.orElse(null);

				if (parentDto != null) {
					return parentDto;
				}
			} catch (IOException ex) {
				throw new ResultCodeException(CoreResultCode.IMPORT_ZIP_EXTRACTION_FAILED, ex);
			}
		}
		return null;
	}

	private long countOfFiles(Path mainPath) {
		try {
			return Files.walk(mainPath)//
					.filter(path -> Files.isRegularFile(path))//
					.count();
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.IMPORT_ZIP_EXTRACTION_FAILED, ex);
		}
	}

	private Class<? extends AbstractDto> getSuperParentDtoClass(ExportDescriptorDto descriptor, ImportContext context) {
		Class<? extends BaseDto> dtoClass = descriptor.getDtoClass();
		Set<String> parentFields = descriptor.getParentFields();

		if (!CollectionUtils.isEmpty(parentFields)) {
			String parentField = parentFields.toArray(new String[0])[0];
			Class<? extends AbstractDto> superParentDtoClass = null;
			while (superParentDtoClass == null) {
				Class<? extends AbstractDto> parentDtoClass = this.getDtoClassFromField(dtoClass, parentField);
				ExportDescriptorDto parentDescriptor = getDescriptor(context, parentDtoClass);

				Assert.notNull(parentDescriptor, "Descriptor was not found!");
				if (CollectionUtils.isEmpty(parentDescriptor.getParentFields())) {
					superParentDtoClass = parentDtoClass;
				} else {
					dtoClass = parentDtoClass;
					parentField = parentDescriptor.getParentFields().toArray(new String[0])[0];
				}
			}
			return superParentDtoClass;
		}
		return null;
	}

	private ExportDescriptorDto getDescriptor(ImportContext context, Class<? extends BaseDto> parentDtoClass) {
		ExportDescriptorDto parentDescriptor = context.getExportDescriptors().stream()
				.filter(des -> des.getDtoClass().equals(parentDtoClass)).findFirst().orElse(null);
		return parentDescriptor;
	}

	private Class<? extends AbstractDto> getDtoClassFromField(Class<? extends BaseDto> dtoClass, String fieldProperty) {
		try {
			Field field = EntityUtils.getFirstFieldInClassHierarchy(dtoClass, fieldProperty);
			if (field.isAnnotationPresent(Embedded.class)) {
				return field.getAnnotation(Embedded.class).dtoClass();
			}
		} catch (ReflectiveOperationException e) {
			throw new ResultCodeException(CoreResultCode.EXPORT_IMPORT_REFLECTION_FAILED, e);
		}
		return null;
	}

	/**
	 * Return read-write service for given DTO type.
	 * 
	 * @param serviceDtoClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ReadWriteDtoService<BaseDto, BaseFilter> getDtoService(Class<? extends BaseDto> serviceDtoClass) {
		return (ReadWriteDtoService<BaseDto, BaseFilter>) lookupService.getDtoService(serviceDtoClass);
	}

	/**
	 * Extract ZIP in attachment to the tamp directory
	 * 
	 * @param attachment
	 * @return
	 */
	private Path extractZip(IdmAttachmentDto attachment) {
		File source = Paths.get(attachmentConfiguration.getStoragePath(), attachment.getContentPath()).toFile();
		Path tempDirectory = attachmentManager.createTempDirectory(null);

		try {
			ZipUtils.extract(source, tempDirectory.toString());
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.IMPORT_ZIP_EXTRACTION_FAILED, ex);
		}
		return tempDirectory;
	}

	/**
	 * Validate import batch
	 * 
	 * @param tempDirectory
	 * @return
	 */
	private IdmExportImportDto validate(Path tempDirectory) {
		Assert.notNull(tempDirectory, "Temp directory cannot be null!");

		File manifest = Paths.get(tempDirectory.toString(), ExportManager.EXPORT_BATCH_FILE_NAME).toFile();
		if (!manifest.exists()) {
			throw new ResultCodeException(CoreResultCode.IMPORT_VALIDATION_FAILED_NO_MANIFEST,
					ImmutableMap.of("manifest", manifest.getAbsoluteFile()));
		}

		return (IdmExportImportDto) convertFileToDto(manifest, IdmExportImportDto.class, new ImportContext());

	}

	/**
	 * Convert given file (json) to the DTO
	 * 
	 * @param file
	 * @param dtoClass
	 * @param context
	 * @return
	 */
	@Override
	public BaseDto convertFileToDto(File file, Class<? extends BaseDto> dtoClass, ImportContext context) {
		try {
			String dtoAsString = new String(Files.readAllBytes(Paths.get(file.toURI())),
					AttachableEntity.DEFAULT_CHARSET);
			return this.convertStringToDto(dtoAsString, dtoClass, context);
		} catch (IOException e) {
			throw new ResultCodeException(CoreResultCode.IMPORT_CONVERT_TO_DTO_FAILED,
					ImmutableMap.of("file", file.getAbsoluteFile(), "dto", dtoClass), e);
		}
	}

	/**
	 * Convert given DTO in string to the DTO
	 * 
	 * @param dtoAsString
	 * @param dtoClass
	 * @param context
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@SuppressWarnings("unchecked")
	private <DTO> DTO convertStringToDto(String dtoAsString, Class<? extends BaseDto> dtoClass, ImportContext context)
			throws IOException {
		String replacedDtoAsString = dtoAsString;
		Map<UUID, UUID> replacedIDs = context.getReplacedIDs();

		// Find IDs to replace
		Set<UUID> idsToReplace = replacedIDs.keySet()//
				.stream()//
				.filter(oldId -> dtoAsString.contains(oldId.toString()))//
				.collect(Collectors.toSet());//

		// Replace found IDs
		for (UUID idToRplace : idsToReplace) {
			replacedDtoAsString = replacedDtoAsString.replace(idToRplace.toString(),
					replacedIDs.get(idToRplace).toString());
		}
		return (DTO) mapper.readValue(replacedDtoAsString, dtoClass);
	}

}
