import { Fragment, useRef, useState } from 'react';
import { depositCash, getEventDetails } from './api.js';
import { NOT_YOURSELF_MESSAGE, useApp } from './AppContext.js';
import { mapLimit, usePoll } from './usePoll.js';
import { EventView } from './EventDetail.jsx';
import { dateKey, dateLabel, money, readableStatus, readableType, statusPillClass, timeOfDay, typePillClass } from './format.js';
import { Guard, nameColor, Pill, Placeholder, SectionCard, StatTile } from './ui.jsx';
import NumberSpinner from './NumberSpinner.jsx';
import CreateEventDialog from './CreateEventDialog.jsx';

export default function AccountTab({ users, events, refreshShared, active }) {
    const { userName } = useApp();
    // Your own page is open by default - the JavaFX client starts empty, but in a browser the
    // first thing anyone wants after logging in is their own balance.
    const [viewedName, setViewedName] = useState(userName);
    const viewed = users?.find(user => user.name === viewedName) ?? null;

    return (
        <div className="account-split">
            <div className="account-left">
                <SectionCard title="Users" className="users-card">
                    <ul className="main-list user-list">
                        {users === null && <li className="list-empty">Loading…</li>}
                        {users?.map(user => (
                            <UserRow key={user.name} user={user} isSelf={user.name === userName}
                                     selected={user.name === viewedName} onSelect={() => setViewedName(user.name)}/>
                        ))}
                    </ul>
                </SectionCard>
                {viewed
                    ? <AccountBox user={viewed} refreshShared={refreshShared}/>
                    : <Placeholder>Select a user to see their account.</Placeholder>}
            </div>
            <div className="detail-scroll account-right">
                {viewed
                    ? <UserDetail key={viewed.name} user={viewed} events={events ?? []} refreshShared={refreshShared} active={active}/>
                    : <Placeholder>Select a user to see their details.</Placeholder>}
            </div>
        </div>
    );
}

function UserRow({ user, isSelf, selected, onSelect }) {
    return (
        <li className={'list-row' + (selected ? ' selected' : '') + (isSelf ? ' self-row' : '')} onClick={onSelect}>
            <div className="event-cell-name" style={{ color: nameColor(user.name) }}>{user.name}</div>
            <div className="pill-row">
                <Pill className="pill-account">{money(user.balance)}</Pill>
                {isSelf && <Pill className="pill-you">YOU</Pill>}
                {user.blocked && <Pill className="pill-closed">BLOCKED</Pill>}
                {user.marketMaker && <Pill className="pill-mm">MARKET MAKER</Pill>}
            </div>
        </li>
    );
}

function AccountBox({ user, refreshShared }) {
    const { userName, runAction } = useApp();
    const [amount, setAmount] = useState(100);

    function handleDeposit() {
        runAction(() => depositCash(userName, amount), refreshShared);
    }

    return (
        <SectionCard title="Account Details" className="account-box">
            <div className="tile-row">
                <StatTile label="Balance" value={money(user.balance)} valueClass="meta-value-money"/>
                <StatTile label="Blocked" value={user.blocked ? 'Yes' : 'No'} valueClass={user.blocked ? 'pl-negative' : 'pl-positive'}/>
            </div>
            <Guard allowed={user.name === userName} reason={NOT_YOURSELF_MESSAGE}>
                <div className="tradebox">
                    <label className="meta-value" htmlFor="deposit-amount">$</label>
                    <NumberSpinner id="deposit-amount" label="Deposit amount" value={amount} onChange={setAmount}
                                   min={1} max={1_000_000}/>
                    <button className="primary-button" onClick={handleDeposit}>Deposit</button>
                </div>
            </Guard>
            <SectionCard title="Account History">
                <AccountHistory entries={user.accountHistory}/>
            </SectionCard>
        </SectionCard>
    );
}

/** Oldest first, one line per entry, with a date pill whenever the day changes (as in the Chat tab). */
function AccountHistory({ entries }) {
    if (entries.length === 0) {
        return <Placeholder>No account activity yet.</Placeholder>;
    }
    return (
        <div className="history">
            {entries.map((entry, index) => {
                const newDay = index === 0 || dateKey(entries[index - 1].at) !== dateKey(entry.at);
                const sign = entry.amount >= 0 ? '+' : '';
                return (
                    <Fragment key={index}>
                        {newDay && <div className="date-divider"><span>{dateLabel(entry.at)}</span></div>}
                        <div className={entry.amount >= 0 ? 'pl-positive history-row' : 'pl-negative history-row'}>
                            [{timeOfDay(entry.at)}] {entry.description}: {sign}{money(entry.amount)} (balance after: {money(entry.balanceAfter)})
                        </div>
                    </Fragment>
                );
            })}
        </div>
    );
}

// Participation summaries fetch only a few events at a time, so they never take all of the
// browser's (six) connections to the server away from the event being traded below.
const SUMMARY_CONCURRENCY = 4;

function UserDetail({ user, events, refreshShared, active }) {
    const { userName } = useApp();
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [creating, setCreating] = useState(false);
    const inlineRef = useRef(null);

    // Holdings only exist in events this user has something in - relevantEventNames lists exactly
    // those (trades, orders, or events they own) - so every other card needs no fetch at all. That
    // keeps this to a user's handful of events instead of every event on the server. The event
    // open below for trading gets its own poll, so these summaries never delay it.
    const names = events.map(event => event.name).filter(name => user.relevantEventNames.includes(name));
    const details = usePoll(() => mapLimit(names, SUMMARY_CONCURRENCY, name => getEventDetails(name, 'low')), [names], active, true);
    const detailsByName = {};
    (details.data ?? []).forEach(dto => { detailsByName[dto.name] = dto; });
    const inline = usePoll(() => getEventDetails(selectedEvent), [selectedEvent], active && selectedEvent !== null);

    function afterAction() {
        refreshShared();
        details.refresh();
        inline.refresh();
    }

    function selectEvent(name) {
        setSelectedEvent(name);
        requestAnimationFrame(() => inlineRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
    }

    const selectedDto = inline.data?.name === selectedEvent ? inline.data : null;

    return (
        <div className="detail-pane">
            <SectionCard title="Account Details">
                <h2 className="detail-title" style={{ color: nameColor(user.name) }}>{user.name}</h2>
                {/* Creating an event makes you its Market Maker, so it only makes sense on your own page. */}
                <div className="centered-row">
                    <Guard allowed={user.name === userName} reason={NOT_YOURSELF_MESSAGE}>
                        <button className="primary-button" onClick={() => setCreating(true)}>+ Create Event</button>
                    </Guard>
                </div>
                {creating && <CreateEventDialog onClose={() => setCreating(false)} onCreated={afterAction}/>}

                <SectionCard title="Events participation / owner">
                    {events.length === 0
                        ? <Placeholder>No events loaded yet.</Placeholder>
                        : (
                            <div className="participation-grid">
                                {events.map(event => (
                                    <ParticipationCard key={event.name} event={event} userName={user.name}
                                                       dto={detailsByName[event.name]}
                                                       involved={user.relevantEventNames.includes(event.name)}
                                                       selected={event.name === selectedEvent}
                                                       onSelect={() => selectEvent(event.name)}/>
                                ))}
                            </div>
                        )}
                </SectionCard>

                {events.length > 0 && (
                    <div ref={inlineRef}>
                        <SectionCard title="Single event details and trade">
                            {selectedDto
                                ? <EventView key={selectedEvent} dto={selectedDto} viewer={{ viewedUserName: user.name, afterAction }}/>
                                : <Placeholder>{selectedEvent ? 'Loading…' : 'Click an event above to see its full details and trade here.'}</Placeholder>}
                        </SectionCard>
                    </div>
                )}
            </SectionCard>
        </div>
    );
}

function ParticipationCard({ event, userName, dto, involved, selected, onSelect }) {
    return (
        <div className={'book-panel participation-card' + (selected ? ' participation-selected' : '')} onClick={onSelect}>
            <div className="book-panel-title">{event.name}</div>
            <div className="pill-row centered">
                <Pill className={typePillClass(event.type)}>{readableType(event.type)}</Pill>
                <Pill className={statusPillClass(event.status)}>{readableStatus(event.status)}</Pill>
                {event.marketMakerName === userName && <Pill className="pill-mm-tag">MM</Pill>}
            </div>
            <span className="hint-chip">Click to view details & trade below</span>
            {!involved && <Placeholder>{event.type === 'LMSR' ? 'No trades by this user yet.' : 'No holdings by this user yet.'}</Placeholder>}
            {involved && dto && <Holdings dto={dto} userName={userName}/>}
        </div>
    );
}

/** A quick glance at this user's position in one event - not the full ledger. */
function Holdings({ dto, userName }) {
    if (dto.options) {
        const mine = dto.transactions.filter(tx => tx.userName === userName);
        if (mine.length === 0) {
            return <Placeholder>No trades by this user yet.</Placeholder>;
        }
        return (
            <div className="tile-row">
                {dto.options.map(option => {
                    const shares = mine.filter(tx => tx.optionName === option.name).reduce((sum, tx) => sum + tx.quantity, 0);
                    return <StatTile key={option.name} label={option.name} value={`${shares} shares`}/>;
                })}
            </div>
        );
    }
    const mine = dto.participants.find(p => p.userName === userName);
    if (!mine) {
        return <Placeholder>No holdings by this user yet.</Placeholder>;
    }
    return (
        <>
            <div className="tile-row">
                {dto.optionBooks.map((book, i) => (
                    <StatTile key={book.optionName} label={book.optionName} value={`${mine.holdingsByOption[i]} shares`}/>
                ))}
                <StatTile label="Est. value" value={money(mine.estimatedValue)} valueClass="meta-value-money"/>
            </div>
            {dto.status === 'CLOSED' && mine.profitOrLoss != null && (
                <div className={mine.profitOrLoss >= 0 ? 'pl-positive' : 'pl-negative'}>
                    {mine.profitOrLoss >= 0 ? 'Profit: ' : 'Loss: '}{money(Math.abs(mine.profitOrLoss))}
                </div>
            )}
        </>
    );
}
