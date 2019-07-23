const authentication = {
    /** The access token which is used for bearer authentication. */
    token: null,
    /** Specifying whether a login request is currently be executed */
    loading: false,
    /** This contains the error message if the last request has failed. */
    error: null,
    /** The username of the currently logged in user. */
    username: null
};

const configuration = {
    /** Specifies how many requests are currently loading in the background */
    pendingRequests: 0,
    /** The existing configuration files. */
    files: [],
    /**
     * The history of file-move operations which were executed successfully.
     * When the files are refetched, it can occur that the selection points to a non existing file.
     * Normally, the selection is then simply removed.
     * With the moveHistory, it is possible to corret the selection, in case the selected file was moved.
     * The moveHistory is cleared after a files have been refetched.
     */
    moveHistory: [/*{soruce, target}*/],
    /** The date when the configuration files have been fetched. */
    updateDate: null,
    /** The (abosolute) path of the currently selected file in the configuration file tree. */
    selection: null
};

const notification = {
    /** The latest notification object. */
    lastNotification: null
};

export {
    authentication,
    configuration,
    notification
}