# Guess Market EX1 - XML Test Files

Two folders: `valid/` and `invalid/`.

Important: every file here is **well-formed XML that matches the EX1 (V1) schema shape**
(as the assignment promises: "wise-schema" is always fine). The files in `invalid/` are
**application-level ("wise-application") invalid** — things your parser has to catch itself
via business-rule validation, not files that would fail actual XML/XSD parsing (except
#15, #16, #17, #18 which are intentionally broken/off-shape, since a real bad submission
during grading could also be a corrupt/wrong file — good to make sure you don't crash).

## valid/ (should load successfully, no error message)
- 01_basic_two_events.xml — plain two-event file, sanity baseline
- 02_commission_zero.xml — commission = 0 (lower bound, inclusive)
- 03_commission_ninety.xml — commission = 90 (upper bound, inclusive)
- 04_single_event.xml — only one event in the file
- 05_many_events.xml — five events
- 06_negative_id.xml — id is documented as a free integer, negative should be fine
- 07_large_b.xml — very large b value
- 08_whitespace_and_case.xml — name/description/options padded with spaces -> must be trimmed
- 09_duplicate_event_names_allowed.xml — two events share the same `name` (only `id` must
  be unique in EX1 — name uniqueness is only required starting EX3)
- 10_option_case_variants.xml — "YES"/"no" — different strings, not a case-insensitive dup

## invalid/ (should be rejected with a clear, specific error message; app must NOT crash
## and must NOT keep/replace previously-loaded valid data)
- 01_commission_over_90.xml — commission = 95 (> 90)
- 02_commission_negative.xml — commission = -10 (< 0)
- 03_duplicate_event_ids.xml — two events both use id = 1
- 04_only_one_option.xml — only 1 GM-option (spec requires exactly 2)
- 05_three_options.xml — 3 GM-options (spec requires exactly 2)
- 06_duplicate_option_names_case_insensitive.xml — "Yes" and "YES" in the same event
- 07_b_zero.xml — b = 0 (must be a positive integer)
- 08_b_negative.xml — b = -50
- 09_missing_description.xml — mandatory `description` element absent
- 10_missing_id.xml — mandatory `id` element absent
- 11_invalid_commission_type_value.xml — `type="sometimes"` (only on-close/on-purchase allowed)
- 12_non_numeric_commission.xml — commission text is "ten" instead of a number
- 13_non_numeric_b.xml — b text is "lots" instead of a number
- 14_empty_options.xml — `<GM-options>` present but empty
- 15_malformed_unclosed_tag.xml — genuinely not well-formed XML (unclosed `<description>`)
  — tests that your XML parsing itself doesn't throw an uncaught exception
- 16_empty_file.xml — completely empty file
- 17_not_xml_content.txt — plain text, wrong extension (tests your "ends with .xml" check —
  remember the spec says checking the extension is enough, you don't need to sniff content)
- 18_wrong_root_element.xml — root element is `<Some-Other-Root>` instead of `<Guess-Market>`
- 19_missing_method.xml — `<GM-method>` (and therefore the LMSR `b`) missing entirely
- 20_no_events_at_all.xml — `<GM-events>` present but contains zero `<GM-event>` — arguably
  a gray area (not explicitly forbidden by the spec); good one to decide & document your
  own assumption about in the readme, per the assignment's instructions.

## Suggested testing flow
1. Load a valid file -> confirm success message + option 2 (list events) shows it correctly.
2. Load an invalid file -> confirm a specific, informative error message, and that the
   system keeps running (no crash).
3. Load an invalid file *after* a valid one was already loaded -> confirm the previously
   loaded valid data is still intact (invalid files must never overwrite good data).
4. Load two valid files back to back -> confirm the second fully replaces the first.
