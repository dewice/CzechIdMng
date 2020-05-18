package eu.bcvsolutions.idm.core.model.entity;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.ValidableEntity;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import java.time.LocalDate;
import org.hibernate.envers.Audited;

/**
 * Definition of a delegation entity.
 *
 * @author Vít Švanda
 *
 */
@Entity
@Table(name = "idm_delegation_def", indexes = {
	@Index(name = "idx_i_del_def_s_identity_id", columnList = "source_identity_id"),
	@Index(name = "idx_i_del_def_t_identity_id", columnList = "target_identity_id"),
	@Index(name = "idx_i_del_def_s_contract_id", columnList = "source_contract_id"),
	@Index(name = "idx_i_del_def_t_contract_id", columnList = "target_contract_id"),
	@Index(name = "idx_i_del_def_valid_from", columnList = "valid_from"),
	@Index(name = "idx_i_del_def_valid_till", columnList = "valid_till"),
	@Index(name = "idx_i_del_def_type", columnList = "type")})
public class IdmDelegationDefinition extends AbstractEntity implements ValidableEntity {

	private static final long serialVersionUID = 1L;

	@Audited
	@SuppressWarnings("deprecation")
	@ManyToOne(fetch = FetchType.LAZY)
	@NotNull
	@JoinColumn(name = "source_identity_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT), nullable = false)
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmIdentity sourceIdentity;

	@Audited
	@SuppressWarnings("deprecation")
	@ManyToOne(fetch = FetchType.LAZY)
	@NotNull
	@JoinColumn(name = "target_identity_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT), nullable = false)
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmIdentity targetIdentity;

	@Audited
	@SuppressWarnings("deprecation")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_contract_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT), nullable = true)
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmIdentityContract sourceContract;

	@Audited
	@SuppressWarnings("deprecation")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_contract_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT), nullable = true)
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmIdentityContract targetContract;

	@Audited
	@NotNull
	@Column(name = "type", nullable = false, length = 255)
	private String type;

	@Audited
	@Column(name = "valid_from")
	private LocalDate validFrom;

	@Audited
	@Column(name = "valid_till")
	private LocalDate validTill;

	public IdmIdentity getSourceIdentity() {
		return sourceIdentity;
	}

	public void setSourceIdentity(IdmIdentity sourceIdentity) {
		this.sourceIdentity = sourceIdentity;
	}

	public IdmIdentity getTargetIdentity() {
		return targetIdentity;
	}

	public void setTargetIdentity(IdmIdentity targetIdentity) {
		this.targetIdentity = targetIdentity;
	}

	public IdmIdentityContract getSourceContract() {
		return sourceContract;
	}

	public void setSourceContract(IdmIdentityContract sourceContract) {
		this.sourceContract = sourceContract;
	}

	public IdmIdentityContract getTargetContract() {
		return targetContract;
	}

	public void setTargetContract(IdmIdentityContract targetContract) {
		this.targetContract = targetContract;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	@Override
	public LocalDate getValidTill() {
		return validTill;
	}

	public void setValidTill(LocalDate validTill) {
		this.validTill = validTill;
	}

}
