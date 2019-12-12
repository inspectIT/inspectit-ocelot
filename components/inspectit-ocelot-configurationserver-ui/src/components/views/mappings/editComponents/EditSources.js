import React from 'react';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration';

import SourceTree from './EditSourceComponents/SourceTree';
import SourceTable from './EditSourceComponents/SourceTable';
import SourceToolbar from './EditSourceComponents/SourceToolbar';

import { isEqual, uniqWith } from 'lodash';
import * as treeUtils from './EditSourceComponents/treeUtils';

class EditSources extends React.Component {

  render() {
    return (
      <div style={{ marginTop: '0.25em', display: 'flex' }}>
        <style jsx>{`
          .left{
            width: 50%;
          }
          .right{
            width: 50%;
            border: 1px solid #ddd;
            margin-left: 0.5em;
            height: ${this.props.maxHeight};
            overflow: auto;
          }
          .right :global(.p-tree){
            border: none;
            margin: 0;
            padding: 0;
            margin-left: 0.5em;
          }
        `}</style>
        <div className='left'>
          <SourceToolbar
            sourcePaths={this.props.sources}
            onChange={this.handleChangeSources}
          />
          <SourceTable
            sourcePaths={this.props.sources}
            onRowReoder={this.props.onChange}
            maxHeight={this.props.maxHeight ? `calc(${this.props.maxHeight} - 3em)` : ''}
          />
        </div>
        <div className='right'>
          <SourceTree
            sourcePaths={this.props.sources}
            onSelectionChange={this.handleChangeSources}
            visible={this.props.visible}
          />
        </div>
      </div>
    )
  }
  componentWillMount = () => {
    this.props.fetchFiles();
  }

  handleChangeSources = (sources = []) => {
    /** uniqWith removes 'equal' entries of the given array using the given function to compare elements */
    const newSourceArray = uniqWith(sources.sort(), treeUtils.isSubfile);

    if (!isEqual(newSourceArray, this.props.sources)) {
      this.props.onChange(newSourceArray);
    }
  }
}

function mapStateToProps(state) {
  return {
    files: configurationSelectors.getFileTree(state),
  }
}

const mapDispatchToProps = {
  fetchFiles: configurationActions.fetchFiles,
}

export default connect(mapStateToProps, mapDispatchToProps)(EditSources)