import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import PromotionToolbar from './PromotionToolbar';
import { promotionActions, promotionSelectors } from '../../../redux/ducks/promotion';
import PromotionSidebar from './PromotionSidebar';
import PromotionFileViewer from './PromotionFileView';
import PromotionFileApproval from './PromotionFileApproval';
import useFetchData from '../../../hooks/use-fetch-data';
import usePrevious from '../../../hooks/use-previous';
import PromotionApprovalDialog from './dialogs/PromotionApprovalDialog';
import axios from '../../../lib/axios-api';
import PromotionConflictDialog from './dialogs/PromotionConflictDialog';

/**
 * The view for displaying existing promotion files including their modifications.
 */
const PromotionView = () => {
  const dispatch = useDispatch();

  const [currentSelection, setCurrentSelection] = useState(null);
  const [showPromotionDialog, setShowPromotionDialog] = useState(false);
  const [showConflictDialog, setShowConflictDialog] = useState(false);

  const currentApprovals = useSelector((state) => state.promotion.approvals);
  const workspaceCommitId = useSelector((state) => state.promotion.workspaceCommitId);
  const liveCommitId = useSelector((state) => state.promotion.liveCommitId);
  const canCommit = useSelector((state) => state.authentication.permissions.commit);

  const [{ data, isLoading, isError, lastUpdate }, refreshData] = useFetchData('/configuration/promotions', { 'include-content': 'true' });
  const entries = data ? data.entries : [];

  const currentSelectionFile = currentSelection ? _.find(entries, { file: currentSelection }) : null;
  

  useEffect(() => {
    if (data) {
      dispatch(promotionActions.updateCommitIds(data.workspaceCommitId, data.liveCommitId));
    }
  }, [data]);

  const fileCount = Array.isArray(entries) ? entries.length : 0;
  const hasApprovals = !_.isEmpty(currentApprovals);
  const isCurrentSelectionApproved = currentApprovals.includes(currentSelection);

  const promotionFiles = _.map(entries, (element) => {
    return {
      ...element,
      isApproved: currentApprovals.includes(element.file),
    };
  });

  const toggleFileApproval = () => {
    if (isCurrentSelectionApproved) {
      dispatch(promotionActions.disapproveFile(currentSelection));
    } else {
      dispatch(promotionActions.approveFile(currentSelection));
    }
  };

  const promoteConfigurations = async () => {
    const payload = {
      files: currentApprovals,
      workspaceCommitId,
      liveCommitId,
    };

    try {
      const result = await axios.post('/configuration/promote', payload);

      refreshData();
    } catch (error) {
      if (error.response.status === 409) {
        setShowConflictDialog(true);
      }
    }
  };

  const promoteFiles = () => {
    promoteConfigurations();
    setShowPromotionDialog(false);
  };

  return (
    <>
      <style jsx>
        {`
          .fixed-toolbar {
            position: fixed;
            top: 4rem;
            width: calc(100vw - 4rem);
          }
          .content {
            margin-top: 3rem;
            height: calc(100vh - 7rem);
            overflow: hidden;
            display: flex;
          }
          .fileContent {
            height: 100%;
            flex-grow: 1;
            display: flex;
            flex-direction: column;
          }
          .selection-information {
            display: flex;
            flex-grow: 1;
            height: 100%;
            align-items: center;
            justify-content: center;
            color: #bbb;
          }
        `}
      </style>

      <PromotionApprovalDialog
        visible={showPromotionDialog}
        onHide={() => setShowPromotionDialog(false)}
        onPromote={promoteFiles}
        approvedFiles={currentApprovals}
      />

      <PromotionConflictDialog visible={showConflictDialog} onHide={() => setShowConflictDialog(false)} onRefresh={refreshData} />

      <div className="this">
        <div className="fixed-toolbar">
          <PromotionToolbar
            onRefresh={refreshData}
            onPromote={() => setShowPromotionDialog(true)}
            loading={isLoading}
            enabled={canCommit && hasApprovals}
          />
        </div>
        <div className="content">
          {fileCount > 0 ? (
            <>
              <PromotionSidebar
                promotionFiles={promotionFiles}
                selection={currentSelectionFile}
                onSelectionChange={setCurrentSelection}
                updateDate={lastUpdate}
              />

              <div className="fileContent">
                {currentSelectionFile ? (
                  <>
                    <PromotionFileViewer oldValue={currentSelectionFile.oldContent} newValue={currentSelectionFile.newContent} />
                    {canCommit && <PromotionFileApproval approved={isCurrentSelectionApproved} onApproveFile={toggleFileApproval} />}
                  </>
                ) : (
                  <div className="selection-information">
                    <span>Select a file to start.</span>
                  </div>
                )}
              </div>
            </>
          ) : (
            <>
              <div className="selection-information">
                <span>The configuration is up to date. Last refresh: {lastUpdate ? new Date(lastUpdate).toLocaleString() : '-'}</span>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default PromotionView;
