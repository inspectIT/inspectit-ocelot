import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { SplitButton } from 'primereact/splitbutton';

const saveButtonItems = [
    {
        label: 'Save as..',
        icon: 'pi pi-save',
        command: (e) => {
            console.log("Save as...");
        }
    }
];

/**
 * The toolbar used within the editor view.
 */
const EditorToolbar = ({ path, filename, icon, enableButtons, onSave, onSearch, onHelp }) => (
    <Toolbar>
        <div className="p-toolbar-group-left">
            {filename &&
                <>
                    <i className={"pi " + icon}></i>
                    <div className="path">{path}</div>
                    <div className="name">{filename}</div>
                </>
            }
        </div>
        <div className="p-toolbar-group-right">
            <Button disabled={!enableButtons} icon="pi pi-question" onClick={onHelp} />
            <Button disabled={!enableButtons} icon="pi pi-search" onClick={onSearch} />
            <SplitButton disabled={!enableButtons} onClick={onSave} label="Save" icon="pi pi-save" model={saveButtonItems} />
        </div>
    </Toolbar>
);

export default EditorToolbar;