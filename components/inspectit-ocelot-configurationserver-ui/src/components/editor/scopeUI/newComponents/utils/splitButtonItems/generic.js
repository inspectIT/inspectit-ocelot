
  // gesamteinheilticher Prozess der Erstellung der splittButtonItems. Die Items werden angefragt, die createAttribute wird ermöglicht
  // Zusätzlich können jetzt sinnvolle Prozessschritte gechained werden. Wie z.B splittButtonItemIsInvalid, welches prüft, ob das Item für ein Attribute
  // überhaupt erlaubt ist im diesem Kontext. Das Beispiel für type.name würde heißen, dass das Item "create name Attribute" disabeled werden muss.
  // Bemerke das Attribute disabeled im item zu setzen graut das Item zwar aus, deaktiviert aber nicht die .command / createAttribute Logik, diese muss manuel annuliert werden.
  createSplitButtonItems = () => {
    const { parentAttribute, item, onUpdate } = this.props; 
    let splittButtonItems = getSplitButtonsItems(parentAttribute, item); // .command key must be added + item must be passed to createAttribute
    splittButtonItems = this.enableCreateAttributeWithinSplitItemEntries(splittButtonItems);
    
    // adjusting the single items
    splittButtonItems.map(splittButtonItem => {
      if(this.splittButtonItemIsInvalid(splittButtonItem)) splittButtonItem = this.adjustInvalidSplitButtonItem(splittButtonItem);
    })
  }


  // prüft für ein Attribut, ob eine Restriction verletzt wurde, deshalb das item für dieses attribute innerhalb des splittButton nicht erlaubt ist 
  // und adjustierst dieses
  splittButtonItemIsInvalid = (splittButtonItem) => {
    const { parentAttribute, item, onUpdate } = this.props; 
    helper_invalidActions.map(invalidAction => {
      if (splittButtonItem.actionId === invalidActionId ) { // eine Action stimmt überrein. Disabele das splitMenuItem, wenn eine restriction verletzt wurde.
        Object.keys(item).map( attribute => {
          if (invalidAttributes.includes(attribute)) {
            // here can happen a set of generic reactions on a specific condition, 
            splittButtonItem =this.adjustSplitButtonItemIfInvalid(splittButtonItem)
          }
        })
      }
    })
    return splittButtonItem;
  }

  adjustInvalidSplitButtonItem = (splittButtonItem) => {
    splittButtonItem.disabled = true,
    splittButtonItem.label = splittButtonItem.invalidLabelText;
    return splittButtonItem;
  }
