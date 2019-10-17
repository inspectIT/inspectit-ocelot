import React from 'react';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';

import { escapeRegExp } from 'lodash';
import * as utils from './utils';

class SourceToolbar extends React.Component{
	constructor(props){
		super(props);
		this.state = {
			newSource: ''
		};
	}

	render(){
		return(
			<div className="p-inputgroup" style = {{display: "inline-flex", verticalAlign: "middle", width: '100%', 'margin-bottom': '0.5em'}}>
				<span className="pi p-inputgroup-addon" style={{background: 'inherit', 'border-color': '#656565'}}>/</span>
					<InputText 
						value={this.state.newSource} 
						placeholder='new Source' 
						onChange={this.handleChange} 
						onKeyPress={e => {if(e.key === 'Enter'){this.handleClick()}}} 
						style={{width: '100%'}} 
					/>
					<Button icon='pi pi-plus' onClick={this.handleClick} style={{width: '3rem'}}/>
			</div>
		)
	}

	handleClick = () => {
		const source = '/' + this.state.newSource

		if(this.checkExistingFile(source)){
			this.props.showInfoMessage('Path has not been Added', `The path "${source}" could not be added. It might be included already.`);
			return
		}
		
		this.props.onAddSource(source);
		this.setState({newSource: ''});
	}

	handleChange = (e) => {
		const {value, style} = e.target;
		style.color = this.checkExistingFile(`/${value}`) ? 'red' : 'black';
		this.setState({newSource: value});
	}

	checkExistingFile = (newSource) => {
		let res;
		if(!this.props.sourcePaths){return res;}

		this.props.sourcePaths.forEach(source => {
			if(source === newSource || (newSource.length > source.length && newSource.search(new RegExp(`${escapeRegExp(source.slice(0, -2))}(?!\\w)(\\/|(?!\\W))`)) !== -1 )){
				res = true;
			}
		});
		
		const substrings = utils.getTreeKeysArray(newSource);
		for(let i = 0; !res && i < substrings.length; i++){
			const isNode = utils.findNode(this.props.tree, substrings[i]);
			if(isNode && !isNode.children && newSource.length > isNode.key.length){
				res = true;
			}
		}

		return res;
	}	
}

const mapDispatchToProps = {
  showInfoMessage: notificationActions.showInfoMessage,
}

export default connect(null, mapDispatchToProps)(SourceToolbar)