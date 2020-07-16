
function MultipleItemBoxContainer ( props) {
  return (
    <div style={{display:'inline-grid'}}>
      {props.children}
    </div>
  )
}

export default MultipleItemBoxContainer;