package eu.bcvsolutions.idm.core.notification.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import eu.bcvsolutions.idm.core.model.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.notification.service.NotificationLogService;

@Entity
@Table(name = "idm_notification_log")
public class IdmNotificationLog extends IdmNotification {

	private static final long serialVersionUID = -3022005218384947356L;
	
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "topic", length = DefaultFieldLengths.NAME)
	private String topic; // can be linked to configuration (this topic send by email, sms, etc)
	
	@JsonProperty(access = Access.READ_ONLY)
	@OneToMany(mappedBy = "parent")
	private List<IdmNotification> relatedNotifications;
	
	@Override
	public String getType() {
		return NotificationLogService.NOTIFICATION_TYPE;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public List<IdmNotification> getRelatedNotifications() {
		if (relatedNotifications == null) {
			relatedNotifications = new ArrayList<>();
		}
		return relatedNotifications;
	}
	
	public void setRelatedNotifications(List<IdmNotification> relatedNotifications) {
		this.relatedNotifications = relatedNotifications;
	}
	
	@Override
	public String toString() {
		return MessageFormat.format("Notification message [{0}] with topic [{1}] to recipient [first:{2}]", getMessage(), topic, getRecipients().isEmpty() ? null : getRecipients().get(0));
	}
}
