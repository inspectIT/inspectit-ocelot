class UpperHeader extends React.Component {

  state= { selectorTypeText:undefined, upperText:undefined }

  componentWillMount(){

    const { selectorType, optionType } = this.props;

    switch(selectorType) {
      case 'Class':
        this.setState({selectorTypeText: 'classes'})
        break;
      case 'Method':
        this.setState({selectorTypeText: 'methods'})
    }

    switch(optionType) {
      // class option types
        case 'type':
          this.setState({upperText: `has a specific class name`});
          break;
        case 'interfaces':
          this.setState({upperText: 'implements all of the following interfaces:'}) 
          break;
      // TODO: case annotation ? oder annotations plural
        case 'annotation':
          this.setState({upperText: 'has an annotation'})
          break;
        case 'superclass':
          this.setState({upperText: 'inherits from a superclass'})
          break;
  
      // method option types
        case 'name':
          this.setState({upperText: `has a specific method name`})
          break;
        case 'visibility':
          this.setState({upperText: 'has any of the visibilities'}) 
          break;
        // TODO: case annotation ? oder annotations plural
        case 'annotation':
          this.setState({upperText: 'has an annotation'})
          break;
        case 'arguments':
          this.setState({upperText: 'has the following arguments'})
          break;
      }
  }

  render() {
    const background_uberSchriftDiv = "white";
    const { selectorType, selectorContainerIndex } = this.props;
    const divStyle = { padding: '5px 10px 0 10px' ,height:'30px',  background: background_uberSchriftDiv, width: 'fit-content', outline:'', marginBottom: '10px', marginTop: '30px', borderRadius: '10px' };
    const pStyle = { fontWeight: 'bold', marginTop: '0px' };
    
    return (
      <div style={{...divStyle}}>
        {selectorContainerIndex === 0 && <h4 style={{ ...pStyle}}>The {selectorType} ...</h4>}
        {selectorContainerIndex >0 && <h4 style={{ ...pStyle}}>and the {selectorType} ...</h4> }
      </div>  
    )
  }
}

export default UpperHeader;