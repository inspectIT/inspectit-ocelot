import React from 'react';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import ClearDialog from "./dialogs/ClearDialog";
import { connect } from 'react-redux'

class StatusToolbar extends React.Component {

    state = {
        isClearDialogShown : false
    }

    render() {
        const { clearing, clearStatus, filter, onFilterChange } = this.props;
        return (
            <div className="this">
                <style jsx>{`
                    .this :global(.p-toolbar) {
                        background: 0;
                        border: 0;
                        border-radius: 0;
                        background-color: #eee;
                        border-bottom: 1px solid #ddd;
                    }
                    .p-toolbar-group-right > :global(*) {
                        margin-left: .25rem;
                    }
                `}</style>
                <Toolbar>
                    <div className="p-toolbar-group-left">
                        <div className="p-inputgroup" style = {{display: "inline-flex", verticalAlign: "middle"}}>
                            <span className="pi p-inputgroup-addon pi-search" />
                            <InputText
                                onKeyPress={this.onKeyPress}
                                style={{width: "300px"}}
                                value={filter}
                                placeholder={"Filter Agents"}
                                onChange={(e) => onFilterChange(e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="p-toolbar-group-right">
                        <Button disabled={clearing} onClick={() => this.setState({isClearDialogShown: true})} label="Clear All" />
                    </div>
                </Toolbar>
                <ClearDialog visible={this.state.isClearDialogShown} onHide={() => this.setState({isClearDialogShown: false})}/>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { pendingClearRequests } = state.agentStatus;
    return {
        clearing: pendingClearRequests > 0,
    }
}

export default connect(mapStateToProps)(StatusToolbar);