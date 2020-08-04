import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import dateformat from 'dateformat';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

class HistoryView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            visibleLeft: true,
            selectedCommit: this.props.data[0],
            show: false,
        }
    }

    selectionChange(selectedCommit) {
        this.setState({
            selectedCommit: selectedCommit
        });
        this.props.commitChangesFunction(selectedCommit);
    }

    showHistoryView = () => {
        this.setState({
            show: !this.state.show
        })
    }

    render() {
        return (
            <div className="this" >
                <style jsx>
                    {`
                        .this :global(.p-toolbar) {
                        border: 0;
                        border-radius: 0;
                        background-color: #eee;
                        border-bottom: 1px solid #ddd;
                        }
                    `}
                </style>
                <Toolbar>
                    <Button onClick={()=>this.showHistoryView()} icon="pi pi-bars"></Button>
                </Toolbar>
                {this.state.show ? (
                    <div style={{ width: 550 }}>
                        <DataTable value={this.props.data} rowHover selection={this.state.selectedCommit} onSelectionChange={e => this.selectionChange(e.value)} >
                            <Column selectionMode="single" style={{ width: '3em' }} />
                            <Column field="name" header='Name' />
                            <Column field="id" header='Id' />
                            <Column field="author" header='Author' />
                            <Column field="date"
                                header='Date'
                                body={(data) => (data.date === 0 ? '-' : dateformat(data.date, 'yyyy-mm-dd HH:MM'))}
                            />
                        </DataTable>
                    </div>

                ) : null}
            </div>

        )
    }
}



export default HistoryView;