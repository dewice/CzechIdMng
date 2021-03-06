package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;

/**
 * Core event - defines order only for now
 * <p>
 * Its better to use {@link Ordered} interface instead {@link Order} annotation - does not work with aspects. 
 * 
 * @param <E> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 */
public class CoreEvent<E extends Serializable> extends AbstractEntityEvent<E> {

	private static final long serialVersionUID = 8862117134483307569L;
	public static final int DEFAULT_ORDER = 0;
	
	public enum CoreEventType implements EventType {
		CREATE, // new entity is created
		UPDATE, // entity is updated
		DELETE, // entity is removed
		EAV_SAVE, // some entity extended attribute is modified (CUD)
		NOTIFY, // notify about entity is changed (CU) (asynchronous)
	}
	
	public CoreEvent(EventType type, E content) {
		super(type, content);
	}
	
	public CoreEvent(EventType type, E content, Map<String, Serializable> properties) {
		super(type, content, properties);
	}
	
	public CoreEvent(EventType type, E content, Map<String, Serializable> properties, EventContext<E> context) {
		super(type, content, properties, context);
	}
	
	public CoreEvent(EventType type, E content, Map<String, Serializable> properties, EventContext<E> context, Class<? extends E> eventClassType) {
		super(type, content, properties, context, eventClassType);
	}
}
