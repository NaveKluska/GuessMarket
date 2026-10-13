/**
 * Shows its children normally when allowed; otherwise disables every control inside (a disabled
 * fieldset disables all its form controls at once) and explains why on hover. The hint sits on
 * the wrapper because browsers don't reliably fire hover events on disabled elements themselves,
 * and is drawn by CSS (see .guard::after) so it appears after 150ms like the JavaFX tooltips,
 * instead of the browser's own ~1s title delay.
 */
export function Guard({ allowed, reason, children }) {
    if (allowed) {
        return children;
    }
    return (
        <span className="guard" data-tip={reason} aria-label={reason}>
            <fieldset disabled>{children}</fieldset>
        </span>
    );
}

export function SectionCard({ title, className = '', children }) {
    return (
        <section className={'section-card ' + className}>
            <header className="section-card-header">{title}</header>
            <div className="section-card-body">{children}</div>
        </section>
    );
}

// Same hue the JavaFX client gives each name: Java's String.hashCode, mod 360, at fixed
// saturation/brightness - so a user is the same colour in both clients.
export function nameColor(name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = (Math.imul(31, hash) + name.charCodeAt(i)) | 0;
    }
    const hue = ((hash % 360) + 360) % 360;
    return `hsl(${hue}, 74%, 39%)`;
}

export function Pill({ className, children }) {
    return <span className={'pill ' + className}>{children}</span>;
}

export function StatTile({ label, value, valueClass = 'meta-value' }) {
    return (
        <div className="stat-tile">
            <div className="meta-label">{label.toUpperCase()}</div>
            <div className={valueClass}>{value}</div>
        </div>
    );
}

export function SectionLabel({ children }) {
    return <div className="section-label">{children}</div>;
}

export function Placeholder({ children }) {
    return <div className="placeholder-label">{children}</div>;
}

export function WinnerBanner({ optionName }) {
    return <div className="winner-banner">🏆 Winner: {optionName}</div>;
}

/** columns: [{ title, render(row), className? }]. Rows need a stable key. */
export function DataTable({ columns, rows, rowKey, emptyText }) {
    return (
        <div className="table-wrap">
            <table className="data-table">
                <thead>
                <tr>{columns.map(col => <th key={col.title} className={col.className}>{col.title}</th>)}</tr>
                </thead>
                <tbody>
                {rows.length === 0
                    ? <tr><td className="table-empty" colSpan={columns.length}>{emptyText}</td></tr>
                    : rows.map((row, index) => (
                        <tr key={rowKey(row, index)}>
                            {columns.map(col => <td key={col.title} className={col.className}>{col.render(row)}</td>)}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
