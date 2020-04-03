import _ from 'lodash';
//
import FormableEntityManager from './FormableEntityManager';
import SecurityManager, { RECEIVE_PROFILE } from '../security/SecurityManager';
import { IdentityService } from '../../services';
import DataManager from './DataManager';
import ProfileManager from './ProfileManager';
import IncompatibleRoleManager from './IncompatibleRoleManager';
import * as Utils from '../../utils';
import { RECEIVE_PERMISSIONS } from './EntityManager';

/**
 * Manager for identities fetching
 *
 * @author Radek Tomiška
 */
export default class IdentityManager extends FormableEntityManager {

  constructor() {
    super();
    this.identityService = new IdentityService();
    this.dataManager = new DataManager();
    this.profileManager = new ProfileManager();
    this.incompatibleRoleManager = new IncompatibleRoleManager();
  }

  getService() {
    return this.identityService;
  }

  getEntityType() {
    return 'Identity'; // TODO: constant or enumeration
  }

  getCollectionType() {
    return 'identities';
  }

  /**
   * Return resource identifier on FE (see BE - IdentifiableByName)
   *
   * @return {string} secondary identifier (unique property)
   */
  getIdentifierAlias() {
    return 'username';
  }

  getFullName(identity) {
    return this.getService().getFullName(identity);
  }

  /**
   * Sets user activity
   *
   * @param {array[string]} usernames selected usernames
   * @param {string} bulkActionName activate|deactivate
   */
  setUsersActivity(entities, bulkActionName) {
    return (dispatch) => {
      dispatch(
        this.startBulkAction(
          {
            name: bulkActionName,
            title: this.i18n(`content.identities.action.${bulkActionName}.header`, { count: entities.length })
          },
          entities.length
        )
      );
      const successEntities = [];
      entities.reduce((sequence, identity) => {
        return sequence.then(() => {
          if (bulkActionName === 'activate') {
            return this.getService().activate(identity.id);
          }
          return this.getService().deactivate(identity.id);
        }).then(json => {
          dispatch(this.updateBulkAction());
          successEntities.push(identity);
          // new entity to redux trimmed store
          json._trimmed = true;
          dispatch(this.receiveEntity(identity.id, json));
        }).catch(error => {
          dispatch(
            this.flashMessagesManager.addErrorMessage({
              title: this.i18n(`content.identities.action.${ bulkActionName }.error`, { username: this.getNiceLabel(identity) })
            },
            error)
          );
          throw error;
        });
      }, Promise.resolve())
        .catch(() => {
          // nothing - message is propagated before
          // catch is before then - we want execute nex then clausule
        })
        .then(() => {
          if (successEntities.length > 0) {
            dispatch(this.flashMessagesManager.addMessage({
              level: successEntities.length === entities.length ? 'success' : 'info',
              message: this.i18n(`content.identities.action.${bulkActionName}.success`, { usernames: this.getNiceLabels(successEntities).join(', ') })
            }));
          }
          dispatch(this.stopBulkAction());
        });
    };
  }

  /**
   * Load username authorities from BE
   *
   * @param  {string} username
   * @param  {string} uiKey
   * @return {array[object]}
   */
  fetchAuthorities(username, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getAuthorities(username)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Load username incompatible roles (by assigned roles) from BE
   *
   * @param  {string} username
   * @param  {string} uiKey
   * @return {array[object]}
   */
  fetchIncompatibleRoles(username, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getIncompatibleRoles(username)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json._embedded[this.incompatibleRoleManager.getCollectionType()]));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Get given identity's main position in organization
   *
   * @param  {string} username
   * @param  {string} uiKey
   * @return {array[object]}
   */
  fetchWorkPosition(username, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getWorkPosition(username)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Return true when currently logged user can change password
   *
   * @param  {string} passwordChangeType see consts bottom
   * @param  {arrayOf(string)} permissions logged identity's base permissions on selected identity
   * @return {bool}
   */
  canChangePassword(passwordChangeType, permissions) {
    if (SecurityManager.isAdmin()) {
      // admin - highest priority
      return true;
    }
    if (!passwordChangeType || passwordChangeType === IdentityManager.PASSWORD_DISABLED) {
      // password cannot be changed by environment configuration
      return false;
    }
    return Utils.Permission.hasPermission(permissions, 'PASSWORDCHANGE');
  }

  /**
   * Profile ui key
   *
   * @param  {[type]} identityId [description]
   * @return {[type]}            [description]
   */
  resolveProfileUiKey(identityId) {
    return `${IdentityManager.UIKEY_PROFILE}${identityId}`;
  }

  /**
   * Save logged identity profile metadata only
   *
   * @param  {string} identityId logged identity
   * @param  {Profile} profile metadata
   * @return {action}
   */
  saveCurrentProfile(identityId, profile) {
    return (dispatch) => {
      this.getService().patchProfile(identityId, profile)
        .then((entity) => {
          // lookout SecurityManager is not used (navigation or loacalization is already refreshed - profile is saved only)
          dispatch({
            type: RECEIVE_PROFILE,
            profile: entity
          });
        })
        .catch(error => {
          // log error only into message history (e.g. 403 - authorization policies are wrong configured)
          // saving profile is optional - logged identity couldn't have permission for read profile (or profile not found)
          this.flashMessagesManager.addErrorMessage({ hidden: true, level: 'info' }, error);
        });
    };
  }

  /**
   * Fetch identity Profile
   * @param  {[type]} identityId [description]
   * @return {[type]}            [description]
   */
  fetchProfile(identityId) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch, getState) => {
      dispatch(this.dataManager.requestData(uiKey));
      this
        .getService()
        .getProfile(identityId)
        .then(json => {
          let profile = json || {}; // profile is not saved yet
          //
          dispatch(this.profileManager.queueFetchPermissions(profile.id, null, () => {
            dispatch(this.profileManager.receiveEntity(profile.id, json, null));
            const previousProfile = DataManager.getData(getState(), uiKey);
            if (previousProfile) {
              profile = { ...previousProfile, ...profile }; // prevent to clear loaded profile image
            }
            dispatch(this.dataManager.receiveData(uiKey, profile)); // profile for identity
          }));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Upload image to BE
   */
  uploadProfileImage(identityId, formData, cb) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().uploadProfileImage(identityId, formData)
        .then((profile) => {
          profile.imageUrl = null; // reload image will occurs after
          dispatch(this.dataManager.receiveData(uiKey, profile, cb)); // enforce reload
          dispatch(this.downloadProfileImage(identityId));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  downloadProfileImage(identityId) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch, getState) => {
      const profile = DataManager.getData(getState(), uiKey);
      if (profile && (profile.imageUrl || profile.imageUrl === false)) {
        // profile already loaded or image not found before (imageUrl === false)
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this
          .getService()
          .downloadProfileImage(identityId)
          .then(response => {
            if (response.status === 404 || response.status === 204) {
              return null;
            }
            if (response.status === 200) {
              return response.blob();
            }
            const json = response.json();
            if (Utils.Response.hasError(json)) {
              throw Utils.Response.getFirstError(json);
            }
            if (Utils.Response.hasInfo(json)) {
              throw Utils.Response.getFirstInfo(json);
            }
            //
            return null;
          })
          .then(blob => {
            let imageUrl = false;
            if (blob) {
              imageUrl = URL.createObjectURL(blob);
            }
            //
            const previousProfile = DataManager.getData(getState(), uiKey);
            let _profile = { imageUrl };
            if (previousProfile) {
              _profile = { ...previousProfile, ..._profile }; // prevent to clear loaded profile image
            }
            dispatch(this.dataManager.receiveData(uiKey, _profile));
          })
          .catch(error => {
            dispatch(this.receiveError(null, uiKey, error));
          });
      }
    };
  }

  deleteProfileImage(identityId) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().deleteProfileImage(identityId)
        .then((profile) => {
          profile.imageUrl = false;
          dispatch(this.dataManager.receiveData(uiKey, profile));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Fetch profile permissions
   *
   * Lookout: permissions will be stored under identity identifier in reducer.
   */
  fetchProfilePermissions(id) {
    return (dispatch, getState) => {
      if (getState().security.userContext.isExpired) {
        return;
      }
      //
      this.getService().getProfilePermissions(id)
        .then(permissions => {
          const profileKey = this.profileManager.resolveUiKey(null, id);
          //
          dispatch({
            type: RECEIVE_PERMISSIONS,
            id,
            entityType: this.profileManager.getEntityType(),
            permissions,
            uiKey: profileKey
          });
        });
    };
  }

  /**
   * PreValidates password
   *
   * @param  {string} requestData
   * @return
   */
  preValidate(requestData) {
    return this.identityService.preValidate(requestData);
  }

  /**
   * Load password by identity id from server
   *
   * @param  {string|number} identityId - Identity identifier
   * @param  {string} uiKey = null - ui key for loading indicator etc
   * @return {object} - action
   * @
   */
  fetchPassword(identityId, uiKey = null) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getPassword(identityId)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Detail link by configured projection or default.
   *
   * @param  {object} identity
   * @return {string} url
   * @since 10.2.0
   */
  getDetailLink(identity) {
    if (!identity) {
      return null;
    }
    //
    if (identity._embedded && identity._embedded.formProjection) {
      const route = Utils.Ui.getRouteUrl(identity._embedded.formProjection.route);
      //
      return `${ route }/${ encodeURIComponent(identity.username) }`;
    }
    // default
    return `/identity/${ encodeURIComponent(identity.username) }/profile`;
  }
}

IdentityManager.PASSWORD_DISABLED = 'DISABLED';
IdentityManager.PASSWORD_ALL_ONLY = 'ALL_ONLY';
IdentityManager.PASSWORD_CUSTOM = 'CUSTOM';
IdentityManager.UIKEY_PROFILE = 'identity-profile-';
