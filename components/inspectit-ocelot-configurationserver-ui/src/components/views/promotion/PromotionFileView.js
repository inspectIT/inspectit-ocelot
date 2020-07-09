import React, { useEffect, useState } from 'react';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer';

/**
 * The view for showing the diff of the currently selected promotion file.
 */
const PromotionFileViewer = ({ oldValue, newValue }) => {
  const [isLargeScreen, setLargeScreen] = useState(window.innerWidth > 1280);

  const updateMedia = () => {
    setLargeScreen(window.innerWidth > 1280);
  };

  useEffect(() => {
    window.addEventListener('resize', updateMedia);
    return () => window.removeEventListener('resize', updateMedia);
  });

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
          oldValue={oldValue || ''}
          newValue={newValue || ''}
          splitView={isLargeScreen}
          compareMethod={DiffMethod.WORDS_WITH_SPACE}
        />
      </div>
    </>
  );
};

export default PromotionFileViewer;
