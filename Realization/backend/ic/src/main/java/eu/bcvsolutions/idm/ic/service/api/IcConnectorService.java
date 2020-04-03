package eu.bcvsolutions.idm.ic.service.api;

import java.util.List;

import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.api.IcConnector;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorInstance;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.api.IcSyncResultsHandler;
import eu.bcvsolutions.idm.ic.api.IcSyncToken;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;
import eu.bcvsolutions.idm.ic.filter.api.IcFilter;
import eu.bcvsolutions.idm.ic.filter.api.IcResultsHandler;

public interface IcConnectorService {

	/**
	 * Return key defined IC implementation
	 * 
	 * @return
	 */
	String getImplementationType();

	/**
	 * Create new object in resource
	 * @param connectorInstance - Identification of connector
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param attributes - Attributes for new object
	 * @return Uid of created object
	 */
	IcUidAttribute createObject(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration,
			IcObjectClass objectClass, List<IcAttribute> attributes);

	
	/**
	 * Replace attributes in exist object in resource
	 * @param connectorInstance - Identification of connector
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param uid - Identification of object in resource
	 * @param replaceAttributes - Attributes to replace in resource object
	 * @return Uid of account after update
	 */
	IcUidAttribute updateObject(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration,
			IcObjectClass objectClass, IcUidAttribute uid, List<IcAttribute> replaceAttributes);
	
	/**
	 * Delete object with same uid from resource
	 * @param connectorInstance - Identification of connector
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param uid - Identification of object in resource
	 */
	void deleteObject(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration,
			IcObjectClass objectClass, IcUidAttribute uid);
	
	/**
	 * Read object with same uid from resource
	 * @param connectorInstance - Identification of connector
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param uid - Identification of object in resource
	 * @return Found {@link IcConnectorObject} or null if none was found
	 */
	IcConnectorObject readObject(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration,
			IcObjectClass objectClass, IcUidAttribute uid);

	/**
	 * Authenticate user
	 * @param connectorInstance - Identification of connector
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param username
	 * @param password
	 * @return
	 */
	IcUidAttribute authenticateObject(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration,
			IcObjectClass objectClass, String username, GuardedString password);


	/**
	 * Do synchronization. For every changed item will be call given handler.
	 * @param connectorInstance - Connector used to perform the operation
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param token - Synchronization token
	 * @param handler - Handler will be call for every synchronization item
	 * @return token
	 */
	IcSyncToken synchronization(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration, IcObjectClass objectClass,
			IcSyncToken token, IcSyncResultsHandler handler);


	/**
	 * Search by given filter. For every searched item will be call given handler.
	 * @param connectorInstance - Connector used to perform the operation
	 * @param connectorConfiguration - Connector configuration
	 * @param objectClass - Type or category of connector object
	 * @param filter - Filter which will be used to search objects
	 * @param handler - Handler will be call for every searched item
	 */
	void search(IcConnectorInstance connectorInstance, IcConnectorConfiguration connectorConfiguration, IcObjectClass objectClass,
			IcFilter filter, IcResultsHandler handler);

	
	/**
	 * Get connector instance (cached)
	 * @param connectorInstance
	 * @param connectorConfiguration
	 * @return
	 */
	IcConnector getConnectorInstance(IcConnectorInstance connectorInstance,
			IcConnectorConfiguration connectorConfiguration);
	
}