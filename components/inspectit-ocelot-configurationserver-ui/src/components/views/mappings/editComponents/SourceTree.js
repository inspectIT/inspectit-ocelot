import React from 'react';
import {Tree} from 'primereact/tree';

import {isEqual} from 'lodash';
import * as utils from './utils';

class TreeTable extends React.Component{
	constructor(props){
	  super(props);
	  this.state = {
		selectedSources: {},
		}
	}

	render(){
	  return(
			<Tree
				value={this.props.tree}
			  selectionMode='checkbox'
			  selectionKeys={this.state.selectedSources}
				onSelectionChange={e => this.handleSelectionChange(e.value)}
			/>
	  )
	}

	componentDidUpdate = () => this.updateSelection();

	updateSelection = () => {
		if(!this.props.sourcePaths){ return }

		let selectionRes = {}
	  this.props.sourcePaths.forEach(source => {
			selectionRes = Object.assign(selectionRes, this.checkPath(source));
		})
		
		this.correctSelection(selectionRes)

		if(!isEqual(selectionRes, this.state.selectedSources)){
			this.setState({selectedSources: selectionRes})
		}
	}

	correctSelection = (selection) => {
		const keys = Object.keys(selection)
		keys.forEach(key => {
			if(selection[key].checked){
				const parentNode = utils.findParentNode(this.props.tree, key)
				if(parentNode && parentNode.children){
					this.checkTreeParent(parentNode, selection)
				}
			}
		})
	}

	handleSelectionChange = (selection) => {
		let selectedSources = [];

		let sourceKeys = Object.keys(selection);
		sourceKeys.forEach(source => {
			if(selection[source].checked === true && selection[source].partialChecked === false){
				selectedSources.push(source);
			}
		})
		
		this.props.onChangeSources(selectedSources);
	  this.setState({selectedSources: selection});
	}
  
	checkPath = (nodePath) => {
		let selectionResult = {};
		const substrings = utils.getTreeKeysArray(nodePath);

		substrings.forEach((nodeKey, index) => {
			if(!substrings[index+1]){
				selectionResult[[nodeKey]] = {checked: true, partialChecked: false};
			} else {
				selectionResult[[nodeKey]] = {checked: false, partialChecked: true};
			}
		})

		const	isNode = utils.findNode((this.props.tree), `${nodePath.endsWith('/*') ? nodePath.slice(0, -2) : nodePath}`);
		if(isNode && isNode.children){
			this.checkTreeChildren(isNode.children, selectionResult);
		}

		return selectionResult;
	}

	checkTreeChildren = (parentNode, selection) => {
	  parentNode.forEach(node => {
			selection[[node.key]] = {checked: true, partialChecked: false};
			if(node.children) {
				this.checkTreeChildren(node.children, selection);
			}
	  })
	}

	checkTreeParent = (parentNode, selection) => {
		let check = true
		parentNode.children.forEach(child => {
			if(!selection[child.key] || !selection[child.key].checked){ 
				check = false 
			}
		})

		if(check) {
			selection[parentNode.key] = {checked: true, partialChecked: false};

			const parent = utils.findParentNode(this.props.tree, parentNode.key)
			
			if(parent && parent.children){
				this.checkTreeParent(parent, selection)
			}
		}
	}
}
  
  export default TreeTable