import React from 'react';
import { connect, useSelector } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { Toolbar } from 'primereact/toolbar';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';

import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
const PromotionFileViewer = () => {
  const currentSelection = useSelector((state) => state.promotion.currentSelection);

  return (
    <>
      <style jsx>{`
        .this {
            flex-grow: 1;
            overflow-y: auto;
        }
      `}</style>

    <div className="this">
      <ReactDiffViewer
        leftTitle="Live Configuration"
        rightTitle="Workspace Configuration"
        oldValue={currentSelection.oldContent || ''}
        newValue={currentSelection.newContent || ''}
        splitView={true}
        compareMethod={DiffMethod.WORDS_WITH_SPACE}
      />
      </div>
    </>
  );
};

export default PromotionFileViewer;
