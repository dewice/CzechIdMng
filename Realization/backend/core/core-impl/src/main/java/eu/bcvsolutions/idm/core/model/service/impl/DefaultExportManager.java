package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;

/**
 * Export manager
 * 
 * @author Vít Švanda
 *
 */
@Service("exportManager")
public class DefaultExportManager implements ExportManager {
	
	@Autowired
	@Lazy
	private AttachmentManager attachmentManager;
	@Autowired
	private ObjectMapper mapper;
	
	@Override
	public IdmExportImportDto exportDto(BaseDto dto, IdmExportImportDto batch) {
		Assert.notNull(dto, "DTO cannot be null!");
		Assert.notNull(batch, "Batch cannot be null!");
		Assert.notNull(batch.getId(), "Batch ID cannot be null!");
		
		Path tempDirectory = batch.getTempDirectory();
		
		if (tempDirectory == null) {
			batch.setTempDirectory(attachmentManager.createTempDirectory(batch.getId().toString()));
			tempDirectory = batch.getTempDirectory();
		}
		
		Path dtoTypePath = this.createDtoDirectory(dto.getClass(), batch);
		Path dtoPath = null;
		try {
			dtoPath = Paths.get(dtoTypePath.toString(),
					MessageFormat.format("{0}.{1}", dto.getId().toString(), EXTENSION_JSON));
			if (Files.exists(dtoPath)) {
				return batch;
			}
			dtoPath = Files.createFile(dtoPath);
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_CREATE_TEMP_FILE_FAILED, ImmutableMap.of(
					"file", MessageFormat.format("{0}.{1}", dto.getId().toString(), EXTENSION_JSON),
					"folder", tempDirectory.toString())
					, ex);
		}
		
		try (FileOutputStream outputStream = new FileOutputStream(dtoPath.toFile())) {
	        // write into json stream
			JsonGenerator jGenerator = mapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);
			try {
				jGenerator.writeObject(dto);		
			} finally {
				// close json stream
				jGenerator.close();
			}
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.EXPORT_GENERATE_JSON_FAILED, ImmutableMap.of(
					"dto", dto.toString())
					, ex);
		}
		
		return batch;
	}
	
	@Override
	public Path createDtoDirectory(Class<? extends BaseDto> dtoClass, IdmExportImportDto batch) {
		Assert.notNull(dtoClass, "DTO class cannot be null!");
		Assert.notNull(batch, "Batch cannot be null!");
		Assert.notNull(batch.getId(), "Batch ID cannot be null!");
		
		Path tempDirectory = batch.getTempDirectory();
		
		if (tempDirectory == null) {
			batch.setTempDirectory(attachmentManager.createTempDirectory(batch.getId().toString()));
			tempDirectory = batch.getTempDirectory();
		}
		
		Path dtoTypePath = Paths.get(tempDirectory.toString(), dtoClass.getSimpleName());
		try {
			if (Files.notExists(dtoTypePath)) {
				return  Files.createDirectory(dtoTypePath);
			}
			return dtoTypePath;
		} catch (IOException ex) {
			throw new ResultCodeException(CoreResultCode.ATTACHMENT_CREATE_TEMP_FILE_FAILED, ImmutableMap.of(
					"extension", EXTENSION_JSON,
					"temp", tempDirectory.toString())
					, ex);
		}
	}
	
	@Override
	public void setAuthoritativeMode(String parentField, String superParentFilterProperty,
			Class<? extends BaseDto> dtoClass, IdmExportImportDto batch) {
		this.setAuthoritativeMode(Sets.newHashSet(parentField), superParentFilterProperty, dtoClass, batch);
	}
	
	@Override
	public void setAuthoritativeMode(Set<String> parentFields, String superParentFilterProperty,
			Class<? extends BaseDto> dtoClass, IdmExportImportDto batch) {
		ExportDescriptorDto descriptorDto = this.getDescriptor(batch, dtoClass);
		if (descriptorDto != null) {
			descriptorDto.getParentFields().addAll(parentFields);
			descriptorDto.setSupportsAuthoritativeMode(true);
			descriptorDto.setSuperParentFilterProperty(superParentFilterProperty);
		}
	}

	@Override
	public ExportDescriptorDto getDescriptor(IdmExportImportDto batch, Class<? extends BaseDto> dtoClass) {
		return batch.getExportOrder().stream()//
				.filter(descriptor -> descriptor.getDtoClass().equals(dtoClass))//
				.findFirst()//
				.orElse(null);//
	}
	
}
