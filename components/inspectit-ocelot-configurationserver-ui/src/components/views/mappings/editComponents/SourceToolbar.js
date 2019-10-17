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
		this.state = {};
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
					<Button icon='pi pi-plus' disabled={!this.state.newSource || this.state.newSource === '/'} onClick={this.handleClick} style={{width: '3rem'}}/>
			</div>
		)
	}

	handleClick = () => {
		const {newSource} = this.state;
		if(!newSource || newSource === '/') {
			return
		}

		if(this.checkExistingFile(newSource)){
			this.props.showInfoMessage('Path has not been added', `The path "${newSource}" could not be added. It might be included already.`);
			return
		}
		
		this.props.onAddSource(newSource);
		this.setState({newSource: ''});
	}

	handleChange = (e) => {
		const {value, style} = e.target;
		const newSource = value.startsWith('/') ? value : `/${value}`;

		style.color = this.checkExistingFile(newSource) ? 'red' : 'black';

		this.setState({newSource});
	}

	checkExistingFile = (newSource) => {
		let res;
		if(!this.props.sourcePaths){return res;}

		this.props.sourcePaths.forEach(source => {
			if(source === newSource || (source.endsWith('/*') && newSource.startsWith(source.slice(0, -2)) && newSource.search(new RegExp(`${escapeRegExp(source.slice(0, -2))}(?!\\w)(\\/|(?!\\W))`)) !== -1 )){
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