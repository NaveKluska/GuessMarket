import { useState } from 'react';
import { buyShares, closeEvent, getEventDetails, openEvent, submitOrder } from './api.js';
import { NOT_YOURSELF_MESSAGE, useApp } from './AppContext.js';
import { usePoll } from './usePoll.js';
import { money, optionalMoney, readableCommission, readableStatus, timestamp } from './format.js';
import { DataTable, Guard, Placeholder, Pill, SectionLabel, StatTile, WinnerBanner } from './ui.jsx';
import NumberSpinner from './NumberSpinner.jsx';

/** Events tab: read-only overview of one event, polled every second. */
export default function EventDetail({ eventName, active }) {
    const { data: dto, error } = usePoll(() => getEventDetails(eventName), [eventName], active);
    if (!dto) {
        return <Placeholder>{error ?? 'Loading…'}</Placeholder>;
    }
    return <EventView dto={dto} viewer={null}/>;
}

/**
 * viewer is null on the Events tab (read-only, everyone's data). On the Account tab it is
 * { viewedUserName, afterAction }: trade controls appear, history narrows to that user, and the
 * controls are only enabled when the viewed page is your own.
 */
export function EventView({ dto, viewer }) {
    return dto.optionBooks ? <OrderBookView dto={dto} viewer={viewer}/> : <LmsrView dto={dto} viewer={viewer}/>;
}

function DetailHeader({ dto, metas }) {
    return (
        <div className="detail-header">
            <h2 className="detail-title">{dto.name}</h2>
            <div className="description-box">
                <div className="meta-label">DESCRIPTION</div>
                <div className="detail-desc">{dto.description}</div>
            </div>
            <div className="tile-row">
                <StatTile label="Status" value={readableStatus(dto.status)}/>
                <StatTile label="Market Maker" value={dto.marketMakerName}/>
                <StatTile label="Commission" value={`${readableCommission(dto.commissionType)} · ${dto.commission}%`}/>
                {metas.map(meta => <StatTile key={meta.label} {...meta}/>)}
            </div>
        </div>
    );
}

/** Open / Close buttons - only on the Market Maker's own page, and only clickable by the MM. */
function MarketMakerBar({ dto, optionNames, viewer }) {
    const { userName, runAction } = useApp();
    if (!viewer || viewer.viewedUserName !== dto.marketMakerName || dto.status === 'CLOSED') {
        return null;
    }
    const reason = `Only ${dto.marketMakerName}, this event's Market Maker, can do this.`;
    return (
        <Guard allowed={userName === dto.marketMakerName} reason={reason}>
            <div className="action-bar">
                {dto.status === 'NOT_ACTIVE' && (
                    <button className="primary-button" onClick={() => runAction(() => openEvent(userName, dto.name), viewer.afterAction)}>
                        Open Event
                    </button>
                )}
                {dto.status === 'ACTIVE' && optionNames.map((name, index) => (
                    <button key={name} className="close-button" onClick={() => runAction(() => closeEvent(userName, dto.name, index), viewer.afterAction)}>
                        Close: {name} wins
                    </button>
                ))}
            </div>
        </Guard>
    );
}

export function PriceCard({ option, isWinner, selected, onClick }) {
    const className = isWinner ? 'price-card price-card-winner' : selected ? 'price-card price-card-selected' : 'price-card';
    return (
        <div className={className + (onClick ? ' clickable' : '')} onClick={onClick}>
            <div className="price-card-name">
                {option.name}
                {isWinner && <Pill className="pill-winner">WINNER</Pill>}
            </div>
            <div className="price-card-value">{money(option.currentProbability)}</div>
            <div className="price-card-chance">{Math.round(option.currentProbability * 100)}% implied chance</div>
            <div className="price-card-shares">{option.sharesBought} shares bought</div>
        </div>
    );
}

const MAX_QUANTITY = 1_000_000;

/** Pick a card, then buy - nothing is pre-selected, same as the JavaFX client. */
function LmsrTradeCards({ dto, viewer }) {
    const { userName, runAction } = useApp();
    const [selected, setSelected] = useState(-1);
    const [quantity, setQuantity] = useState(1);

    function handleBuy() {
        runAction(() => buyShares(userName, dto.name, selected, quantity), viewer.afterAction);
    }

    return (
        <div className="trade-area">
            <span className="hint-chip">Click an option below to trade it ↓</span>
            <div className="card-row">
                {dto.options.map((option, index) => (
                    <PriceCard key={option.name} option={option} selected={index === selected} onClick={() => setSelected(index)}/>
                ))}
            </div>
            <Guard allowed={userName === viewer.viewedUserName} reason={NOT_YOURSELF_MESSAGE}>
                <Guard allowed={selected >= 0} reason="Select an option above first.">
                    <div className="buy-controls">
                        <label className="meta-label" htmlFor={`qty-${dto.name}`}>QUANTITY</label>
                        <NumberSpinner id={`qty-${dto.name}`} label="Quantity" value={quantity} onChange={setQuantity}
                                       min={1} max={MAX_QUANTITY}/>
                        <button className="primary-button" onClick={handleBuy}>Buy Shares</button>
                    </div>
                </Guard>
            </Guard>
        </div>
    );
}

function LmsrView({ dto, viewer }) {
    const interactive = viewer !== null;
    const commissionMeta = interactive
        ? { label: 'Your commission', value: money(dto.commissionPaidByUser?.[viewer.viewedUserName] ?? 0) }
        : { label: 'Commission collected', value: money(dto.totalCommissionCollected) };
    const history = [...dto.transactions].reverse()
        .filter(tx => !interactive || tx.userName === viewer.viewedUserName);

    const columns = [
        { title: 'Option', render: tx => tx.optionName },
        { title: 'Qty', render: tx => tx.quantity, className: 'num' },
        { title: 'Paid', render: tx => money(tx.pricePaid), className: 'num' },
        { title: 'Commission', render: tx => optionalMoney(tx.commissionPaid), className: 'num' },
        { title: 'Time', render: tx => timestamp(tx.timestamp) }
    ];
    if (!interactive) {
        columns.unshift({ title: 'User', render: tx => tx.userName });
    }

    return (
        <div className="detail-pane">
            <DetailHeader dto={dto} metas={[
                { label: 'Account balance', value: money(dto.accountBalance), valueClass: 'meta-value-money' },
                commissionMeta
            ]}/>
            {dto.winningOptionName && <WinnerBanner optionName={dto.winningOptionName}/>}
            <MarketMakerBar dto={dto} optionNames={dto.options.map(o => o.name)} viewer={viewer}/>
            {interactive && dto.status === 'ACTIVE'
                ? <LmsrTradeCards dto={dto} viewer={viewer}/>
                : (
                    <div className="card-row">
                        {dto.options.map(option => (
                            <PriceCard key={option.name} option={option} isWinner={option.name === dto.winningOptionName}/>
                        ))}
                    </div>
                )}
            <SectionLabel>{interactive ? 'Your trade history' : 'Trade history'}</SectionLabel>
            <DataTable rows={history} rowKey={(tx, index) => tx.timestamp + tx.userName + index}
                       emptyText="No trades yet." columns={columns}/>
        </div>
    );
}

function StatBox({ label, value }) {
    return (
        <div className="stat-box">
            <div className="stat-label">{label.toUpperCase()}</div>
            <div className="stat-value">{value == null ? '—' : money(value)}</div>
        </div>
    );
}

function OrderTable({ orders }) {
    return (
        <DataTable
            rows={orders}
            rowKey={(order, index) => order.userName + order.price + index}
            emptyText="—"
            columns={[
                { title: 'User', render: order => order.userName },
                { title: 'Qty', render: order => order.quantity, className: 'num' },
                { title: 'Price', render: order => money(order.price), className: 'num' }
            ]}
        />
    );
}

function OrderForm({ dto, optionIndex, viewer }) {
    const { userName, runAction } = useApp();
    const minPrice = 0.01;
    const maxPrice = Math.max(minPrice, Number((dto.baseValue - 0.01).toFixed(2)));
    const [side, setSide] = useState('BUY');
    const [quantity, setQuantity] = useState(1);
    const [price, setPrice] = useState(() => Math.round((minPrice + maxPrice) / 2 * 100) / 100);
    const idBase = `order-${dto.name}-${optionIndex}`;

    function handleSubmit() {
        runAction(() => submitOrder(userName, dto.name, optionIndex, side, price, quantity), viewer.afterAction);
    }

    return (
        <Guard allowed={userName === viewer.viewedUserName} reason={NOT_YOURSELF_MESSAGE}>
            <div className="tradebox">
                <select id={`${idBase}-side`} className="select" aria-label="Side" value={side} onChange={event => setSide(event.target.value)}>
                    <option value="BUY">Buy</option>
                    <option value="SELL">Sell</option>
                </select>
                <label className="field">
                    <span className="meta-label">QTY</span>
                    <NumberSpinner id={`${idBase}-qty`} label="Quantity" value={quantity} onChange={setQuantity}
                                   min={1} max={MAX_QUANTITY}/>
                </label>
                <label className="field">
                    <span className="meta-label">PRICE</span>
                    <NumberSpinner id={`${idBase}-price`} label="Price" value={price} onChange={setPrice}
                                   min={minPrice} max={maxPrice} step={0.01} decimals={2}/>
                </label>
                <button className="primary-button" onClick={handleSubmit}>Submit Order</button>
            </div>
        </Guard>
    );
}

function BookPanel({ dto, book, index, viewer }) {
    const isWinner = book.optionName === dto.winningOptionName;
    return (
        <div className={isWinner ? 'book-panel book-panel-winner' : 'book-panel'}>
            <div className="book-panel-title">
                {book.optionName}
                {isWinner && <Pill className="pill-winner">WINNER</Pill>}
            </div>
            <div className="stats-row">
                <StatBox label="Last" value={book.last}/>
                <StatBox label="Bid" value={book.bid}/>
                <StatBox label="Ask" value={book.ask}/>
                <StatBox label="Mid" value={book.mid}/>
                <StatBox label="Spread" value={book.spread}/>
            </div>
            <SectionLabel>Bids</SectionLabel>
            <OrderTable orders={book.bids}/>
            <SectionLabel>Asks</SectionLabel>
            <OrderTable orders={book.asks}/>
            {viewer && dto.status === 'ACTIVE' && <OrderForm dto={dto} optionIndex={index} viewer={viewer}/>}
        </div>
    );
}

function PositionCard({ dto, viewedUserName }) {
    const mine = dto.participants.find(p => p.userName === viewedUserName);
    return (
        <div className="book-panel position-card">
            <SectionLabel>Your position</SectionLabel>
            {!mine
                ? <Placeholder>No holdings by this user yet.</Placeholder>
                : (
                    <>
                        <div className="card-row">
                            {dto.optionBooks.map((book, i) => (
                                <div key={book.optionName} className="price-card position-option">
                                    <div className="price-card-name">{book.optionName}</div>
                                    <div className="position-card-shares">{mine.holdingsByOption[i]} shares held</div>
                                    <div className="position-card-paid">Paid: {money(mine.paidByOption[i])}</div>
                                </div>
                            ))}
                        </div>
                        <div className="tile-row">
                            <StatTile label="Your commission" value={money(mine.commissionPaid)}/>
                            {dto.status === 'CLOSED' && mine.profitOrLoss != null && (
                                <StatTile label={mine.profitOrLoss >= 0 ? 'Profit' : 'Loss'}
                                          value={money(Math.abs(mine.profitOrLoss))}
                                          valueClass={mine.profitOrLoss >= 0 ? 'pl-positive' : 'pl-negative'}/>
                            )}
                        </div>
                    </>
                )}
        </div>
    );
}

function OrderBookView({ dto, viewer }) {
    return (
        <div className="detail-pane">
            <DetailHeader dto={dto} metas={[
                { label: 'Base value (d)', value: money(dto.baseValue) },
                { label: 'Account balance', value: money(dto.accountBalance), valueClass: 'meta-value-money' }
            ]}/>
            {dto.winningOptionName && <WinnerBanner optionName={dto.winningOptionName}/>}
            <MarketMakerBar dto={dto} optionNames={dto.optionBooks.map(b => b.optionName)} viewer={viewer}/>
            <div className="card-row">
                {dto.optionBooks.map((book, index) => (
                    <BookPanel key={book.optionName} dto={dto} book={book} index={index} viewer={viewer}/>
                ))}
            </div>
            {viewer
                ? <PositionCard dto={dto} viewedUserName={viewer.viewedUserName}/>
                : (
                    <>
                        <SectionLabel>Participations</SectionLabel>
                        <DataTable
                            rows={dto.participants}
                            rowKey={p => p.userName}
                            emptyText="No participants yet."
                            columns={[
                                { title: 'Participant', render: p => p.userName },
                                ...dto.optionBooks.map((book, i) => ({
                                    title: book.optionName + ' held', render: p => p.holdingsByOption[i], className: 'num'
                                })),
                                { title: 'Est. value', render: p => money(p.estimatedValue), className: 'num' }
                            ]}
                        />
                    </>
                )}
        </div>
    );
}
