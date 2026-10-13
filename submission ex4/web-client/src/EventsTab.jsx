import { useState } from 'react';
import EventDetail from './EventDetail.jsx';
import { money, readableCommission, readableStatus, readableType, statusPillClass, typePillClass } from './format.js';
import { Pill, Placeholder } from './ui.jsx';

const ALL = 'All';

function FilterSelect({ id, label, value, onChange, options }) {
    return (
        <label className="filter" htmlFor={id}>
            <span className="filter-label">{label}</span>
            <select id={id} className="select" value={value} onChange={event => onChange(event.target.value)}>
                {options.map(([raw, text]) => <option key={raw} value={raw}>{text}</option>)}
            </select>
        </label>
    );
}

export default function EventsTab({ events, active }) {
    const [method, setMethod] = useState(ALL);
    const [status, setStatus] = useState(ALL);
    const [commission, setCommission] = useState(ALL);
    const [selectedName, setSelectedName] = useState(null);

    const filtered = (events ?? []).filter(event =>
        (method === ALL || event.type === method)
        && (status === ALL || event.status === status)
        && (commission === ALL || event.commissionType === commission));

    const selectedVisible = filtered.some(event => event.name === selectedName);

    return (
        <div className="tab-body">
            <div className="filter-bar">
                <FilterSelect id="filter-method" label="METHOD" value={method} onChange={setMethod} options={[
                    [ALL, 'All Methods'], ['LMSR', readableType('LMSR')], ['ORDER_BOOK', readableType('ORDER_BOOK')]
                ]}/>
                <FilterSelect id="filter-status" label="STATUS" value={status} onChange={setStatus} options={[
                    [ALL, 'All Statuses'], ...['NOT_ACTIVE', 'ACTIVE', 'CLOSED'].map(s => [s, readableStatus(s)])
                ]}/>
                <FilterSelect id="filter-commission" label="COMMISSION" value={commission} onChange={setCommission} options={[
                    [ALL, 'All Commission Types'], ...['ON_PURCHASE', 'ON_CLOSE'].map(c => [c, readableCommission(c)])
                ]}/>
            </div>

            <div className="split">
                <ul className="main-list">
                    {events === null && <li className="list-empty">Loading…</li>}
                    {events !== null && filtered.length === 0 && (
                        <li className="list-empty">{events.length === 0 ? 'No events yet.' : 'No events match these filters.'}</li>
                    )}
                    {filtered.map(event => (
                        <li
                            key={event.name}
                            className={'list-row' + (event.name === selectedName ? ' selected' : '')}
                            onClick={() => setSelectedName(event.name)}
                        >
                            <div className="event-cell-name">{event.name}</div>
                            <div className="pill-row">
                                <Pill className={typePillClass(event.type)}>{readableType(event.type)}</Pill>
                                <Pill className={statusPillClass(event.status)}>{readableStatus(event.status)}</Pill>
                            </div>
                            <div className="pill-row">
                                <Pill className="pill-commission">{readableCommission(event.commissionType)} · {event.commission}%</Pill>
                                <Pill className="pill-account">Account: {money(event.accountBalance)}</Pill>
                                <Pill className="pill-mm">MM: {event.marketMakerName}</Pill>
                            </div>
                        </li>
                    ))}
                </ul>

                <section className="detail-scroll">
                    {selectedVisible
                        ? <EventDetail eventName={selectedName} active={active}/>
                        : <Placeholder>Select an event to see its details.</Placeholder>}
                </section>
            </div>
        </div>
    );
}
