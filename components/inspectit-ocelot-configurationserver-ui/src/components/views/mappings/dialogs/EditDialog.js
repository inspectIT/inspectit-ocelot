import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../../redux/ducks/mappings';
import { notificationActions } from '../../../../redux/ducks/notification';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Fieldset } from 'primereact/fieldset';

import EditSources from '../editComponents/EditSources'
import {cloneDeep, isEqual} from 'lodash';
import EditAttributes from '../editComponents/EditAttributes';

/**
 * Dialog for editing the given mapping.
 */

const defaultMapping = {
	name: '',
	sources: [],
	attributes: {}
}

class EditMappingDialog extends React.Component {

	constructor(props){
		super(props);
		this.state = {
			mapping: {},
		};
	}

	handleAddSource = (newSource) => {
		let sourceCopy = cloneDeep(this.state.mapping.sources);
		if(!sourceCopy){
			sourceCopy = [];
		}

		sourceCopy.unshift(newSource);
		this.setState({ mapping: {...this.state.mapping, sources: sourceCopy} });
  }

	handleUpdateSources = (newSources) => this.setState({ mapping: {...this.state.mapping, sources: newSources} });

	handleChangeAttributeKey = (oldKey, newKey) => {
		if (oldKey === newKey){
			return
		}

		const {mapping} = this.state;
    let newAttributes = {}
		if(!oldKey){
			newAttributes = cloneDeep(mapping.attributes)
			if(!newAttributes){
				newAttributes = {}
			}
			newAttributes[newKey] = '.*'
		} else {
				const oldKeys = Object.keys(mapping.attributes);
				oldKeys.forEach(key => {
						if(oldKey === key && !newAttributes[newKey]){
								newAttributes[newKey] = mapping.attributes[key]
						} else if(oldKey !== key && !newAttributes[oldKey]){
								newAttributes[key] = mapping.attributes[key]
						}
				})
		}

		this.setState({ mapping: {...mapping, attributes: newAttributes} });
	}
	
	handleChangeValueOrAddAttribute = (key, value) => this.setState({ mapping: {...this.state.mapping, attributes: {...this.state.mapping.attributes, [key]: value }} });

	handleDeleteAttribute = (attribute) => {
		let newAttributes = cloneDeep(this.state.mapping.attributes)
		delete newAttributes[Object.keys(attribute)[0]]
		this.setState({ mapping: {...this.state.mapping, attributes: newAttributes} });
  }

  render() {
		const {mapping} = this.state;
		const maxHeightFieldset = this.state.height * 0.35
    return (
			<div className='this'>
				<style jsx>{`
					.this :global(.p-dialog-content){
						padding: 0.25em 0em 0em 0em;
						border-left: 1px solid #ddd;
						border-right: 1px solid #ddd;
					}
					.middle{
					 border-top: 2px solid #ddd;
					 border-bottom: 2px solid #ddd;
					}
				`}</style>
				<Dialog
					header={this.state.isNewMapping ? 'Add Mapping' : 'Edit Mapping'}
					modal={true}
					visible={this.props.visible}
					onHide={this.handleCancel}
					style={{'max-width': '1100px', 'min-width': '650px'}}
					footer={(
						<div>
							<Button label={this.state.isNewMapping ? 'Add' : 'Update'} className="p-button-primary" onClick={this.handleClick} />
							<Button label="Cancel" className="p-button-secondary" onClick={this.handleCancel} />
						</div>
					)}
				>
					<Fieldset legend='Mapping Name' style={{'padding-top': 0, overflow: 'hidden'}}>
						<span>
							<div className="p-inputgroup" style = {{display: "inline-flex", verticalAlign: "middle", width: '100%',}}>
									<InputText 
										placeholder='Enter new name'
										value={mapping.name ? mapping.name : ''} 
										onChange={e => this.setState({mapping: {...mapping, name: e.target.value}}) }
										style={{width: '100%'}} 
									/>
									<span className="pi p-inputgroup-addon pi-pencil" style={{background: 'inherit', 'border-color': '#656565'}}/>
							</div>
						</span>
					</Fieldset>
					<Fieldset legend='Sources' style={{'padding-top': 0, 'max-height': maxHeightFieldset, overflow: 'hidden'}}>
						<EditSources
								visible={this.props.visible}
								sources={mapping.sources} 
								onUpdateAllSources={this.handleUpdateSources}
								onAddSource={this.handleAddSource}
								maxHeight={`calc(${maxHeightFieldset}px - 10em)`}
							/> 
					</Fieldset>
					<Fieldset legend='Attributes' style={{'padding-top': 0, 'max-height': maxHeightFieldset, overflow: 'hidden'}}>
						<EditAttributes 
							attributes={mapping.attributes}
							onChangeAttributeKey={this.handleChangeAttributeKey}
							onDeleteAttribute={this.handleDeleteAttribute}
							onChValueOrAddAttribute={this.handleChangeValueOrAddAttribute}
							maxHeight={`calc(${maxHeightFieldset}px - 10em)`}
						/>
					</Fieldset>
				</Dialog>
			</div>
    )
	}
	
	componentWillReceiveProps(nextProps){
		if(nextProps.mapping !== this.props.mapping){
			if(!nextProps.mapping.name){
				this.setState({ mapping : defaultMapping, isNewMapping: true})
			}
			else {
				this.setState({mapping: cloneDeep(nextProps.mapping), isNewMapping: false});
			}
		}
	}

	componentDidUpdate(){
		const newHeight = window.innerHeight;
		if(newHeight !== this.state.height){
			this.setState({height: newHeight});
		}
	}

  handleClick = () => {
		if(!this.state.mapping.name  || (this.props.mapping.name !== this.state.mapping.name && checkIfNameAlreadyExists(this.props.mappings, this.state.mapping.name)) ){
			const msg = !this.state.mapping.name ? 'Enter a name for your mapping' : 'A Mapping with this name already exists';
			this.props.showWarningMessage('Mappings Could not be Updated', msg);
			return	
		}
		
		if(this.state.isNewMapping){
			this.props.addMapping(this.state.mapping, this.callbackOnSavingSuccess)
		} else{
			let newMappings = cloneDeep(this.props.mappings);
    	let indexToUpdate;
    	newMappings.forEach((element, index) => {
      	if(isEqual(element, this.props.mapping)) { 
					indexToUpdate = index; 
				}
    	});
			newMappings.splice(indexToUpdate, 1, this.state.mapping);
			this.props.putMappings(newMappings, this.callbackOnSavingSuccess)
		}
	}
	
	handleCancel = () => {
		this.props.onHide();
		this.setState({mapping: null, isNewMapping: null});
	}

	callbackOnSavingSuccess = (savingSuccessful) => {
		if(savingSuccessful){
			this.props.onHide();
			this.setState({mapping: {}, isNewMapping: null});
		}
	}
}

const checkIfNameAlreadyExists = (allMappings, newName) => {
	let res
	allMappings.forEach(mapping => {
		if(mapping.name === newName){
			res = true
		}
	})
	return res
}

function mapStateToProps(state) {
  const {mappings} = state.mappings;
  return {
    mappings
  }
}

const mapDispatchToProps = {
	addMapping: mappingsActions.putMapping,
	putMappings: mappingsActions.putMappings,
	showWarningMessage: notificationActions.showWarningMessage,
}

export default connect(mapStateToProps, mapDispatchToProps)(EditMappingDialog);