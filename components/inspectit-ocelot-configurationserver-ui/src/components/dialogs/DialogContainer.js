import React from 'react';
import PromotionConflictDialog from './promotions/PromotionConflictDialog';
import PromotionApprovalDialog from './promotions/PromotionApprovalDialog';

const DialogContainer = () => {
    return (
        <>
            <PromotionConflictDialog />
            <PromotionApprovalDialog />
        </>
    );
};

export default DialogContainer;