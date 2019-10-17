import React from 'react';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration';

import TreeTable from './SourceTree';
import SourceTable from './SourceTable';
import EditSourceToolbar from './SourceToolbar';

import * as utils from './utils'
import { isEqual, cloneDeep } from 'lodash';

const rootTree = {
  key: '/',
  label: '/',
  icon: 'pi pi-fw pi-folder',
  children: []
}

class EditSources extends React.Component{
  constructor(props){
    super(props);
    this.state = {
      tree: [rootTree]
    }
  }

  render(){
    return(
      <div className='outerContainer' style={{marginTop: '0.25em', display: 'flex'}}>
        <style jsx>{`
          .left{
            width: 50%;
          }
          .right{
            width: 50%;
            border: 1px solid #ddd;
            margin-left: 0.5em;
            max-height: ${this.props.maxHeight};
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
          <EditSourceToolbar 
            sourcePaths={this.props.sources}
            onAddSource={this.props.onAddSource}
            tree={this.state.tree}
          />
          <SourceTable 
            sourcePaths={this.props.sources}
            onRowReoder={this.props.onUpdateAllSources}  
            maxHeight={this.props.maxHeight ? `calc(${this.props.maxHeight} - 2.95em)` : ''}
          />
        </div>
        <div className='right'>
          <TreeTable 
            sourcePaths={this.props.sources}
            tree={this.state.tree}
            onChangeSources={this.cleanUpSourcePaths}
            expandedKeys={{ '/': true}}
          />
        </div>
      </div>
    )
  }
  componentDidMount = () => {
    this.props.fetchFiles();
  }

  componentDidUpdate(prevProps){
    if(!prevProps.visible && this.props.visible){
      const newTree = [
        {
          ...rootTree, 
          children: cloneDeep(this.props.files)
        }
      ]
      this.setState({tree: newTree})
    } else{
      this.updateTree();
      this.cleanUpSourcePaths();
    }
  }

  updateTree = () => {
    if(!this.props.sources) {return}
    let newTree = cloneDeep(this.state.tree)
    this.props.sources.forEach(path => {
      path = utils.toNodeKey(path);

      if(!utils.findNode(newTree, path)){
        utils.addNode(newTree[0].children, path);
      }
    })

    if(!isEqual(newTree, this.state.tree)){
      this.setState({tree: newTree});
    }
  }

  cleanUpSourcePaths = (sources = this.props.sources) => {
    if(!sources) { 
      return 
    }

    let newSourceArray = [];
    let includedNodes = [];

    sources.forEach(path => {
      path = utils.toNodeKey(path);
      let node = utils.findNode(this.state.tree, path);

      if(!newSourceArray.includes(path)){
        if(node){
          newSourceArray.push(path);
          includedNodes.push(node.children ? {path: path, isFolder: true} : {path: path});
        } else if(path === '/') {
          newSourceArray.push(path)
          includedNodes.push({path: path, isFolder: true});
        } else {
          newSourceArray.push(path);
        }
      }
    })
    newSourceArray = utils.removeSequelPaths(newSourceArray, includedNodes);

    if(!isEqual(newSourceArray, this.props.sources)){
      this.props.onUpdateAllSources(newSourceArray);
    }
  }
}

function mapStateToProps(state) {
  const { files } = state.configuration;
  return {
      files: configurationSelectors.getFileTree(state),
  }
}

const mapDispatchToProps = {
  fetchFiles: configurationActions.fetchFiles,
}

export default connect(mapStateToProps, mapDispatchToProps)(EditSources)