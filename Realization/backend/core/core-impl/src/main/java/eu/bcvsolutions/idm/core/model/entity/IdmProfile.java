package eu.bcvsolutions.idm.core.model.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.entity.IdmAttachment;

/**
 * Profile
 * 
 * @author Radek Tomiška 
 * @since 9.0.0
 */
@Entity
@Table(name = "idm_profile")
public class IdmProfile extends AbstractEntity implements AttachableEntity {
	
	private static final long serialVersionUID = 1L;
	//
	@Audited
	@OneToOne(optional = false)
	@JoinColumn(name = "identity_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmIdentity identity;
	
	/**
	 * Attachment with the image
	 */
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(optional = true)
	@JoinColumn(name = "image_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmAttachment image;
	
	@Audited
	@Size(max = DefaultFieldLengths.ENUMARATION)
	@Column(name = "preferred_language", length = DefaultFieldLengths.ENUMARATION)
	private String preferredLanguage;
	
	@Audited
	@Column(name = "navigation_collapsed", nullable = false)
	private boolean navigationCollapsed;
	
	@Audited
	@Column(name = "system_information", nullable = false)
	private boolean systemInformation;
	
	@Audited
	@Column(name = "default_page_size", nullable = true)
	private Integer defaultPageSize;

	public IdmProfile() {
	}
	
	public IdmProfile(UUID id) {
		super(id);
	}

	public IdmIdentity getIdentity() {
		return identity;
	}

	public void setIdentity(IdmIdentity identity) {
		this.identity = identity;
	}

	public IdmAttachment getImage() {
		return image;
	}

	public void setImage(IdmAttachment image) {
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
