

import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import { AdvancedTable, AdvancedColumn } from '../../../components/advanced';
import { SecurityManager } from '../../../modules/core/redux';
import { ConfigService } from '../../../modules/core/services';
import ComponentService from '../../../services/ComponentService';

class AppModules extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      moduleDescriptors: [],
      components: []
    }
    this.configService = new ConfigService();
    this.componentService = new ComponentService();
  }

  getContentKey() {
    return 'content.system.app-modules';
  }

  componentDidMount() {
    this.selectNavigationItem('system-modules');
  }


  render() {
    const { userContext } = this.props;

    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader text={this.i18n('header')}/>

        {
          this.configService.getEnabledModuleIds().sort().map(moduleId => {
            const moduleDescriptor = this.configService.getModuleDescriptor(moduleId);
            const componentDescriptor = this.componentService.getComponentDescriptor(moduleId);
            //
            return (
              <Basic.Panel>
                <Basic.PanelHeader text={<span>{moduleDescriptor.name} <small>{moduleDescriptor.id}</small></span>}/>
                <Basic.PanelBody>
                  {moduleDescriptor.description}
                </Basic.PanelBody>

                <Basic.PanelHeader text="Komponenty"/>

                <Basic.Table
                  data={componentDescriptor.components}
                  rowClass={({rowIndex, data}) => { return data[rowIndex].module !==  this.componentService.getComponentDefinition(data[rowIndex].id).module ? 'disabled' : ''}}>
                  <Basic.Column
                    property="id"
                    header="Identifikátor"
                    cell={({rowIndex, data}) => {
                      const overridedInModule = this.componentService.getComponentDefinition(data[rowIndex].id).module;
                      const overrided = data[rowIndex].module !== overridedInModule;
                      let textStyle = {};
                      if (overrided) {
                        textStyle.textDecoration = 'line-through';
                      }
                      return (
                        <span
                          title={overrided ? 'Komponenta přetížena v modulu: ' + overridedInModule : ''}>
                          <span style={textStyle}>{data[rowIndex].id}</span>
                          {
                            !overrided
                            ||
                            <span> ({overridedInModule})</span>
                          }
                        </span>
                      );
                    }}/>
                  <Basic.Column property="priority" header="Priorita" width="100px"/>
                  <Basic.Column property="type" header="Typ" width="100px"/>
                  <Basic.Column property="order" header="Pořadí" width="100px"/>
                  <Basic.Column property="span" header={<span>Span <small>col-lg</small></span>} width="100px"/>
                </Basic.Table>
              </Basic.Panel>
            );
          })
        }

      </div>
    );
  }
}

AppModules.propTypes = {
  userContext: PropTypes.object
};
AppModules.defaultProps = {
  userContext: null
};

function select(state) {
  return {
    userContext: state.security.userContext
  }
}

export default connect(select)(AppModules)
