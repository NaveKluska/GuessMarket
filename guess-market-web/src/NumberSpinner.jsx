import { useEffect, useRef, useState } from 'react';

const HOLD_DELAY_MS = 400;
const HOLD_REPEAT_MS = 60;

function format(value, decimals) {
    return decimals > 0 ? value.toFixed(decimals) : String(value);
}

/**
 * Behaves like the JavaFX client's editable Spinner: arrows (and Up/Down keys) step within
 * [min, max]; a typed value is committed on Enter or when the field loses focus - a number out of
 * range is clamped to the nearest bound, anything that isn't a valid number reverts to the last
 * good value. So, as in JavaFX, typing -3 into a 1..1,000,000 field becomes 1, not an error.
 */
export default function NumberSpinner({ id, value, onChange, min, max, step = 1, decimals = 0, label }) {
    const [text, setText] = useState(format(value, decimals));
    const holdTimer = useRef(null);
    const valueRef = useRef(value);
    valueRef.current = value;

    useEffect(() => {
        setText(format(value, decimals));
    }, [value, decimals]);

    useEffect(() => stopHold, []);

    function clamp(number) {
        const rounded = Number(number.toFixed(decimals));
        return Math.min(max, Math.max(min, rounded));
    }

    function typedValue() {
        const trimmed = text.trim();
        const pattern = decimals > 0 ? /^[+-]?(\d+\.?\d*|\.\d+)$/ : /^[+-]?\d+$/;
        return pattern.test(trimmed) ? clamp(Number(trimmed)) : valueRef.current;
    }

    function commit() {
        const next = typedValue();
        setText(format(next, decimals));
        if (next !== valueRef.current) {
            valueRef.current = next;
            onChange(next);
        }
    }

    function stepBy(direction) {
        const next = clamp(valueRef.current + direction * step);
        if (next !== valueRef.current) {
            valueRef.current = next;
            onChange(next);
        }
        setText(format(next, decimals));
    }

    function startHold(direction) {
        valueRef.current = typedValue();
        stepBy(direction);
        holdTimer.current = setTimeout(function repeat() {
            stepBy(direction);
            holdTimer.current = setTimeout(repeat, HOLD_REPEAT_MS);
        }, HOLD_DELAY_MS);
    }

    function stopHold() {
        clearTimeout(holdTimer.current);
    }

    function handleKeyDown(event) {
        if (event.key === 'Enter') {
            // Stops Enter from also submitting a surrounding form with the not-yet-committed value.
            event.preventDefault();
            commit();
        } else if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            event.preventDefault();
            stepBy(event.key === 'ArrowUp' ? 1 : -1);
        }
    }

    return (
        <span className="spinner">
            <input
                id={id}
                className="spinner-input"
                aria-label={label}
                inputMode={decimals > 0 ? 'decimal' : 'numeric'}
                value={text}
                onChange={event => setText(event.target.value)}
                onBlur={commit}
                onKeyDown={handleKeyDown}
            />
            <span className="spinner-arrows">
                <button type="button" tabIndex={-1} aria-label="Increase" className="spinner-arrow"
                        onMouseDown={event => { event.preventDefault(); startHold(1); }}
                        onMouseUp={stopHold} onMouseLeave={stopHold}>▲</button>
                <button type="button" tabIndex={-1} aria-label="Decrease" className="spinner-arrow"
                        onMouseDown={event => { event.preventDefault(); startHold(-1); }}
                        onMouseUp={stopHold} onMouseLeave={stopHold}>▼</button>
            </span>
        </span>
    );
}
