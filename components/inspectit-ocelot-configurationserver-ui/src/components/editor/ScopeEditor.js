import {ListBox} from 'primereact/listbox';

import { cloneDeep, isEqual, set, unset } from 'lodash';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { ColumnGroup } from 'primereact/columngroup';
import { InputText } from 'primereact/inputtext';
import { Menubar } from 'primereact/menubar';
import { Message } from 'primereact/message';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React from 'react';
import Scope from './scopeUI/Scope';
import { BreadCrumb } from 'primereact/breadcrumb';

// Faster deep-copy for json style object.

// Features
// Simplified lodash.deepClone, maybe 15 times faster when copy many compilcated objects.
// Only support plain objects with number, string, array, boolean
import deepCopy from 'json-deep-copy'


/**
 * Editor for showing the scopes of the config file as UI
 */
class ScopeEditor extends React.Component {

  state = { 
    showOverview: true,
    breadCrumbItems: [
      { label: 'Scope Overview' },
    ],
    currentlyDisplayScopeName: '',
  }

  componentDidMount(){
    this.addEventListenerToBreadCrumbs();
    this.setScopeNamesFromConfig();
  }

  // { !showOverview && ( <Scope ... to either display the Scopelist or the single scope 
  displayOverview = () => this.setState({ showOverview: true})

  handleOnEdit = () => this.setState({ showOverview: false});

  handleDoubleClick = (e) => {
    const name = e.target.dataset.name;
    const { config } = this.props;

    // TODO: inserting name into scopeObject is obsolete, since onUpdate= (updatedValue) => this.onUpdate(updatedValue, scopeName)
    // // getting the correct single scopeObject to pass as props
    let scopeObject = config.inspectit.instrumentation.scopes[name];
    // scopeObject['scopeName'] = name;

    const breadCrumbItems = [{'label': 'Scope Overview'}, {'label': name }];
    this.setState({ scopeObject, showOverview: false, breadCrumbItems, currentlyDisplayScopeName: name})
  }
      
  setScopeNamesFromConfig = () => {
    // asserting that accessing attributes does not happen on undefined. Since sometimes the inspectIt wasnt passed
    // TODO: hint: the scopeEditor is always shown, for each yml file. thinking about, how to safely access the props.
    // thinking about , if it is empty, it should still display the empty scopelist, and let the user create a "empty scope"
    if( this.props.config && this.props.config.inspectit &&  this.props.config.inspectit.instrumentation && this.props.config.inspectit.instrumentation.scopes) {
      let scopes = this.props.config.inspectit.instrumentation.scopes;
      let scopeNameList = [];
      Object.keys(scopes).map(name => {
        scopeNameList.push({label: name})
      })
      this.setState({scopeNameList})
    }
  }

  // TODO:
  handleBreadCrumbClick = () => {
    this.setState({ breadCrumbItems: [{ label: 'Scope Overview' }]});
    this.displayOverview();
  }

  // TODO: c-important. Please do not use limited time on less important code for the month goal.
  // <BreadCrumb> elements nagivate to other urls. example     { label: 'Lionel Messi', url: 'https://en.wikipedia.org/wiki/Lionel_Messi' }
  // <BreadCrumb> does not enable onClick listener on the elements, this manually adding.
  addEventListenerToBreadCrumbs = () => {
    // Array.from(htmlCollection)
    let breadCrumbArray = Array.from(document.getElementsByClassName('p-breadcrumb p-component')[0].getElementsByClassName('p-menuitem-link'));
    breadCrumbArray.map(element => {
      if( element.innerText === 'Scope Overview' ) {
        element.addEventListener('click', this.handleBreadCrumbClick);
      }
    })
  }

  updateBreadCrumbs = breadCrumbItems => this.setState({ breadCrumbItems});
  
  itemTemplate = (option) => {
    return (
        <div >
          <p data-name={option.label} onDoubleClick={this.handleDoubleClick}> {option.label} </p>
        </div>
    );
  }

  onUpdate = ( updatedValue, scopeName ) => {
    console.log( 'Freezer', config );
    let { onUpdate, config } = this.props;
    // updating the scopeObject in state
    // this variable is being passed down, and must be updated
    // this variable is only setted in the doubleClick elsewise.
    let copyConfig = deepCopy(config)
    this.setState({ scopeObject: updatedValue})

    // TODO: überlegung. Kann bei keinen angelegten scopes überhaupt ...instrumentation.scopes <-- ein Attribute gesetzt werden ? 
    copyConfig.inspectit.instrumentation.scopes[scopeName] = updatedValue;
    onUpdate(copyConfig);
  }

  render() {
    const { config, ...rest } = this.props;
    const { scopeNameList, showOverview, scopeObject, breadCrumbItems, currentlyDisplayScopeName } = this.state;

    // TODO: consistent styling of all toolbarButtonStyles?
    const toolbarButtonStyle = {padding: '5px' , background: 'rgb(139, 172, 189)', margin: '5px'};

    return (
      <div className="this">
        {/* the style here was copied from i believe tree table, it gives good indications about how the page should look like, so i used it */}
        <style jsx>{`
          .this {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            min-width: 760px;
          }
          .this :global(.p-menubar) {
            background-color: #f4f4f4;
          }
          .this :global(.p-menuitem-text) {
            font-size: smaller;
          }
          .this :global(.p-menuitem-icon) {
            font-size: smaller;
          }
          .errorBox {
            align-self: center;
            justify-content: center;
            flex: 1;
            display: flex;
            flex-direction: column;
            color: #bbb;
          }
          .this :global(.composite-row),
          .this :global(.composite-row) :global(.key-column) {
            color: grey;
            font-weight: normal;
          }
          .this :global(.key-column) {
            color: black;
            font-weight: bold;
          }
          .this :global(.value-column) {
            font-family: monospace;
          }
          .this :global(.edit-text) {
            align-self: center;
            align-items: center;
            margin-inline-end: 1em;
          }
          .this :global(.p-treetable .p-treetable-tbody > tr > td.p-cell-editing .p-button) {
            width: unset;
            min-width: 96px;
          }
        `}</style>


        <div>
          <BreadCrumb model={breadCrumbItems} />
          {
            showOverview && (
              <div style={{ marginLeft: '50px', marginTop: '25px'}} className="content-section implementation">
                <h4 >The following scopes exist within the selected file</h4>
                <ListBox filter={true} filterPlaceholder="Search" options={scopeNameList} itemTemplate={this.itemTemplate}
                  style={{width:'500px'}} listStyle={{}}/>
                <div style={{ margin: '25px'}}>
                  <Button onClick={this.createOption} label="create new" style={{ ...toolbarButtonStyle}}> </Button>
                  <Button onClick={this.handleOnEdit} label="edit" style={{ ...toolbarButtonStyle}}> </Button>
                  <Button onDoubleClick={this.deleteOption} label="delete" style={{ ...toolbarButtonStyle}}> </Button>
                </div>
              </div>
          )}
          { !showOverview && (
            <Scope currentlyDisplayScopeName={currentlyDisplayScopeName} scopeObject={scopeObject} updateBreadCrumbs={this.updateBreadCrumbs} onUpdate={(updatedValue) => this.onUpdate(updatedValue, currentlyDisplayScopeName)} />
            )} 
        </div>
      </div>
    );
  }
}

ScopeEditor.propTypes = {
  /** The configuration object */
  config: PropTypes.object,
  /** Function to invoke for full config update */
  onUpdate: PropTypes.func,
};

export default ScopeEditor;
