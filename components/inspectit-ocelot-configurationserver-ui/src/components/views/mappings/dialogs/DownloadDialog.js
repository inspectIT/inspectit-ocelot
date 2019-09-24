import React from 'react'

import { connect } from 'react-redux'
import { agentConfigActions } from '../../../../redux/ducks/agent-config'

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import EditAttributes from '../editComponents/EditAttributes';

/**
 * Dialog for downloading a configuration file.
 */
class DownloadDialog extends React.Component {

    downloadButton = React.createRef();

    state = {
        attributes : null
    }

    handleChangeAttributeKey = (oldKey, newKey) => {
		if (oldKey === newKey){return}
		const {attributes} = this.state;

        const oldKeys = Object.keys(attributes);
		let newAttributes = {};

		oldKeys.forEach(key => {
			if(oldKey === key && !newAttributes[newKey]){
				newAttributes[newKey] = attributes[key];
			} else if (!(oldKey === key && newAttributes[keyValue])){
				newAttributes[key] = attributes[key];
			}
		})
		this.setState({ attributes: newAttributes });
	}
	
	handleChangeValueOrAddAttribute = (key, value) => this.setState({ attributes: {...this.state.attributes, [key]: value} });

	handleDeleteAttribute = (deletable) => {
        const {attributes} = this.state
        let newAttributes = {};
        Object.keys(attributes).forEach(key => {
            newAttributes[key] = attributes[key];
        })

        deletable.forEach(element => {
            const key =  Object.keys(element)[0];
            const value = element[Object.keys(element)[0]];
            if(newAttributes[key] && newAttributes[key] === value){
                delete newAttributes[key];
            }                                                       
        })
        this.setState({ attributes: newAttributes });
    }

    render() {
        return (
            <Dialog
                header={'Download configuration file'}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                style={{'max-width': '900px'}}
                footer={(
                    <div>
                        <Button label="Download" ref={this.downloadButton} className="p-button-primary" onClick={this.handleDownload} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                <EditAttributes 
                    onChValueOrAddAttribute={this.handleChangeValueOrAddAttribute}
                    onChangeAttributeKey={this.handleChangeAttributeKey}
                    onDeleteAttribute={this.handleDeleteAttribute}
                    attributes={this.state.attributes}
                    height={200}
                />
            </Dialog>
        )
    }

    handleDownload = () => {
        this.props.onHide();
        this.props.downloadConfiguration(this.state.attributes);
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.downloadButton.current.element.focus();
        }
    }

}

const mapDispatchToProps = {
    downloadConfiguration: agentConfigActions.fetchConfigurationFile
}

export default connect(null, mapDispatchToProps)(DownloadDialog);