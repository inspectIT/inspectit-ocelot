import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Tree } from 'primereact/tree';
import { RadioButton } from 'primereact/radiobutton';
import _ from 'lodash';

/**
 * The class browser dialog.
 */
const ClassBrowserDialog = ({ visible, onHide, onApply }) => {
  const [selectedAgent, setSelectedAgent] = useState(null);
  const [selectedMethod, setSelectedMethod] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [treeModel, setTreeModel] = useState([]);

  const agents = [
    {
      metaInformation: {
        agentId: 'sample-agent',
      },
      attributes: { service: 'service-name' },
    },
  ];

  const classStructure = [
    { name: 'java.lang.String', type: 'class', methods: ['public String toString()', 'public String equals(java.lang.Object)'] },
    { name: 'java.lang.Runnable', type: 'interface', methods: ['public void run()'] },
    { name: 'org.lang.String', type: 'class', methods: ['public String toString()', 'public String equals(java.lang.Object)'] },
  ];

  const existingAgent = [
    { label: 'InspectIT Agent (11360@NB171217MO)', value: '11360@NB171217MO' },
    { label: 'InspectIT Agent (21360@NB171217MO)', value: '21360@NB171217MO' },
    { label: 'InspectIT Agent (31360@NB171217MO)', value: '31360@NB171217MO' },
  ];

  useEffect(() => {
    const result = [];

    _.each(classStructure, ({ name, type, methods }) => {
      const packages = name.split('.');
      const className = packages.pop();

      // find target package
      let targetNode = result;
      let packagePath;
      packages.forEach((packageName) => {
        let target;
        if (targetNode === result) {
          target = _.find(targetNode, { label: packageName });
        } else {
          target = _.find(targetNode.children, { label: packageName });
        }

        if (packagePath) {
          packagePath += '.' + packageName;
        } else {
          packagePath = packageName;
        }

        if (!target) {
          target = {
            key: packagePath,
            label: packageName,
            type: 'package',
            children: [],
            selectable: false,
          };

          if (targetNode === result) {
            result.push(target);
          } else {
            targetNode.children.push(target);
          }
        }

        targetNode = target;
      });

      // add type
      const typeNode = {
        key: name,
        label: className,
        type,
        children: [],
        selectable: false,
      };
      targetNode.children.push(typeNode);

      // add methods
      methods.forEach((method) => {
        typeNode.children.push({
          key: name + ': ' + method,
          label: method,
          type: 'method',
        });
      });
      console.log(result);
    });

    setTreeModel(result);
    console.log(result);
  }, []);

  const nodes = [
    {
      key: 'java',
      label: 'java',
      type: 'package',
      children: [
        { key: '0-0', label: 'Getting Started', url: 'https://reactjs.org/docs/getting-started.html' },
        { key: '0-1', label: 'Add React', url: 'https://reactjs.org/docs/add-react-to-a-website.html' },
        { key: '0-2', label: 'Create an App', url: 'https://reactjs.org/docs/create-a-new-react-app.html' },
        { key: '0-3', label: 'CDN Links', url: 'https://reactjs.org/docs/cdn-links.html' },
      ],
    },
  ];

  const nodeTemplate = ({ label, type, key }) => {
    if (type === 'package') {
      return (
        <>
          <style jsx>{`
            i {
              margin-right: 0.5rem;
              color: gray;
            }
            span {
              font-family: monospace;
            }
          `}</style>
          <i className="pi pi-folder"></i> <span>{label}</span>
        </>
      );
    } else {
      let typeIcon;
      let typeClass;
      let selectionButton;
      if (type === 'class') {
        typeIcon = 'c';
        typeClass = 'theme-class';
      } else if (type === 'interface') {
        typeIcon = 'i';
        typeClass = 'theme-interface';
      } else if (type === 'method') {
        typeIcon = 'm';
        typeClass = 'theme-method';

        selectionButton = <RadioButton value={key} onChange={(e) => setSelectedMethod(e.value)} checked={selectedMethod === key} />;
      }

      return (
        <>
          <style jsx>{`
            .theme-class {
              background-color: #719cbb;
            }
            .theme-interface {
              background-color: #55ab73;
            }
            .theme-method {
              background-color: #c855f7;
            }
            .type-icon {
              border-radius: 50%;
              width: 1rem;
              height: 1rem;
              text-align: center;
              color: white;
              font-size: 0.75rem;
              margin-right: 0.5rem;
              font-family: monospace;
              line-height: 1rem;
            }
            span {
              flex-grow: 1;
            }
          `}</style>
          <div className={'type-icon ' + typeClass}>{typeIcon}</div>
          <span>{label}</span>
          {selectionButton}
        </>
      );
    }
  };

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Select" onClick={() => onApply()} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
    </div>
  );

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
          height: 20rem;
          margin-top: 0.5rem;
        }

        .content :global(.p-treenode-content) {
          display: flex;
        }
        .content :global(.p-treenode-label) {
          flex-grow: 1;
          align-items: center;
          display: flex;
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

          <Tree value={treeModel} nodeTemplate={nodeTemplate} />
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
