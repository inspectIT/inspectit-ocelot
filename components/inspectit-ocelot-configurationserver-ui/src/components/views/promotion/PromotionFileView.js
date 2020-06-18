import React from 'react';
import { useSelector } from 'react-redux';
import { promotionSelectors } from '../../../redux/ducks/promotion';

import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer';

const PromotionFileViewer = () => {
    const currentSelection = useSelector(promotionSelectors.getCurrentSelectionFile);

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
