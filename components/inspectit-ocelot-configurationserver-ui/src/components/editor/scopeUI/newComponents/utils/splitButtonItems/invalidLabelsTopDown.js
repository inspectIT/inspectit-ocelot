export const helper_invalidActions = [
  {
    invalidActionId: 'adding name',
    invalidAttributes: 'name',
    affectedAttributes: [
      'type',
      'interfaces',
      'method',
      'superclass',
    ],
    invalidLabelText: 'only 1 name specification',
  },
  {
    invalidActionId: 'adding visibility',
    invalidAttributes: 'visibility',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'visibility specification does already exist for this selector',
  },
  {
    invalidActionId: 'adding arguments',
    invalidAttributes: 'arguments',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'arguments specification does already exist for this selector',
  },
  {
    invalidActionId: 'adding is-synchronized',
    invalidAttributes: 'is-synchronized',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'is-synchronized specification does already exist for this selector',
  },
  {
    invalidActionId: 'adding is-constructor',
    invalidAttributes: 'is-constructor',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'is-constructor specification does already exist for this selector',
  },
]



  // component context is required to know existing item.attributes
  // Iteration through each entry
export const splittButtonItemIsInvalid = ( item , splittButtonItem) => {
  let result = false;
  helper_invalidActions.map(invalidAction => {
    if (splittButtonItem.actionId === invalidAction.invalidActionId ) { // eine Action stimmt überrein. Disabele das splitMenuItem, wenn eine restriction verletzt wurde.
      Object.keys(item).map( attribute => {
        if (invalidAction.invalidAttributes.includes(attribute)) {
          result = invalidAction  // generic. The condition has been meet, the required informationObject is returned for following actions
        } 
      })
    }
  })
  return result;
}

export const adjustInvalidSplitButtonItem = (splittButtonItem , invalidAction) => {
  splittButtonItem.disabled = true;
  delete splittButtonItem.command; // removing command, since disable only greys the entry of the splitMenu out, PrimeReact fault
  if (invalidAction && splittButtonItem) { splittButtonItem.label = invalidAction.invalidLabelText;}
  return splittButtonItem;
}
