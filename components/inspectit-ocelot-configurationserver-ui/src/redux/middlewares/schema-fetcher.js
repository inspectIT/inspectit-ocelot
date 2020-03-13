import { REHYDRATE } from "redux-persist";
import { isAuthenticated } from "../ducks/authentication/selectors";
import { FETCH_TOKEN_SUCCESS } from "../ducks/authentication/types";
import { fetchConfigurationSchema } from "../ducks/configuration/actions";

const schemaFetcher = store => nextDispatch => action => {
  // fire original action first
  const result = nextDispatch(action);

  // fetch schema if
  // - we have succesfull token fetch (login)
  // - or we came from rehydration and are authenticated
  if (action.type === FETCH_TOKEN_SUCCESS) {
    nextDispatch(fetchConfigurationSchema());
  } else if (action.type === REHYDRATE && isAuthenticated(store.getState())) {
    nextDispatch(fetchConfigurationSchema());
  }

  return result;
}

export default schemaFetcher;