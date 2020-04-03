import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Utils, Managers, Domain, Enums } from 'czechidm-core';
import { ExportImportManager } from '../../redux';
import ExportImportTypeEnum from '../../enums/ExportImportTypeEnum';
import ExportImportDetail from './ExportImportDetail';

const manager = new ExportImportManager();
const longRunningTaskManager = new Managers.LongRunningTaskManager();

/**
* Table of export-imports
*
* @author Vít Švanda
*
*/
export class ExportImportTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      filterOpened: this.props.filterOpened,
      report: null,
      activeKey: 1,
      longRunningTask: null
    };
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'content.export-imports';
  }

  /**
   * Base manager for this agenda (used in `AbstractTableContent`)
   */
  getManager() {
    return manager;
  }

  componentDidMount() {
    super.componentDidMount();
  }

  /**
   * Submit filter action
   */
  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.useFilterForm(this.refs.filterForm);
  }

  /**
   * Cancel filter action
   */
  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.cancelFilter(this.refs.filterForm);
  }

  /**
   * Submit filter action
   */
  useLogFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table_import_log.useFilterForm(this.refs.logFilterForm);
  }

  /**
   * Cancel filter action
   */
  cancelLogFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table_import_log.cancelFilter(this.refs.logFilterForm);
  }

  /**
   * Open modal with report's select
   */
  loadDetail(entity) {
    // reload Entity
    if (!Utils.Entity.isNew(entity)) {
      this.setState({
        detail: {
          show: false,
          entity
        }
      }, () => {
        this.context.store.dispatch(manager.fetchEntity(entity.id, `${this.getUiKey()}-detail`, (loadedEntity, error) => {
          if (error) {
            this.addError(error);
          } else {
            this.showDetail(loadedEntity);
          }
        }));
      });
    } else {
      this.showDetail(entity);
    }
  }

  showDetail(entity) {
    this.setState({
      longRunningTask: entity._embedded && entity._embedded.longRunningTask ? entity._embedded.longRunningTask : null
    }, () => {
      super.showDetail(entity, () => {
        this.setState({activeKey: 1});
      });
    });
  }

  /**
   * Close modal task detail
   */
  closeDetail() {
    this.setState({
      detail: {
        show: false,
        entity: {}
      },
      report: null,
      longRunningTask: null
    });
  }

  _onChangeSelectTabs(activeKey) {
    this.setState({activeKey});
  }

  _execute(entity, dryRun, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs[dryRun ? `confirm-execute-dry-run` : `confirm-execute`].show(
      this.i18n(dryRun ? `action.dry-run.message` : `action.execute.message`, { name: entity.name }),
      this.i18n(dryRun ? `action.dry-run.header` : `action.execute.header`,)
    ).then(() => {

      const cb = (realizedEntity, newError) => {
        if (!newError) {
          this.setState({
            showLoading: false
          });

          if (this.refs.table) {
            this.refs.table.reload();
          }
          this.addMessage({ message: this.i18n('action.execute.success', { count: 1, record: realizedEntity.uid }) });
        } else {
          this.setState({
            showLoading: false
          });
          this.addError(newError);
          if (this.refs.table) {
            this.refs.table.reload();
          }
        }
      };

      this.setState({
        showLoading: true
      }, () => {
        const dispatch = this.context.store.dispatch;
        dispatch(this.getManager().execute(entity.id, dryRun, null, cb));
      });
    }, () => {
      // Rejected
    });
  }

  /**
   * Validate extension type and upload definition
   * @param  {file} file File to upload
   */
  _upload(file) {
    if (!file.name.endsWith('.zip')) {
      this.addMessage({
        message: this.i18n('fileRejected', {name: file.name}),
        level: 'warning'
      });
      return;
    }
    this.setState({
      showLoading: true
    }, () => {
      const formData = new FormData();
      formData.append('name', file.name);
      formData.append('fileName', file.name);
      formData.append('data', file);

      this.getManager().getService().upload(formData)
        .then(() => {
          this.setState({
            showLoading: false
          }, () => {
            this.addMessage({
              message: this.i18n('fileUploaded', {name: file.name})
            });
            this.refs.table.reload();
          });
        })
        .catch(error => {
          this.setState({
            showLoading: false
          });
          this.addError(error);
        });
    });
  }

  /**
   * Dropzone component function called after select file
   * @param  {array} files Array of selected files
   */
  _onDrop(files) {
    if (this.refs.dropzone.state.isDragReject) {
      this.addMessage({
        message: this.i18n('filesRejected'),
        level: 'warning'
      });
      return;
    }
    files.forEach((file) => {
      this._upload(file);
    });
  }

  /**
  * Method get last string of split string by dot.
  * Used for get niceLabel for type entity.
  */
  _getType(name) {
    return Utils.Ui.getSimpleJavaType(name);
  }

  renderButtons(entity, className = '') {
    if (!entity.data) {
      return null;
    }
    return (
      <div>
        <a
          key="export-download-zip"
          href={ this.getManager().getService().getDownloadUrl(entity.id) }
          title={ this.i18n('action.download.title')}
          className={ `btn btn-primary ${className}` }
          style={{ color: 'white', marginLeft: 3 }}>
          <Basic.Icon value="fa:download" />
          {' '}
        </a>
        <Basic.Button
          ref="import-dry-run"
          type="button"
          level="info"
          rendered={ Managers.SecurityManager.hasAnyAuthority(['EXPORTIMPORT_UPDATE']) }
          style={{ marginLeft: 3 }}
          title={ this.i18n('action.dry-run.title') }
          titlePlacement="bottom"
          onClick={ this._execute.bind(this, entity, true) }
          disabled={ entity.type !== 'IMPORT' }
          className="btn-xs"
          icon="fa:play"/>
        <Basic.Button
          ref="import-execute"
          type="button"
          level="success"
          rendered={ Managers.SecurityManager.hasAnyAuthority(['EXPORTIMPORT_UPDATE']) }
          style={{ marginLeft: 3 }}
          title={ this.i18n('action.execute.title') }
          titlePlacement="bottom"
          onClick={ this._execute.bind(this, entity, false) }
          disabled={ entity.type !== 'IMPORT' }
          className="btn-xs"
          icon="fa:play"/>
      </div>
    );
  }

  render() {
    const {
      uiKey,
      columns,
      forceSearchParameters,
      showRowSelection,
      showLoadingDetail
    } = this.props;
    const {
      filterOpened,
      detail,
      longRunningTask,
    } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Confirm ref="confirm-execute" level="success"/>
        <Basic.Confirm ref="confirm-execute-dry-run" level="info"/>
        <Basic.Panel style={{margin: '5px'}}>
          <Basic.Dropzone
            ref="dropzone"
            accept=".zip"
            onDrop={this._onDrop.bind(this)}>
            {this.i18n('dropzone.infoText')}
          </Basic.Dropzone>
        </Basic.Panel>
        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          manager={ manager }
          rowClass={ ({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); } }
          filterOpened={filterOpened}
          forceSearchParameters={ forceSearchParameters }
          showRowSelection={ Managers.SecurityManager.hasAuthority('EXPORTIMPORT_DELETE') && showRowSelection }
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate ref="fromTill"/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text.placeholder')}
                      help={ Advanced.Filter.getTextHelp() }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false },
            ]
          }
          _searchParameters={ this.getSearchParameters() }
        >
          <Advanced.Column
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.loadDetail.bind(this, data[rowIndex])}/>
                );
              }
            }/>
          <Advanced.Column
            property="type"
            face="enum"
            enumClass={ExportImportTypeEnum}
            sort
            width={ 100 }
            rendered={ _.includes(columns, 'type') }/>
          <Advanced.Column
            width={75}
            header={this.i18n('entity.LongRunningTask.result.state')}
            sort
            sortProperty="longRunningTask.result.state"
            rendered={ _.includes(columns, 'state') }
            cell={
              ({ data, rowIndex }) => {
                const entity = data[rowIndex];
                if (!entity.result || !entity.result.state) {
                  return '';
                }
                const lrt = entity._embedded && entity._embedded.longRunningTask ? entity._embedded.longRunningTask : null;
                const label = !lrt || Enums.OperationStateEnum.findSymbolByKey(entity.result.state) !==
                Enums.OperationStateEnum.RUNNING ? null : longRunningTaskManager.getProcessedCount(lrt);
                //
                return (
                  <Advanced.OperationResult value={ entity.result } stateLabel={ label } detailLink={ () => this.loadDetail(data[rowIndex]) }/>
                );
              }
            }/>
          <Advanced.Column
            property="executorName"
            width="20%"
            sort
            rendered={_.includes(columns, 'executorName')}
            cell={
              ({ data, rowIndex, property }) => {
                const name = data[rowIndex][property];
                let key = `eav.bulk-action.${name}.title`;
                let localizedKey = this.i18n(key);
                // TODO: localization supports only core and acc now.
                if (key === localizedKey) {
                  key = `acc:${key}`;
                  localizedKey = this.i18n(key);
                  if (key === localizedKey) {
                    return localizedKey;
                  }
                }
                return localizedKey;
              }
            }
          />
          <Advanced.Column
            property="name"
            sort
            face="text"
            width="30%"
            rendered={ _.includes(columns, 'name') }/>
          <Advanced.Column
            property="created"
            sort
            face="datetime"
            rendered={ _.includes(columns, 'created') }/>
          <Advanced.Column
            property="modified"
            sort
            face="datetime"
            rendered={ _.includes(columns, 'modified') }/>
          <Advanced.Column
            property="creator"
            rendered={_.includes(columns, 'creator')}
            sort
            cell={
              ({ data, rowIndex, property }) => {
                return (
                  <Advanced.EntityInfo
                    entityType="identity"
                    entityIdentifier={ data[rowIndex][property] }
                    face="popover" />
                );
              }
            }
          />
          <Advanced.Column
            header={ this.i18n('actions') }
            width="20%"
            cell={
              ({ data, rowIndex }) => {
                return this.renderButtons(data[rowIndex], 'btn-xs');
              }
            }/>
        </Advanced.Table>

        <Basic.Modal
          show={ detail.show }
          onHide={ this.closeDetail.bind(this) }
          backdrop="static"
          bsSize="large"
          keyboard={ !showLoadingDetail }>

          <form>
            <Basic.Modal.Header
              closeButton={ !showLoadingDetail }
              text={ this.i18n('detail.header') }/>
            <Basic.Modal.Body>
              <ExportImportDetail
                showLoading={showLoadingDetail}
                detail={detail}
                longRunningTask={longRunningTask}
              />
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.closeDetail.bind(this)}
                showLoading={ showLoadingDetail }>
                {this.i18n('button.close')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

ExportImportTable.propTypes = {
  /**
   * Entities, permissions etc. fro this content are stored in redux under given key
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Rendered columns (all by default)
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  /**
   * Show filter or collapse
   */
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Show add button to create new product
   */
  showAddButton: PropTypes.bool,
  /**
   * Show row selection for bulk actions
   */
  showRowSelection: PropTypes.bool,
  /**
   * Supported reports - immutable map
   */
  supportedTasks: PropTypes.object
};

ExportImportTable.defaultProps = {
  columns: ['state', 'created', 'modified', 'creator', 'type', 'name', 'executorName'],
  filterOpened: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showAddButton: true,
  showRowSelection: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey), // persisted filter state in redux,
    showLoading: Utils.Ui.isShowLoading(state, ExportImportManager.UI_KEY_SUPPORTED_REPORTS)
                  || Utils.Ui.isShowLoading(state, component.uiKey),
    showLoadingDetail: Utils.Ui.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { forwardRef: true })(ExportImportTable);
