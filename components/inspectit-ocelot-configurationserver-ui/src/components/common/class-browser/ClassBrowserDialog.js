import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Tree } from 'primereact/tree';
import CBTableNode from './CBTableNode';
import _ from 'lodash';
import { transformClassStructureToTableModel } from './ClassBrowserUtilities';

/**
 * The class browser dialog.
 */
const ClassBrowserDialog = ({ visible, onHide, onSelect }) => {
  const [selectedAgent, setSelectedAgent] = useState(null);
  const [selectedMethod, setSelectedMethod] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [treeModel, setTreeModel] = useState([]);

  const classStructure = [
    { name: 'java.lang.String', type: 'class', methods: ['public String toString()', 'public String equals(java.lang.Object)'] },
    { name: 'java.lang.Runnable', type: 'interface', methods: ['public void run()'] },
    { name: 'org.lang.String', type: 'class', methods: ['public String toString()', 'public String equals(java.lang.Object) public String equals(java.lang.Object)'] },
  ];

  const existingAgent = [
    { label: 'InspectIT Agent (11360@NB171217MO)', value: '11360@NB171217MO' },
    { label: 'InspectIT Agent (21360@NB171217MO)', value: '21360@NB171217MO' },
    { label: 'InspectIT Agent (31360@NB171217MO)', value: '31360@NB171217MO' },
  ];

  // generate table model
  useEffect(() => {
    const result = [];
    _.each(classStructure, (classElement) => transformClassStructureToTableModel(result, classElement));
    setTreeModel(result);
  }, []);

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Select" onClick={() => onSelect()} disabled={!selectedMethod} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
    </div>
  );

  const tableNodeTemplate = ({ label, type, key }) => {
    return <CBTableNode label={label} type={type} value={key} onChange={setSelectedMethod} selectedMethod={selectedMethod} />;
  };

  return (
    <>
      <style jsx>{`
        .content {
          display: flex;
          flex-direction: column;
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
          height: 20rem;
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
      `}</style>

      <Dialog
        className="class-browser-dialog"
        header="Class Browser"
        visible={visible}
        style={{ width: '40rem' }}
        onHide={onHide}
        blockScroll
        footer={footer}
      >
        <div className="content">
          <Dropdown
            value={selectedAgent}
            options={existingAgent}
            onChange={(e) => {
              setSelectedAgent(e.value);
            }}
            placeholder="Select an Agent"
          />

          <div className="search-box p-inputgroup">
            <InputText value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="org.example.MyClass" />
            <Button label="Search" />
          </div>

          <Tree value={treeModel} nodeTemplate={tableNodeTemplate} />
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
  onApply: () => {},
};

export default ClassBrowserDialog;
