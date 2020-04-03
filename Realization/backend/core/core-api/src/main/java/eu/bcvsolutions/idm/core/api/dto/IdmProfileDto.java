package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Size;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Dto for identity profile - setting.
 * 
 * @author Radek Tomiška
 *
 */
@Relation(collectionRelation = "profiles")
@ApiModel(description = "Identity profile - e.g. profile image, preffered language")
public class IdmProfileDto extends AbstractDto  {

	private static final long serialVersionUID = 1L;
	//
	@Embedded(dtoClass = IdmIdentityDto.class)
	private UUID identity;
	@Embedded(dtoClass = IdmAttachmentDto.class)
	private UUID image;
	@Size(max = DefaultFieldLengths.ENUMARATION)
	@ApiModelProperty(notes = "This localization will be choosed right after log in.")
	private String preferredLanguage;
	@ApiModelProperty(notes = "Side menu will be collapsed, icons will be shown only.")
	private boolean navigationCollapsed;
	@ApiModelProperty(notes = "Show internal entity identifiers, user transactions, logs and other system information.")
	private boolean systemInformation;
	@ApiModelProperty(notes = "Tables will show given count of records by default, default application setting will be used otherwise.")
	private Integer defaultPageSize;

	public IdmProfileDto() {
	}
	
	public IdmProfileDto(UUID id) {
		super(id);
	}

	public UUID getIdentity() {
		return identity;
	}

	public void setIdentity(UUID identity) {
		this.identity = identity;
	}

	public UUID getImage() {
		return image;
	}

	public void setImage(UUID image) {
		this.image = image;
	}

	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}
	
	public boolean isNavigationCollapsed() {
		return navigationCollapsed;
	}
	
	public void setNavigationCollapsed(boolean navigationCollapsed) {
		this.navigationCollapsed = navigationCollapsed;
	}
	
	/**
	 * Show internal system information like identifiers, detail logs etc.
	 * 
	 * @return if internal system information will be shown
	 * @since 10.2.0 
	 */
	public boolean isSystemInformation() {
		return systemInformation;
	}
	
	/**
	 * Show internal system information like identifiers, detail logs etc.
	 * 
	 * @param systemInformation if internal system information will be shown
	 * @since 10.2.0
	 */
	public void setSystemInformation(boolean systemInformation) {
		this.systemInformation = systemInformation;
	}
	
	/**
	 * Default page size used in tables.
	 * 
	 * @return default page size
	 * @since 10.2.0
	 */
	public Integer getDefaultPageSize() {
		return defaultPageSize;
	}
	
	/**
	 * Default page size used in tables.
	 * 
	 * @param defaultPageSize default page size
	 * @since 10.2.0
	 */
	public void setDefaultPageSize(Integer defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}
}
