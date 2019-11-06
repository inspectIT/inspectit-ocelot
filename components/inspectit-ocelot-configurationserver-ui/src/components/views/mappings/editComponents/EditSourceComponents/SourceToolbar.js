import React from 'react';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../../redux/ducks/notification';

import { cloneDeep } from 'lodash';
import * as treeUtils from './treeUtils';

/**
 * Toolbar for EditSources Component
 * Path's for config files can be added through type input
 */
class SourceToolbar extends React.Component {
  state = {
    newSource: ''
  }

  render() {
    return (
      <div className="p-inputgroup" style={{ display: "inline-flex", verticalAlign: "middle", width: '100%', 'margin-bottom': '0.5em' }}>
        <span className="pi p-inputgroup-addon" style={{ background: 'inherit', 'border-color': '#656565' }}>/</span>
        <InputText
          value={this.state.newSource}
          placeholder='New Source'
          onChange={this.handleChange}
          onKeyPress={e => { if (e.key === 'Enter') { this.handleClick() } }}
          style={{ width: '100%' }}
        />
        <Button icon='pi pi-plus' onClick={this.handleClick} style={{ width: '3rem' }} />
      </div>
    )
  }

	/**
	 * Tries to add the source 
	 */
  handleClick = () => {
    const newSource = '/' + this.state.newSource;

    if (!this.canAddSource(newSource)) {
      this.props.showInfoMessage('Path has not been added', `The path "${newSource}" could not be added, since it is already included.`);
      return;
    }

    let newSourceArray = cloneDeep(this.props.sourcePaths) || [];
    newSourceArray.unshift(newSource);
    this.props.onChange(newSourceArray);

    this.setState({ newSource: '' });
  }

  handleChange = (e) => {
    const { value, style } = e.target;
    style.color = !this.canAddSource(`/${value}`) ? 'red' : 'black';
    this.setState({ newSource: value });
  }

  /**
   * returns if the node can/should be added or not
   * ~ nodes can't be added if it's parent node (and therefore all childs) are already included
   * ~ nodes can't be added as children of files
   * 
   * @param {string} newSource - the source to be added
   */
  canAddSource = (newSource) => {
    let res = true;
    if (!this.props.sourcePaths) { return res; }

    // check if the new source is already included or a subfile
    this.props.sourcePaths.forEach(source => {
      if (source === newSource || treeUtils.isSubfile(newSource, source)) {
        res = false;
      }
    });

    // check if the the new source can't exist - since it includes a file as parent node
    const substrings = treeUtils.getParentKeys(newSource);
    for (let i = 0; res && i < substrings.length; i++) {
      const isNode = treeUtils.findNode(this.props.tree, substrings[i]);
      if (isNode && !isNode.children && treeUtils.isSubfile(newSource, isNode.key)) {
        res = false;
      }
    }

    return res;
  }
}

const mapDispatchToProps = {
  showInfoMessage: notificationActions.showInfoMessage,
}

export default connect(null, mapDispatchToProps)(SourceToolbar)