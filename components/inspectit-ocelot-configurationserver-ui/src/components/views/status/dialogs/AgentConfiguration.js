import React from 'react';
import { Dialog } from 'primereact/dialog';
import { connect } from 'react-redux';
import { InputTextarea } from 'primereact/inputtextarea'
import { Button } from 'primereact/button';

class AgentConfiguration extends React.Component {

    render() {
        return (
            <Dialog
                header={'Agent Configuration'}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={
                    <div>
                        <Button label="Download" className="p-button-primary" onClick={this.handleDownload} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.handleClose} />
                    </div>
                }
            >
                <InputTextarea
                    value={"Hello World"}
                />
            </Dialog>
        );
    }

    /**
     * Closing dialog.
     */
    handleClose = (success = true) => {
        if (success) {
            this.props.onHide();
        }
    };

    handleDownload = () => {

        // todo
        console.log("download")
    }

    onChange = (value) => {
        console.log(value);
    }
}

const mapDispatchToProps = {

};

export default connect(null, mapDispatchToProps)(AgentConfiguration);