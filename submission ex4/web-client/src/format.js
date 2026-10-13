const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function money(value) {
    return '$' + Number(value).toFixed(2);
}

export function optionalMoney(value) {
    return value > 0 ? money(value) : '—';
}

export function readableType(type) {
    return type === 'ORDER_BOOK' ? 'Order Book' : 'LMSR';
}

export function readableStatus(status) {
    if (status === 'NOT_ACTIVE') return 'Not active';
    if (status === 'ACTIVE') return 'Active';
    if (status === 'CLOSED') return 'Closed';
    return status;
}

export function readableCommission(type) {
    return type === 'ON_CLOSE' ? 'On close' : 'On purchase';
}

export function typePillClass(type) {
    return type === 'ORDER_BOOK' ? 'pill-ob' : 'pill-lmsr';
}

export function statusPillClass(status) {
    if (status === 'ACTIVE') return 'pill-active';
    if (status === 'CLOSED') return 'pill-closed';
    return 'pill-notactive';
}

// The server sends Java LocalDateTime as "2026-10-13T18:55:29.1524778" (no zone, 7 fraction
// digits). Reading the fields directly avoids the browser guessing at a time zone.
function parts(isoLocal) {
    const [date, time] = isoLocal.split('T');
    const [year, month, day] = date.split('-').map(Number);
    return { year, month, day, time: time.slice(0, 8) };
}

export function timestamp(isoLocal) {
    const p = parts(isoLocal);
    return `${MONTHS[p.month - 1]} ${p.day}, ${p.year} ${p.time}`;
}

export function dateKey(isoLocal) {
    return isoLocal.slice(0, 10);
}

export function dateLabel(isoLocal) {
    const p = parts(isoLocal);
    return `${String(p.day).padStart(2, '0')}.${String(p.month).padStart(2, '0')}.${p.year}`;
}

export function timeOfDay(isoLocal) {
    return parts(isoLocal).time;
}
