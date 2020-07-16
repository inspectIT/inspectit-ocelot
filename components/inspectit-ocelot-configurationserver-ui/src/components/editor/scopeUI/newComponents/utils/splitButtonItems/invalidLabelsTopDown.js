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
    invalidActionId: 'adding type',
    invalidAttributes: 'type',
    affectedAttributes: [
      '_class'
    ],
    invalidLabelText: 'only 1 class name specification',
  },
  {
    invalidActionId: 'adding superclass',
    invalidAttributes: 'superclass',
    affectedAttributes: [
      '_class'
    ],
    invalidLabelText: 'only 1 superclass specification',
  },
  {
    invalidActionId: 'adding visibility',
    invalidAttributes: 'visibility',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'only 1  visibility option allowed',
  },
  {
    invalidActionId: 'adding arguments',
    invalidAttributes: 'arguments',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'only 1  arguments option allowed',
  },
  {
    invalidActionId: 'adding is-synchronized',
    invalidAttributes: 'is-synchronized',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'only 1 is-synchronized option allowed',
  },
  {
    invalidActionId: 'adding is-constructor',
    invalidAttributes: 'is-constructor',
    affectedAttributes: [
      'method',
    ],
    invalidLabelText: 'only 1  is-constructor option allowed',
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
