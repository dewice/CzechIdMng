import React from 'react';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { IdentityManager } from '../../../redux';
import IdentityStateEnum from '../../../enums/IdentityStateEnum';

const identityManager = new IdentityManager();

/**
 * Quick button to enable identity manually.
 *
 * @author Radek Tomiška
 * @since 9.6.0
 */
class EnableIdentityDashboardButton extends Advanced.AbstractIdentityDashboardButton {

  getIcon() {
    return this.i18n('eav.bulk-action.identity-enable-bulk-action.icon');
  }

  isRendered() {
    const { identity, userContext, permissions } = this.props;
    //
    return identity.state === IdentityStateEnum.findKeyBySymbol(IdentityStateEnum.DISABLED_MANUALLY)
      && userContext.id !== identity.id
      && Utils.Permission.hasPermission(permissions, 'MANUALLYENABLE');
  }

  getLabel() {
    return this.i18n('eav.bulk-action.identity-enable-bulk-action.label');
  }

  getTitle() {
    return this.i18n('eav.bulk-action.identity-enable-bulk-action.title');
  }

  onClick() {
    const { identity } = this.props;
    //
    this.refs['confirm-enable'].show(
      this.i18n(`content.identities.action.activate.message`, { count: 1, record: identityManager.getNiceLabel(identity) }),
      this.i18n(`content.identities.action.activate.header`, { count: 1 })
    ).then(() => {
      this.setState({
        showLoading: true
      }, () => {
        identityManager
          .getService()
          .activate(identity.id)
          .then(json => {
            // new entity to redux trimmed store
            this.context.store.dispatch(identityManager.receiveEntity(identity.id, json));
            //
            this.setState({
              showLoading: false
            });
          });
      });
    }, () => {
      // nothing
    });
  }

  renderConfirm() {
    return (
      <Basic.Confirm ref="confirm-enable" level="success" />
    );
  }
}

function select(state) {
  return {
    i18nReady: state.config.get('i18nReady'), // required
    userContext: state.security.userContext // required
  };
}

export default connect(select)(EnableIdentityDashboardButton);
