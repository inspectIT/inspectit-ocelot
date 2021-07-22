import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import PromotionToolbar from './PromotionToolbar';
import { promotionActions } from '../../../redux/ducks/promotion';
import { notificationActions } from '../../../redux/ducks/notification';
import PromotionSidebar from './PromotionSidebar';
import PromotionFileViewer from './PromotionFileView';
import PromotionFileApproval from './PromotionFileApproval';
import useFetchData from '../../../hooks/use-fetch-data';
import PromotionApprovalDialog from './dialogs/PromotionApprovalDialog';
import axios from '../../../lib/axios-api';
import PromotionConflictDialog from './dialogs/PromotionConflictDialog';
import WarningDialog from '../../common/dialogs/WarningDialog';
import _ from 'lodash';
import { ProgressSpinner } from 'primereact/progressspinner';

/**
 * The view for displaying existing promotion files including their modifications.
 */
const PromotionView = () => {
  const dispatch = useDispatch();

  // state variables
  const [currentSelection, setCurrentSelection] = useState(null); // the current selected file name
  const [showPromotionDialog, setShowPromotionDialog] = useState(false);
  const [showConflictDialog, setShowConflictDialog] = useState(false);
  const [showWarningDialog, setShowWarningDialog] = useState(false);
  const [promotionResult, setPromotionResult] = useState(null);
  const [isPromoting, setIsPromoting] = useState(false);

  // global state variables
  const currentApprovals = useSelector((state) => state.promotion.approvals);
  const workspaceCommitId = useSelector((state) => state.promotion.workspaceCommitId);
  const liveCommitId = useSelector((state) => state.promotion.liveCommitId);
  const canPromote = useSelector((state) => state.authentication.permissions.promote);
  const currentUser = useSelector((state) => state.authentication.username);

  // fetching promotion data
  const [{ data, isLoading, lastUpdate }, refreshData] = useFetchData('/configuration/promotions', { 'include-content': 'true' });

  // derived variables
  const canSelfApprove = _.get(data, 'canPromoteOwnChanges', false); // whether the user can approve self-made changes
  const entries = _.get(data, 'entries', []); // all modified files
  const currentSelectionFile = currentSelection ? _.find(entries, { file: currentSelection }) : null; // the current selected file object
  const authors = _.get(currentSelectionFile, 'authors', []); // list of users which modified the selected file
  const hasApprovals = !_.isEmpty(currentApprovals); // if more at least one file is approved and waiting for promotion
  const isCurrentSelectionApproved = currentApprovals.includes(currentSelection); // whether the current selected file is approved
  const entriesWithApproval = _.map(entries, (element) => {
    return {
      ...element,
      isApproved: currentApprovals.includes(element.file),
      canSelfApprove,
      currentUser,
      canPromote,
    };
  }); // copy of the entries array including the data whether a file is approved or not.

  // initially load data
  useEffect(() => refreshData(), []);

  // updating commit ids in the global state using the latest data
  useEffect(() => {
    if (data) {
      dispatch(promotionActions.updateCommitIds(data.workspaceCommitId, data.liveCommitId));
    }
  }, [data]);

  /**
   * Toggles the approval state of the current selected file.
   */
  const toggleFileApproval = () => {
    if (isCurrentSelectionApproved) {
      dispatch(promotionActions.disapproveFile(currentSelection));
    } else {
      dispatch(promotionActions.approveFile(currentSelection));
    }
  };

  /**
   * Promotes the currently approved files.
   */
  const promoteConfigurations = async (commitMessage) => {
    const payload = {
      files: currentApprovals,
      workspaceCommitId,
      liveCommitId,
      commitMessage,
    };

    setIsPromoting(true);

    try {
      const { data } = await axios.post('/configuration/promote', payload);

      if (data.result && data.result != 'OK') {
        setShowWarningDialog(true);
        setPromotionResult(data.result);
      }

      dispatch(
        notificationActions.showSuccessMessage('Configuration Promoted', 'The approved configurations have been successfully promoted.')
      );
      refreshData();
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setShowConflictDialog(true);
      }
    }

    setIsPromoting(false);
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
            flex-direction: column;
            height: 100%;
            align-items: center;
            justify-content: center;
            color: #bbb;
          }
          .selection-information > span {
            margin-bottom: 1rem;
          }
          .promotion-result {
            font-family: monospace;
          }
        `}
      </style>

      <PromotionApprovalDialog
        visible={showPromotionDialog}
        onHide={() => setShowPromotionDialog(false)}
        onPromote={promoteConfigurations}
        approvedFiles={currentApprovals}
        isLoading={isPromoting}
      />

      <PromotionConflictDialog visible={showConflictDialog} onHide={() => setShowConflictDialog(false)} onRefresh={refreshData} />

      <WarningDialog visible={showWarningDialog} onHide={() => setShowWarningDialog(false)}>
        <p>
          The promotion was successful, but there was an associated problem.
          <br />
          Please contact your administrator if there are any further problems.
        </p>
        <p>
          Result: <span className="promotion-result">{promotionResult}</span>
        </p>
      </WarningDialog>

      <div className="this">
        <div className="fixed-toolbar">
          <PromotionToolbar
            onRefresh={refreshData}
            onPromote={() => setShowPromotionDialog(true)}
            loading={isLoading}
            enabled={hasApprovals}
            canPromote={canPromote}
          />
        </div>
        <div className="content">
          {entries.length > 0 ? (
            <>
              <PromotionSidebar
                promotionFiles={entriesWithApproval}
                selection={currentSelectionFile}
                onSelectionChange={setCurrentSelection}
                updateDate={lastUpdate}
              />

              <div className="fileContent">
                {currentSelectionFile ? (
                  <>
                    <PromotionFileViewer oldValue={currentSelectionFile.oldContent} newValue={currentSelectionFile.newContent} />

                    <PromotionFileApproval
                      currentUser={currentUser}
                      authors={authors}
                      canApprove={canPromote}
                      approved={isCurrentSelectionApproved}
                      onApproveFile={toggleFileApproval}
                      allowSelfApproval={canSelfApprove}
                    />
                  </>
                ) : (
                  <div className="selection-information">
                    <span>Select a file to start.</span>
                  </div>
                )}
              </div>
            </>
          ) : isLoading ? (
            <>
              <div className="selection-information">
                <span>Loading promotion files...</span>
                <ProgressSpinner />
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
