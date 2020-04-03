import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { SearchParameters } from '../../domain';
import {
  SecurityManager,
  ConfigurationManager,
  IdentityManager,
  FormProjectionManager,
  IdentityProjectionManager
} from '../../redux';
import IdentityStateEnum from '../../enums/IdentityStateEnum';
import ConfigLoader from '../../utils/ConfigLoader';
//
const manager = new IdentityManager(); // default manager
const projectionManager = new FormProjectionManager();
const identityProjectionManager = new IdentityProjectionManager();

/**
 * Table of identities
 *
 * @author Radek Tomiška
 */
export class IdentityTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened,
      showAddModal: false
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    if (this.refs.text) {
      this.refs.text.focus();
    }
  }

  getContentKey() {
    return 'content.identities';
  }

  getManager() {
    return this.props.identityManager;
  }

  setTreeNodeId(treeNodeId, cb) {
    if (this.refs.treeNodeId) {
      this.refs.treeNodeId.setValue(treeNodeId, cb);
    }
  }

  /**
   * Filter identities by given tree node id
   *
   * @param  {string} treeNodeId
   */
  filterByTreeNodeId(treeNodeId) {
    this.setTreeNodeId(treeNodeId, () => {
      this.useFilter();
    });
  }

  /**
  * Redirect to user form
  */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    //
    const { skipDashboard, isDefaultFormProjection } = this.props;
    const ctrlKey = !event || event.ctrlKey;
    //
    if (Utils.Entity.isNew(entity)) {
      const searchParameters = new SearchParameters()
        .setName(SearchParameters.NAME_AUTOCOMPLETE)
        .setFilter('ownerType', 'eu.bcvsolutions.idm.core.model.entity.IdmIdentity')
        .setFilter('disabled', 'false')
        .setSort('code', 'asc')
        .setSize(1000); // I don't believe more projections will be defined ... :-)
      this.context.store.dispatch(projectionManager.fetchEntities(searchParameters, null, (json, error) => {
        let projections = [];
        if (error && error.statusCode !== 403) {
          this.addError(error);
          return;
        }
        if (!error) { // 403 is ignored => no projection
          projections = json._embedded[projectionManager.getCollectionType()];
        }
        //
        if (!projections || projections.length === 0) {
          const newIdentity = {
            id: uuid.v1(),
            username: this.refs.text.getValue()
          };
          this.context.store.dispatch(this.getManager().receiveEntity(newIdentity.id, newIdentity));
          this.context.history.push(`/identity/new?id=${ newIdentity.id }`);
        } else if (!isDefaultFormProjection && projections.length === 1) {
          const newIdentity = {
            id: uuid.v1(),
            username: this.refs.text.getValue(),
            formProjection: projections[0].id
          };
          this.context.store.dispatch(identityProjectionManager.receiveEntity(newIdentity.id, {
            id: newIdentity.id,
            identity: newIdentity
          }));
          const route = Utils.Ui.getRouteUrl(projections[0].route);
          this.context.history.push(`${ route }/${ newIdentity.id }?new=1&projection=${ encodeURIComponent(projections[0].id) }`);
        } else {
          this.setState({
            projections,
            showAddModal: true
          });
        }

      }));
    } else if (!skipDashboard && !ctrlKey) {
      // dashboard
      this.context.history.push(`/identity/${ encodeURIComponent(entity.username) }/dashboard`);
    } else {
      // detail by projection
      this.context.history.push(manager.getDetailLink(entity));
    }
  }

  getDefaultSearchParameters() {
    let searchParameters = this.getManager().getDefaultSearchParameters();
    //
    searchParameters = searchParameters.setFilter('disabled', ConfigLoader.getConfig('identity.table.filter.disabled', false));
    searchParameters = searchParameters.setFilter('recursively', ConfigLoader.getConfig('identity.table.filter.recursively', true));
    //
    return searchParameters;
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      text: null,
      treeNodeId: null
    }, () => {
      this.refs.table.cancelFilter(this.refs.filterForm);
    });
  }

  closeAddModal() {
    this.setState({
      showAddModal: false
    });
  }

  render() {
    const {
      uiKey,
      identityManager,
      columns,
      forceSearchParameters,
      showAddButton,
      showDetailButton,
      showFilter,
      showRowSelection,
      rendered,
      treeType,
      className,
      prohibitedActions,
      showAddLoading,
      isDefaultFormProjection
    } = this.props;
    const { filterOpened, projections, showAddModal } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    let _forceSearchParameters = forceSearchParameters || new SearchParameters();
    let forceTreeNodeSearchParams = new SearchParameters();
    if (treeType) {
      forceTreeNodeSearchParams = forceTreeNodeSearchParams.setFilter('treeTypeId', treeType.id);
      _forceSearchParameters = _forceSearchParameters.setFilter('treeTypeId', treeType.id);
    }
    //
    const roleDisabled = _forceSearchParameters.getFilters().has('role');
    const treeNodeDisabled = _forceSearchParameters.getFilters().has('treeNodeId');
    //
    return (
      <Basic.Div>
        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          prohibitedActions={ prohibitedActions }
          manager={ identityManager }
          showRowSelection={ showRowSelection }
          filter={
            <Advanced.Filter onSubmit={ this.useFilter.bind(this) }>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={ this.i18n('filter.name.placeholder') }
                      help={ Advanced.Filter.getTextHelp() }/>
                  </Basic.Col>
                  <Basic.Col lg={ 3 }>
                    <Advanced.Filter.RoleSelect
                      ref="role"
                      label={ null }
                      placeholder={ this.i18n('filter.role.placeholder') }
                      header={ this.i18n('filter.role.placeholder') }
                      rendered={ !roleDisabled && SecurityManager.hasAuthority('ROLE_AUTOCOMPLETE') }
                      multiSelect/>
                  </Basic.Col>
                  <Basic.Col lg={ 3 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={ this.cancelFilter.bind(this) }/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row rendered={ SecurityManager.hasAllAuthorities(['TREETYPE_AUTOCOMPLETE', 'TREENODE_AUTOCOMPLETE']) }>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TreeNodeSelect
                      ref="treeNodeId"
                      header={ this.i18n('filter.organization.placeholder') }
                      label={ null }
                      placeholder={ this.i18n('filter.organization.placeholder') }
                      rendered={ !treeNodeDisabled }
                      forceSearchParameters={ forceTreeNodeSearchParams }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.BooleanSelectBox
                      ref="recursively"
                      placeholder={ this.i18n('filter.recursively.placeholder') }
                      options={ [
                        { value: 'true', niceLabel: this.i18n('filter.recursively.yes') },
                        { value: 'false', niceLabel: this.i18n('filter.recursively.no') }
                      ]}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.BooleanSelectBox
                      ref="disabled"
                      placeholder={ this.i18n('filter.disabled.placeholder') }
                      options={ [
                        { value: 'true', niceLabel: this.i18n('label.disabled') },
                        { value: 'false', niceLabel: this.i18n('label.enabled') }
                      ]}/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.FormProjectionSelect
                      ref="formProjection"
                      placeholder={ this.i18n('filter.formProjection.placeholder') }
                      manager={ projectionManager }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="state"
                      placeholder={ this.i18n('filter.state.placeholder') }
                      enum={ IdentityStateEnum }/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 12 }>
                    <Advanced.Filter.CreatableSelectBox
                      ref="identifiers"
                      manager={ identityManager }
                      useCheck
                      placeholder={ this.i18n('filter.identifiers.placeholder') }
                      tooltip={ this.i18n('filter.identifiers.tooltip') }/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={ filterOpened }
          showFilter={ showFilter }
          forceSearchParameters={ _forceSearchParameters }
          buttons={
            [
              <Basic.Button
                level="success"
                showLoading={ showAddLoading }
                showLoadingIcon
                key="add_button"
                type="submit"
                className="btn-xs"
                onClick={ this.showDetail.bind(this, {}) }
                rendered={ showAddButton && this.getManager().canSave() }
                icon="fa:user-plus">
                { this.i18n('content.identity.create.button.add') }
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }
          className={ className }>
          <Advanced.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => (
                <Advanced.DetailButton
                  title={ this.i18n('button.detail') }
                  onClick={ this.showDetail.bind(this, data[rowIndex]) }/>
              )
            }
            rendered={ showDetailButton }/>
          <Advanced.Column
            header={ this.i18n('entity.Identity._type') }
            property="username"
            sort
            cell={
              ({ rowIndex, data }) => (
                <Advanced.EntityInfo
                  entityType="identity"
                  entityIdentifier={ data[rowIndex].username }
                  entity={ data[rowIndex] }
                  face="popover"/>
              )
            }
            rendered={ _.includes(columns, 'entityInfo') }/>
          <Advanced.ColumnLink
            to={ ({ rowIndex, data, event }) => {
              this.showDetail(data[rowIndex], event);
            }}
            property="username"
            width="20%"
            sort
            face="text"
            rendered={ _.includes(columns, 'username') }/>
          <Advanced.Column property="lastName" sort face="text" rendered={ _.includes(columns, 'lastName') }/>
          <Advanced.Column property="firstName" sort width="10%" face="text" rendered={ _.includes(columns, 'firstName') }/>
          <Advanced.Column property="externalCode" sort width="10%" face="text" rendered={ _.includes(columns, 'externalCode') }/>
          <Advanced.Column property="email" width="15%" face="text" sort rendered={ _.includes(columns, 'email') }/>
          <Advanced.Column property="disabled" face="bool" sort width={ 100 } rendered={ _.includes(columns, 'disabled') }/>
          <Advanced.Column property="state" face="enum" enumClass={ IdentityStateEnum } sort width={ 100 } rendered={ _.includes(columns, 'state') }/>
          <Advanced.Column property="description" sort face="text" rendered={ _.includes(columns, 'description') } maxLength={ 30 }/>
        </Advanced.Table>

        <Basic.Modal
          show={ showAddModal }
          onHide={ this.closeAddModal.bind(this) }
          backdrop="static"
          keyboard>
          <Basic.Modal.Header
            closeButton
            icon="fa:user-plus"
            text={ this.i18n('action.add.header') }/>
          <Basic.Div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', margin: '25px 0' }}>
            {
              !projections
              ||
              projections.map(projection => (
                <Basic.Button
                  className="btn-lg"
                  level={ projectionManager.getLocalization(projection, 'level', 'default') }
                  title={ projectionManager.getLocalization(projection, 'help') }
                  titlePlacement="bottom"
                  style={{ minWidth: 240, height: 125, margin: 15 }}
                  onClick={ (event) => {
                    if (event) {
                      event.preventDefault();
                    }
                    const newIdentity = {
                      id: uuid.v1(),
                      username: this.refs.text.getValue(),
                      formProjection: projection.id
                    };
                    this.context.store.dispatch(identityProjectionManager.receiveEntity(newIdentity.id, {
                      id: newIdentity.id,
                      identity: newIdentity
                    }));
                    const route = Utils.Ui.getRouteUrl(projection.route);
                    this.context.history.push(`${ route }/${ newIdentity.id }?new=1&projection=${ encodeURIComponent(projection.id) }`);
                  }}>
                  <Basic.Icon
                    icon={ projectionManager.getLocalization(projection, 'icon', 'fa:user-plus') }
                    style={{ display: 'block', marginBottom: 10 }}
                    className="fa-2x"/>
                  { projectionManager.getLocalization(projection, 'label', projection.code) }
                </Basic.Button>
              ))
            }
          </Basic.Div>
          <Basic.Div style={{ margin: '0 75px' }} rendered={ isDefaultFormProjection }>
            <Basic.Div className="text-divider">
              <span>{ this.i18n('action.add.or') }</span>
            </Basic.Div>
            <Basic.Div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: 25 }}>
              <Basic.Button
                level="link"
                onClick={ (event) => {
                  if (event) {
                    event.preventDefault();
                  }
                  this.context.history.push(`/identity/new?id=${ uuid.v1() }`);
                }}>
                { this.i18n('action.add.default') }
              </Basic.Button>
            </Basic.Div>
          </Basic.Div>
          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={ this.closeAddModal.bind(this) }>
              { this.i18n('button.close') }
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </Basic.Div>
    );
  }
}

IdentityTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  identityManager: PropTypes.object,
  /**
   * Rendered columns - see table columns above
   *
   * TODO: move to advanced table and add column sorting
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Button for create user will be shown
   */
  showAddButton: PropTypes.bool,
  /**
   * Detail button will be shown
   */
  showDetailButton: PropTypes.bool,
  /**
   * Show filter
   */
  showFilter: PropTypes.bool,
  /**
   * Table supports delete identities
   */
  deleteEnabled: PropTypes.bool,
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.bool,
  /**
   * Rendered
   */
  rendered: PropTypes.bool,
  /**
   * Css
   */
  className: PropTypes.string,
  /**
   * Filter tree type structure - given id ur default - false
   * @deprecated Remove after better tree type - node filter component
   */
  treeType: PropTypes.oneOfType([PropTypes.bool, PropTypes.string]),
};

IdentityTable.defaultProps = {
  identityManager: manager,
  columns: ['username', 'lastName', 'firstName', 'externalCode', 'email', 'state', 'description'],
  filterOpened: false,
  showAddButton: true,
  showDetailButton: true,
  showFilter: true,
  deleteEnabled: false,
  showRowSelection: false,
  forceSearchParameters: null,
  rendered: true,
  treeType: false
};

function select(state, component) {
  return {
    i18nReady: state.config.get('i18nReady'),
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    deleteEnabled: ConfigurationManager.getPublicValueAsBoolean(state, 'idm.pub.core.identity.delete'),
    skipDashboard: ConfigurationManager.getPublicValueAsBoolean(
      state,
      'idm.pub.core.identity.dashboard.skip',
      ConfigLoader.getConfig('identity.dashboard.skip', false)
    ),
    isDefaultFormProjection: ConfigurationManager.getPublicValueAsBoolean(state, 'idm.pub.app.show.identity.formProjection.default', true),
    showAddLoading: projectionManager.isShowLoading(state)
  };
}

export default connect(select, null, null, { forwardRef: true })(IdentityTable);
