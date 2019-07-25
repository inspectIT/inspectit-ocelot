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
    /** The date when the configuration files have been fetched. */
    updateDate: null,
    /** The (abosolute) path of the currently selected file in the configuration file tree. */
    selection: null
};

const notification = {
    /** The latest notification object. */
    lastNotification: null
};

const mappings = {
    /** Specifies whether the agent mappings are currently being loaded. */
    loading: false,
    /** The current agent mappings. */
    mappings: null
}

export {
    authentication,
    configuration,
    notification,
    mappings
}