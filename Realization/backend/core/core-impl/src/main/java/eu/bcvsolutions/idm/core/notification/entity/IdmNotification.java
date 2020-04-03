package eu.bcvsolutions.idm.core.notification.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.notification.domain.BaseNotification;

/**
 * Common IdM notification
 * 
 * @author Radek Tomiška
 *
 */
@Entity
@Table(name = "idm_notification", indexes = {
		@Index(name = "idx_idm_notification_sender", columnList = "identity_sender_id"),
		@Index(name = "idx_idm_notification_parent", columnList = "parent_notification_id")
		})
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class IdmNotification extends AbstractEntity implements BaseNotification {

	private static final long serialVersionUID = -2038771692205141212L;

	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "topic", length = DefaultFieldLengths.NAME)
	private String topic; // can be linked to configuration (this topic send by email, sms, etc)
	
	@Embedded
	private IdmMessage message;
	
	@ManyToOne(optional = true)
	@JoinColumn(name = "identity_sender_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@NotFound(action = NotFoundAction.IGNORE)
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmIdentity identitySender;

	@OneToMany(mappedBy = "notification", cascade = CascadeType.ALL)
	private List<IdmNotificationRecipient> recipients;

	@Column(name = "sent")
	private ZonedDateTime sent;

	@Size(max = DefaultFieldLengths.LOG)
	@Column(name = "SENT_LOG", length = DefaultFieldLengths.LOG)
	private String sentLog;
	
	@JsonIgnore
	@ManyToOne(optional = true)
	@JoinColumn(name = "parent_notification_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmNotificationLog parent;

	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public IdmIdentity getIdentitySender() {
		return identitySender;
	}
	
	public void setIdentitySender(IdmIdentity identitySender) {
		this.identitySender = identitySender;
	}

	public void setRecipients(List<IdmNotificationRecipient> recipients) {
		this.recipients = recipients;
	}
	
	public List<IdmNotificationRecipient> getRecipients() {
		if (recipients == null) {
			recipients = new ArrayList<>();
		}
		return recipients;
	}

	public String getSentLog() {
		return sentLog;
	}

	public void setSentLog(String sentLog) {
		this.sentLog = sentLog;
	}
	
	public void setMessage(IdmMessage message) {
		this.message = message;
	}
	
	public IdmMessage getMessage() {
		return message;
	}
	
	public void setSent(ZonedDateTime sent) {
		this.sent = sent;
	}
	
	public ZonedDateTime getSent() {
		return sent;
	}
	
	public void setParent(IdmNotificationLog parent) {
		this.parent = parent;
	}
	
	public IdmNotificationLog getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return MessageFormat.format("Notification [id:{2}] [message:{0}] [first recipient:{1}]", getMessage(), getRecipients().isEmpty() ? null : getRecipients().get(0), getId());
	}

}
