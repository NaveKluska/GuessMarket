import { useState } from 'react';
import { createLmsrEvent, createOrderBookEvent } from './api.js';
import { useApp } from './AppContext.js';
import { readableCommission } from './format.js';
import NumberSpinner from './NumberSpinner.jsx';

function FieldRow({ id, caption, children }) {
    return (
        <div className="field-row">
            <label className="meta-label" htmlFor={id}>{caption.toUpperCase()}</label>
            {children}
        </div>
    );
}

/**
 * Same fields, ranges and defaults as the JavaFX Create Event dialog. A rejected event (empty
 * name, duplicate name, duplicate options...) shows the server's reason and keeps the dialog
 * open with everything typed, so nothing has to be re-entered.
 */
export default function CreateEventDialog({ onClose, onCreated }) {
    const { userName, runAction } = useApp();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [optionOne, setOptionOne] = useState('');
    const [optionTwo, setOptionTwo] = useState('');
    const [commissionType, setCommissionType] = useState('ON_PURCHASE');
    const [commission, setCommission] = useState(5);
    const [method, setMethod] = useState('LMSR');
    const [b, setB] = useState(100);
    const [d, setD] = useState(1);
    const [initial, setInitial] = useState(100);
    const [allowMint, setAllowMint] = useState(true);

    function handleSubmit(event) {
        event.preventDefault();
        const options = [optionOne.trim(), optionTwo.trim()];
        const work = method === 'LMSR'
            ? () => createLmsrEvent(userName, name.trim(), description.trim(), commission, commissionType, options, b)
            : () => createOrderBookEvent(userName, name.trim(), description.trim(), commission, commissionType, options, allowMint, initial, d);
        runAction(work, () => {
            onCreated();
            onClose();
        });
    }

    return (
        <div className="modal-backdrop">
            <form className="create-dialog" onSubmit={handleSubmit}
                  onKeyDown={event => event.key === 'Escape' && onClose()}
                  role="dialog" aria-labelledby="create-title">
                <div className="create-dialog-header">
                    <div id="create-title" className="create-dialog-title">Create Event</div>
                    <div className="create-dialog-subtitle">New event, with {userName} as its Market Maker</div>
                </div>

                <div className="create-dialog-fields">
                    <FieldRow id="create-name" caption="Event name">
                        <input id="create-name" className="text-input" value={name} onChange={e => setName(e.target.value)} autoFocus/>
                    </FieldRow>
                    <FieldRow id="create-description" caption="Description">
                        <input id="create-description" className="text-input" value={description} onChange={e => setDescription(e.target.value)}/>
                    </FieldRow>
                    <FieldRow id="create-option-1" caption="Option 1">
                        <input id="create-option-1" className="text-input" value={optionOne} onChange={e => setOptionOne(e.target.value)}/>
                    </FieldRow>
                    <FieldRow id="create-option-2" caption="Option 2">
                        <input id="create-option-2" className="text-input" value={optionTwo} onChange={e => setOptionTwo(e.target.value)}/>
                    </FieldRow>
                    <FieldRow id="create-commission-type" caption="Commission type">
                        <select id="create-commission-type" className="select" value={commissionType} onChange={e => setCommissionType(e.target.value)}>
                            {['ON_PURCHASE', 'ON_CLOSE'].map(type => <option key={type} value={type}>{readableCommission(type)}</option>)}
                        </select>
                    </FieldRow>
                    <FieldRow id="create-commission" caption="Commission %">
                        <NumberSpinner id="create-commission" label="Commission percent" value={commission} onChange={setCommission} min={0} max={90}/>
                    </FieldRow>
                    <FieldRow id="create-method" caption="Trading method">
                        <select id="create-method" className="select" value={method} onChange={e => setMethod(e.target.value)}>
                            <option value="LMSR">LMSR</option>
                            <option value="ORDER_BOOK">Order Book</option>
                        </select>
                    </FieldRow>

                    {method === 'LMSR' ? (
                        <FieldRow id="create-b" caption="Liquidity (b)">
                            <NumberSpinner id="create-b" label="Liquidity b" value={b} onChange={setB} min={1} max={1_000_000}/>
                        </FieldRow>
                    ) : (
                        <>
                            <FieldRow id="create-d" caption="Base value (d)">
                                <NumberSpinner id="create-d" label="Base value d" value={d} onChange={setD} min={1} max={1_000_000}/>
                            </FieldRow>
                            <FieldRow id="create-initial" caption="Initial shares">
                                <NumberSpinner id="create-initial" label="Initial shares" value={initial} onChange={setInitial} min={0} max={1_000_000}/>
                            </FieldRow>
                            <label className="checkbox-row" htmlFor="create-allow-mint">
                                <input id="create-allow-mint" type="checkbox" checked={allowMint} onChange={e => setAllowMint(e.target.checked)}/>
                                Allow mint
                            </label>
                        </>
                    )}
                </div>

                <hr className="separator"/>
                <div className="dialog-buttons">
                    <button type="submit" className="primary-button">OK</button>
                    <button type="button" className="secondary-button" onClick={onClose}>Cancel</button>
                </div>
            </form>
        </div>
    );
}
