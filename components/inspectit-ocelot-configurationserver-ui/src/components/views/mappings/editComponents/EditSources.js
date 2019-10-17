import React from 'react';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration';

import TreeTable from './SourceTree';
import SourceTable from './SourceTable';
import EditSourceToolbar from './SourceToolbar';

import * as utils from './utils'
import { isEqual, cloneDeep } from 'lodash';

class EditSources extends React.Component{
  constructor(props){
    super(props);
    this.state = {
      tree: null
    }
  }

  render(){
    return(
      <div className='outerContainer' style={{marginTop: "0.25em"}}>
        <style jsx>{`
          .outerContainer{
            display: flex;
          }
          .containerLeft{
            display: flex;
            flex-direction: column;
            border-right: 1px solid #ddd;
          }
          .innerContainerTop{
            height: 3rem;
            display: flex;
            align-items: center;
            padding-left: 0.5em;
            border-bottom: 2px solid #ddd;
          }
          .containerRight :global(.p-tree){
            border: none;
            padding-top: 0.25em;
            margin: 0.5em;
          }
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
            // overflow: auto auto;
          }
        `}</style>
        <div className='left'>
          <EditSourceToolbar 
            sourcePaths={this.props.sources}
            onAddSource={this.handleAddSource}
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
          />
        </div>



        {/* <div className='containerLeft'>
          <div className='innerContainerTop'>
            <EditSourceToolbar 
              sourcePaths={this.props.sources}
              onAddSource={this.handleAddSource}
              tree={this.state.tree}
            />
          </div>
            
        </div>
        <div className='containerRight'>
          <ScrollPanel style={{height: `${this.props.height+45}px`}}>
            <TreeTable 
              sourcePaths={this.props.sources}
              tree={this.state.tree}
              onChangeSources={this.cleanUpSourcePaths}
            />
          </ScrollPanel>
        </div> */}
      </div>
    )
  }

  componentDidMount = () => this.props.fetchFiles();

  componentDidUpdate(prevProps){
    if(!prevProps.visible && this.props.visible){
      this.setState({tree: cloneDeep(this.props.files)});
    }
    this.updateTree();
    this.cleanUpSourcePaths();
  }

  handleAddSource = (newSource) => {
    const isNode = utils.findNode(this.props.files, newSource);
    if(isNode){
      this.props.onAddSource(isNode.children ? `${newSource}/*` : newSource)
    } else if (newSource === '/*'){
      this.props.onAddSource(newSource)
    } else {
      this.props.onAddSource(!utils.isFile(newSource) ? `${newSource}/*` : newSource)
    }
  }

  updateTree = () => {
    if(!this.props.sources) {return}
    let newTree = cloneDeep(this.state.tree ? this.state.tree : this.props.files)
    this.props.sources.forEach(path => {
      path = utils.toNodeKey(path);

      if(!utils.findNode(newTree, path)){
        utils.addNode(newTree, path);
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

    const treeToUse = this.state.tree ? this.state.tree : this.props.files;
    let newSourceArray = [];
    let includedNodes = [];

    sources.forEach(path => {
      path = utils.toNodeKey(path);
      let node = utils.findNode(treeToUse, path);

      if(!newSourceArray.includes(path)){
        if(node){
          newSourceArray.push(node.children ? `${path}/*` : path);
          includedNodes.push(node.children ? {path: path, isFolder: true} : {path: path});
        } else if(path === '/') {
          newSourceArray.push('/*')
        } else {
          newSourceArray.push(!utils.isFile(path) ? `${path}/*` : path);
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