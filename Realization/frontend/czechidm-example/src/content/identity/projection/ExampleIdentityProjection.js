import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import { Basic, Advanced, Utils, Managers, Content } from 'czechidm-core';

const identityManager = new Managers.IdentityManager();
const identityProjectionManager = new Managers.IdentityProjectionManager();
const formProjectionManager = new Managers.FormProjectionManager();


/**
 * Example  form for identity projection.
 *
 * @author Radek Tomiška
 * @since 10.2.0
 */
class ExampleIdentityProjection extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
    };
  }

  getContentKey() {
    return 'content.identity.projection';
  }

  getNavigationKey() {
    return 'identities';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.match.params;
    const { identityProjection, location } = this.props;
    const isNew = !!Utils.Ui.getUrlParameter(location, 'new');
    //
    if (isNew) {
      let formProjectionId;
      if (identityProjection && identityProjection.identity && identityProjection.identity.formProjection) {
        formProjectionId = identityProjection.identity.formProjection;
      } else {
        formProjectionId = Utils.Ui.getUrlParameter(this.props.location, 'projection');
      }
      if (!formProjectionId) {
        // form projection not found - default will be shown
        this._initProjection(entityId, identityProjection, {});
      } else {
        // fetch projection definition
        this.context.store.dispatch(formProjectionManager.autocompleteEntityIfNeeded(formProjectionId, null, (entity, error) => {
          if (error) {
            this.addError(error);
          } else {
            this._initProjection(entityId, identityProjection, entity);
          }
        }));
      }
    } else {
      this.getLogger().debug(`[FormDetail] loading entity detail [id:${ entityId }]`);
      this.context.store.dispatch(identityProjectionManager.fetchProjection(entityId, null, (entity, error) => {
        if (error) {
          this.addError(error);
        } else {
          this._initProjection(entityId, entity);
        }
      }));
    }
  }

  _initProjection(entityId, identityProjection = null, formProjection = null) {
    //
    // prepare form friendly projection
    let _identityProjection = null;
    if (identityProjection) {
      if (!formProjection && identityProjection.identity && identityProjection.identity._embedded) {
        formProjection = identityProjection.identity._embedded.formProjection;
      }
      //
      _identityProjection = {
        ...identityProjection.identity
      };
    } else {
      // new projection
      _identityProjection = {
        id: entityId,
        identity: {
          id: entityId,
          formProjection: formProjection ? formProjection.id : Utils.Ui.getUrlParameter(this.props.location, 'projection')
        }
      };
    }
    //
    this.setState({
      identityProjection: _identityProjection,
      formProjection
    }, () => {
      if (this.refs.username) {
        this.refs.username.focus();
      }
    });
  }

  save(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const { identityProjection } = this.state;
    //
    this.refs.form.processStarted();
    const data = this.refs.form.getData();
    // construct projection
    const _identityProjection = {
      ...identityProjection,
      id: data.id,
      identity: data
    };
    //
    // post => save
    this.context.store.dispatch(identityProjectionManager.saveProjection(_identityProjection, null, this._afterSave.bind(this)));
  }

  /**
  * Just set showloading to false and set processEnded to form.
  * Call after save/create
  */
  _afterSave(identityProjection, error) {
    if (error) {
      this.setState({
        validationError: error,
        validationDefinition: false
      }, () => {
        this.addError(error);
        if (this.refs.form) {
          this.refs.form.processEnded();
        }
      });
    } else {
      this.addMessage({
        message: this.i18n('action.save.success', { record: identityProjection.identity.username, count: 1 })
      });
      this.context.history.replace(identityManager.getDetailLink(identityProjection.identity));
      if (this.refs.form) {
        this.refs.form.processEnded();
      }
    }
  }

  render() {
    const {
      match,
      location,
      showLoading,
      _imageUrl
    } = this.props;
    const { entityId } = match.params;
    const {
      identityProjection,
      formProjection
    } = this.state;
    const isNew = !!Utils.Ui.getUrlParameter(location, 'new');
    //
    return (
      <Basic.Div>
        <Helmet title={ isNew ? this.i18n('create.title') : this.i18n('edit.title') } />
        <Basic.Row>
          <Basic.Div className="col-lg-offset-2 col-lg-8">
            <Advanced.DetailHeader
              entity={ isNew ? null : identityProjection }
              back="/identities"
              buttons={[
                <Basic.Icon
                  value="fa:angle-double-right"
                  style={{ marginRight: 5, cursor: 'pointer' }}
                  title={ this.i18n('component.advanced.IdentityInfo.link.detail.default.label') }
                  onClick={ () => this.context.history.push(`/identity/${ encodeURIComponent(identityProjection.username) }/profile`) }
                  rendered={ !isNew }/>
              ]}>
              {
                _imageUrl
                ?
                <img src={ _imageUrl } alt="profile" className="img-circle img-thumbnail" style={{ height: 40, padding: 0 }} />
                :
                <Basic.Icon
                  icon={ formProjection ? formProjectionManager.getLocalization(formProjection, 'icon', 'component:identity') : 'component:identity' }
                  identity={ identityProjection }/>
              }
              { ' ' }
              { identityProjectionManager.getNiceLabel(identityProjection) }
              <small>
                { ' ' }
                { isNew ? this.i18n('create.title') : this.i18n('edit.title') }
              </small>
            </Advanced.DetailHeader>

            <Content.OrganizationPosition identity={ entityId } rendered={ !isNew }/>

            <form onSubmit={ this.save.bind(this) }>

              <Basic.Panel rendered={ identityProjection === null || identityProjection === undefined }>
                <Basic.Loading isStatic show/>
              </Basic.Panel>

              <Basic.Panel className={ identityProjection === null || identityProjection === undefined ? 'hidden' : 'last' }>

                <Basic.PanelBody>
                  <Basic.AbstractForm
                    ref="form"
                    data={ identityProjection }
                    readOnly={ !identityProjectionManager.canSave(isNew ? null : identityProjection) }
                    style={{ padding: 0 }}>

                    <Basic.TextField
                      ref="username"
                      label={ this.i18n('identity.username.label') }
                      max={ 255 }/>

                  </Basic.AbstractForm>
                </Basic.PanelBody>
                <Basic.PanelFooter>
                  <Basic.Button type="button" level="link" onClick={ this.context.history.goBack }>
                    { this.i18n('button.back') }
                  </Basic.Button>
                  <Basic.Button
                    type="submit"
                    level="success"
                    showLoading={ showLoading }
                    showLoadingIcon
                    showLoadingText={ this.i18n('button.saving') }
                    rendered={ identityProjectionManager.canSave(isNew ? null : identityProjection) }>
                    { this.i18n('button.save') }
                  </Basic.Button>
                </Basic.PanelFooter>
              </Basic.Panel>
            </form>
          </Basic.Div>
        </Basic.Row>
      </Basic.Div>
    );
  }
}

ExampleIdentityProjection.propTypes = {
  identityProjection: PropTypes.object
};
ExampleIdentityProjection.defaultProps = {
  identityProjection: null,
  _imageUrl: null
};

function select(state, component) {
  const { entityId } = component.match.params;
  const profileUiKey = identityManager.resolveProfileUiKey(entityId);
  const profile = Managers.DataManager.getData(state, profileUiKey);
  const identityProjection = identityProjectionManager.getEntity(state, entityId);
  //
  return {
    identityProjection,
    showLoading: identityProjectionManager.isShowLoading(state, null, !identityProjection ? entityId : identityProjection.id),
    _imageUrl: profile ? profile.imageUrl : null
  };
}

export default connect(select)(ExampleIdentityProjection);
