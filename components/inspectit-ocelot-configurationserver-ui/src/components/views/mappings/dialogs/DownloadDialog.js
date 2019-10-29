import React from 'react';

import { connect } from 'react-redux';
import { agentConfigActions } from '../../../../redux/ducks/agent-config';
import { notificationActions } from '../../../../redux/ducks/notification';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import KeyValueEditor from '../editComponents/KeyValueEditor';

/**
 * Dialog for downloading a configuration file.
 */
class DownloadDialog extends React.Component {

    downloadButton = React.createRef();

    state = {
        attributes : []
    }

    handleChangeAttribute = (newAttributes) => {
        this.setState({attributes: newAttributes});
    }

    render() {
        return (
            <Dialog
                header={'Download Configuration File'}
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
                <p style={{'margin-bottom': '1em'}}>
                    Enter key/value pairs to download the correlating agent configuration. 
                    <br/> You will get a different result depending on the mapping that fits your input.
                </p>
                <KeyValueEditor 
                    onChange={this.handleChangeAttribute}
                    keyValueArray={this.state.attributes}
                    maxHeight={`300px`}
                />
            </Dialog>
        )
    }

    handleDownload = () => {
        const objForDownload = {};
        this.state.attributes.forEach(pair => {
            objForDownload[pair.key || ''] = pair.value || '';
        })

        if(Object.keys(objForDownload).length !== this.state.attributes.length){
            this.props.showWarningMessage('Invalid Input', 'Certein attribute keys were duplicates and have been omitted.')
        }

        this.props.onHide();
        this.props.downloadConfiguration(objForDownload);
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.downloadButton.current.element.focus();
        }
    }

}

const mapDispatchToProps = {
    downloadConfiguration: agentConfigActions.fetchConfigurationFile,
    showWarningMessage: notificationActions.showWarningMessage,
}

export default connect(null, mapDispatchToProps)(DownloadDialog);