import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import classnames from 'classnames';
//
import * as Basic from '../../basic';

export default class TabPanelItem extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const { className, to, active, ...others } = this.props;
    const itemClassNames = classnames(
      { 'list-group-item': false },
      { active },
      className
    );
    return (
      <li className={itemClassNames}>
        <Link to={to} {...others}>
          {this.props.children}
        </Link>
      </li>
    );
  }
}

TabPanelItem.propTypes = {
  to: PropTypes.string,
  title: PropTypes.string,
  active: PropTypes.bool
};
