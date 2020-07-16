const authentication = {
  /** The access token which is used for bearer authentication. */
  token: null,
  /** The authorization permissions the user has*/
  permissions: {
    write: false,
    promote: false,
    admin: false,
  },
  /** Specifying whether a login request is currently be executed */
  loading: false,
  /** This contains the error message if the last request has failed. */
  error: null,
  /** The username of the currently logged in user. */
  username: null,
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
  moveHistory: [
    /* {source, target} */
  ],
  /** The date when the configuration files have been fetched. */
  updateDate: null,
  /** The (abosolute) path of the currently selected file in the configuration file tree. */
  selection: null,
  /** The content of the currently selected file. */
  selectedFileContent: null,
  /**
   * A map of unsaved file changes.
   * Maps the absolute file path to the files contents.
   */
  unsavedFileContents: {
    /* fileName: fileContents */
  },
  /** The default configuration of the Ocelot agents. Will be retrieved as key/value pairs each representing path/content of a file. */
  defaultConfig: {},
  /** The path of the currently selected default configuration file in the file tree. */
  selectedDefaultConfigFile: null,
  /** The configuration schema */
  schema: null,
  /** If the config view should be in the visual mode */
  showVisualConfigurationView: false,
};

const notification = {
  /** The latest notification object. */
  lastNotification: null,
};

const mappings = {
  /** Specifies how many requests are currently loading in the background */
  pendingRequests: 0,
  /** The current agent mappings. */
  mappings: [],
  /** The date when the agent mappings have been fetched. */
  updateDate: null,
};

const alerting = {
  /** A mapping of rule names and corresponding unsaved contents.*/
  unsavedRuleContents: {},
  ruleGrouping: {
    groupByTemplates: true,
    groupByTopics: false,
  },
};

const agentStatus = {
  /**
   * The list of connected agents, each agent is an object with the following structure:
   * {attributes: (dictionary), mappingName: string, lastConfigFetch: timestamp}
   */
  agents: [],
  /** Specifies how many requests are currently loading the data in the background */
  pendingRequests: 0,
  /** Specifies how many requests are currently clearing the data in the background */
  pendingClearRequests: 0,
};

const settings = {
  /** The list of users */
  users: [],
  /** Specifies how many requests are currently loading the data in the background */
  pendingRequests: 0,
};

const promotion = {
  /** The names of the files which are currently be approved */
  approvals: [],
  /** The commit id of the working directory related to the currently changed files */
  workspaceCommitId: null,
  /** The commit id of the live directory related to the currently changed files */
  liveCommitId: null,
};

export { authentication, configuration, notification, mappings, agentStatus, settings, promotion, alerting };
