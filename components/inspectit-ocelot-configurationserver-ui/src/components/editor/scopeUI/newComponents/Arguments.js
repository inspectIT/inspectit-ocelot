  import {OrderList} from 'primereact/orderlist';
import { InputText } from 'primereact/inputtext';
import {RadioButton} from 'primereact/radiobutton';
import { Button } from 'primereact/button';

class Arguments extends React.Component {

  removeItem = (e,entry) => {
    let { items, onUpdate } = this.props;
    items = items.filter(item => {
      if ( item !== entry ) return entry
    });

    onUpdate(items);
  }

  itemTemplate = (entry) => {
    return (
      <div style={{width: '1000px' }} >
        <InputText ref={this.inputRef} onChange={e => this.handleChange(e,entry)} style={{marginRight: '15px' }} value={entry} />
        <Button onClick={(e) => this.removeItem(e,entry)} label='remove'  className="p-button-danger"></Button>
      </div>
    )
  }

  addArgument = () => {
    const { items, onUpdate } = this.props;
    items.push('');
    onUpdate(items);
  }

  handleChange = (e,entry) => {
    const { items, onUpdate } = this.props;
    const index = items.indexOf(entry);
    items[index] = e.target.value;
    onUpdate(items);
  }

  render() {
    let { items, onUpdate } = this.props;

    console.log('bbb',items)
    items = items || [1,2,3,4];

    return(
      <div>
        <p style={{ marginRight: '15px'}}> ... has the following arguments:</p>
        {/* <div style={{display:'flex', alignItems: 'center'}}>
          <RadioButton style={{marginRight: '10px'}} value="noArguments" name="city" onChange={(e) => this.setState({checkBox: e.value})} checked={this.state.checkBox === 'noArguments'} />
          <p> method(   ) does not have any arguments.</p>

        </div> */}
        <div style={{display:'flex', alignItems: 'center'}}>
          {/* <RadioButton style={{marginRight: '10px'}}  value="hasArguments" name="city" onChange={(e) => this.setState({checkBox: e.value})} checked={this.state.checkBox === 'hasArguments'} /> */}
          <p> 
            method(
              { items.map((item) => 
              <React.Fragment>

                {item !== '' && <span> {item}  </span> }
                {item === '' && <span> &nbsp;&nbsp;&nbsp;&nbsp;</span>}
                <span> , </span>
              </React.Fragment>
              )}
            ) 
          </p>
          <Button style={{marginLeft:'20px' }} onClick={this.addArgument} label="add argument"></Button>
        </div>
        { items && items.length > 0 && 
          <OrderList style={{ display: 'content' , marginBottom: '20px', overflow: 'hidden'}} listStyle={{display: 'content', minWidth:'600px' ,  overflow: 'hidden', height: '250px'}} value={items} itemTemplate={this.itemTemplate} dragdrop={true} onChange={(e) => onUpdate(e.value)}></OrderList>
        }
      </div>
    )
  }
}

export default Arguments

