import * as types from "./types";

export const showSuccessMessage = (summary, detail) => ({
    type: types.SHOW,
    payload: {
        severity: 'success',
        summary,
        detail
    }
});

export const showWarningMessage = (summary, detail) => ({
    type: types.SHOW,
    payload: {
        severity: 'warn',
        summary,
        detail
    }
});