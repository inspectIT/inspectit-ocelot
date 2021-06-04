import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Tree } from 'primereact/tree';
import { ProgressBar } from 'primereact/progressbar';
import CBTableNode from './CBTableNode';
import _ from 'lodash';
import { transformClassStructureToTableModel, transformAgentStatuses } from './ClassBrowserUtilities';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import useFetchData from '../../../hooks/use-fetch-data';

/**
 * The class browser dialog.
 */
const ClassBrowserDialog = ({ visible, onHide, onSelect }) => {
  const dispatch = useDispatch();

  // state variables
  const [selectedAgent, setSelectedAgent] = useState(null);
  const [selectedMethodKey, setSelectedMethodKey] = useState(null);
  const [selectedMethod, setSelectedMethod] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [treeModel, setTreeModel] = useState([]);

  // global state variables
  const agentStatuses = useSelector((state) => state.agentStatus.agents);
  const loadingAgents = useSelector((state) => state.agentStatus.pendingRequests) > 0;

  // fetching the search results
  const [{ data: searchResult, isLoading: isSearching }, executeSearch] = useFetchData('/command/list/classes', {
    query: searchQuery,
    'agent-id': selectedAgent,
  });

  // derived variables
  const existingAgents = !loadingAgents ? transformAgentStatuses(agentStatuses) : [];
  const hasData = !!searchResult && !_.isEmpty(searchResult);

  // refreshing the agents list
  const refreshAgents = () => {
    // fetch agents
    if (!loadingAgents) {
      dispatch(agentStatusActions.fetchStatus());
    }
  };

  // loading the agents once the dialog is shown
  useEffect(() => {
    if (visible) {
      refreshAgents();
    }
  }, [visible]);

  // generate table model
  useEffect(() => {
    const result = [];
    _.each(searchResult, (classElement) => transformClassStructureToTableModel(result, classElement));
    setTreeModel(result);

    setSelectedMethod(null);
    setSelectedMethodKey(null);
  }, [searchResult]);

  // when the selection of the currelty selected method changes
  const onSelectionChange = ({ value, label, parent }) => {
    setSelectedMethodKey(value);
    setSelectedMethod({ label, parent });
  };

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Select" onClick={() => onSelect(selectedMethod)} disabled={!selectedMethod || isSearching} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
    </div>
  );

  // the template for the tree nodes
  const tableNodeTemplate = ({ label, type, key, parent }) => {
    return (
      <CBTableNode label={label} type={type} value={key} onChange={onSelectionChange} selectedMethod={selectedMethodKey} parent={parent} />
    );
  };

  return (
    <>
      <style jsx>{`
        .content {
          display: flex;
          flex-direction: column;
        }

        .agent-box :global(.p-dropdown) {
          flex-grow: 1;
        }

        .search-box {
          display: flex;
          margin-top: 0.5rem;
        }
        .search-box :global(.p-inputtext) {
          flex-grow: 1;
          font-family: monospace;
        }

        .content :global(.p-tree) {
          width: 100%;
          margin-top: 0.5rem;
        }
        .content :global(.p-tree-container) {
          min-height: 20rem;
          height: 40vh;
        }
        .content :global(.p-treenode-content) {
          display: flex;
        }
        .content :global(.p-tree .p-tree-container .p-treenode .p-treenode-content .p-treenode-label) {
          flex-grow: 1;
          align-items: center;
          display: flex;
          overflow: hidden;
        }

        .select-agent-hint {
          height: 40vh;
          margin-top: 0.5rem;
          display: flex;
          align-items: center;
          justify-content: center;
          color: gray;
        }

        .content :global(.p-hidden-accessible) {
          display: none;
        }
      `}</style>

      <Dialog
        className="class-browser-dialog"
        header="Class Browser"
        visible={visible}
        style={{ width: '50rem' }}
        onHide={onHide}
        blockScroll
        footer={footer}
      >
        <div className="content">
          <div className="agent-box p-inputgroup">
            <span className="p-inputgroup-addon">Agent</span>
            <Dropdown
              value={selectedAgent}
              options={existingAgents}
              onChange={(e) => {
                setSelectedAgent(e.value);
              }}
              placeholder="Select an Agent"
              disabled={loadingAgents || isSearching}
            />
            <Button
              icon={'pi pi-refresh' + (loadingAgents ? ' pi-spin' : '')}
              onClick={refreshAgents}
              disabled={loadingAgents || isSearching}
            />
          </div>

          <ProgressBar mode="indeterminate" style={{ height: '4px', visibility: loadingAgents ? '' : 'hidden' }} />

          <div className="search-box p-inputgroup">
            <InputText
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="org.example.MyClass"
              disabled={loadingAgents || !selectedAgent || isSearching}
            />
            <Button label="Search" disabled={loadingAgents || !selectedAgent || isSearching} onClick={executeSearch} />
          </div>

          <ProgressBar mode="indeterminate" style={{ height: '4px', visibility: isSearching ? '' : 'hidden' }} />

          {!selectedAgent || !hasData ? (
            <div className="select-agent-hint">
              <span>{!selectedAgent ? 'Please select an agent.' : 'The search did not return any results.'}</span>
            </div>
          ) : (
            <Tree value={treeModel} nodeTemplate={tableNodeTemplate} />
          )}
        </div>
      </Dialog>
    </>
  );
};

ClassBrowserDialog.propTypes = {
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  /** Callback on dialog apply */
  onApply: PropTypes.func,
};

ClassBrowserDialog.defaultProps = {
  visible: true,
  onHide: () => {},
  onSelect: () => {},
};

export default ClassBrowserDialog;
