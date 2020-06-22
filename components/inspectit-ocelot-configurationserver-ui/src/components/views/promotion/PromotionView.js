import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import PromotionToolbar from './PromotionToolbar';
import { promotionActions, promotionSelectors } from '../../../redux/ducks/promotion';
import PromotionSidebar from './PromotionSidebar';
import PromotionFileViewer from './PromotionFileView';
import PromotionFileApproval from './PromotionFileApproval';

/** View to display and change mappings */
const PromotionView = () => {
  const dispatch = useDispatch();
  const contentHeight = 'calc(100vh - 7rem)';

  const currentSelection = useSelector(promotionSelectors.getCurrentSelectionFile);
  const fileCount = useSelector(promotionSelectors.getFileCount);
  const updateDate = useSelector((state) => state.promotion.updateDate);

  const fetchPromotionFiles = () => {
    dispatch(promotionActions.fetchPromotions());
  };

  useEffect(fetchPromotionFiles, []);

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
            height: ${contentHeight};
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

      <div className="this">
        <div className="fixed-toolbar">
          <PromotionToolbar />
        </div>
        <div className="content">
          {fileCount > 0 ? (
            <>
              <PromotionSidebar />

              <div className="fileContent">
                {currentSelection ? (
                  <>
                    <PromotionFileViewer />
                    <PromotionFileApproval />
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
                <span>The configuration is up to date. Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</span>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default PromotionView;
